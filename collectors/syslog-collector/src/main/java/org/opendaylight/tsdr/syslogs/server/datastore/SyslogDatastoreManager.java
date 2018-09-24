/*
 * Copyright (c) 2016 TethrNet Technology Co.Ltd and others.  All rights reserved.
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.syslogs.server.datastore;

import static org.opendaylight.mdsal.common.api.LogicalDatastoreType.CONFIGURATION;

import com.google.common.base.Strings;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.mdsal.binding.api.ClusteredDataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.tsdr.syslogs.server.decoder.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.SyslogCollectorConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.SyslogDispatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.SyslogFilter;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.util.concurrent.QueuedNotificationManager;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This SyslogDatastoreManager handles the initialization of the data
 * structure of Syslog collector. it also implements all its RPC and
 * provides multi-threads to filter the incoming Syslog messages.
 *
 * @author Wei Lai(weilai@tethrnet.com)
 * @author Wenbo Hu(wenbhu@tethrnet.com)
 */
@Singleton
public class SyslogDatastoreManager implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(SyslogDatastoreManager.class);

    private final ExecutorService executor;
    private final DataBroker dataBroker;
    private final Map<String, CallbackRegistrationInfo> callbackRegMap = new ConcurrentHashMap<>();
    private final QueuedNotificationManager<CallbackRegistrationInfo, Message> notificationManager;
    private ListenerRegistration<?> listenerReg;

    @Inject
    public SyslogDatastoreManager(DataBroker dataBroker, SyslogCollectorConfig collectorConfig) {
        this.dataBroker = Objects.requireNonNull(dataBroker);

        executor = SpecialExecutors.newBlockingBoundedCachedThreadPool(
                collectorConfig.getMaxDispatcherExecutorPoolSize(), collectorConfig.getMaxDispatcherExecutorQueueSize(),
                "SyslogDatastoreMgr", SyslogDatastoreManager.class);
        notificationManager = QueuedNotificationManager.create(executor,
            (regInfo, messages) -> regInfo.notifyCallback(messages),
            collectorConfig.getMaxDispatcherNotificationQueueSize(), "SyslogDatastoreQueueMgr");

        LOG.info("SyslogDatastoreManager created: maxDispatcherExecutorPoolSize: {}, "
            + "maxDispatcherExecutorQueueSize: {}, maxDispatcherNotificationQueueSize: {}",
            collectorConfig.getMaxDispatcherExecutorPoolSize(), collectorConfig.getMaxDispatcherExecutorQueueSize(),
            collectorConfig.getMaxDispatcherNotificationQueueSize());
    }

    @PostConstruct
    public void init() {
        ClusteredDataTreeChangeListener<SyslogFilter> listener = changes -> {
            for (DataTreeModification<SyslogFilter> modification : changes) {
                DataObjectModification<SyslogFilter> rootNode = modification.getRootNode();
                switch (rootNode.getModificationType()) {
                    case WRITE:
                    case SUBTREE_MODIFIED:
                        onFilterUpdated(rootNode.getDataAfter());
                        break;
                    case DELETE:
                        SyslogFilter deleted = rootNode.getDataBefore();
                        LOG.debug("SyslogFilter deleted: {}", deleted);
                        callbackRegMap.remove(deleted.getFilterId());
                        break;
                    default:
                        break;
                }
            }
        };

        listenerReg = dataBroker.registerDataTreeChangeListener(DataTreeIdentifier.create(CONFIGURATION,
                InstanceIdentifier.create(SyslogDispatcher.class).child(SyslogFilter.class)), listener);
    }

    @Override
    @PreDestroy
    public void close() {
        if (listenerReg != null) {
            listenerReg.close();
        }

        executor.shutdown();

        LOG.info("SyslogDatastoreManager closed");
    }

    public void execute(Message message) {
        callbackRegMap.values().forEach(regInfo -> notificationManager.submitNotification(regInfo, message));
    }

    private void onFilterUpdated(SyslogFilter filter) {
        LOG.debug("SyslogFilter added/changed: {}", filter);

        final String callbackUrl = filter.getCallbackUrl();
        if (!Strings.isNullOrEmpty(callbackUrl)) {
            try {
                callbackRegMap.put(filter.getFilterId(), new CallbackRegistrationInfo(
                        MessageFilter.from(filter.getFilter()), new URL(callbackUrl)));
            } catch (MalformedURLException e) {
                LOG.error("Invalid callback URL {}", callbackUrl, e);
            } catch (PatternSyntaxException e) {
                LOG.error("Invalid filter expression", e);
            }
        }
    }

    private static class CallbackRegistrationInfo {
        MessageFilter messageFilter;
        URL callbackURL;

        CallbackRegistrationInfo(MessageFilter messageFilter, URL callbackURL) {
            this.messageFilter = messageFilter;
            this.callbackURL = callbackURL;
        }

        @SuppressFBWarnings("DM_DEFAULT_ENCODING")
        void notifyCallback(Collection<? extends Message> messages) {
            final List<Message> toSend = messages.stream().filter(messageFilter::matches)
                    .collect(Collectors.toList());

            if (LOG.isDebugEnabled()) {
                messages.stream().collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
                    list.removeAll(toSend);
                    return list;
                })).forEach(msg -> LOG.debug("Syslog message \"{}\" does not match filter for URL {}",
                        msg.getContent(), callbackURL));
            }

            if (toSend.isEmpty()) {
                return;

            }

            try {
                URLConnection urlConnection = callbackURL.openConnection();
                urlConnection.setDoOutput(true);
                urlConnection.setDoInput(true);
                urlConnection.setRequestProperty("content-type", "application/x-www-form-urlencoded");

                try (OutputStreamWriter out = new OutputStreamWriter(urlConnection.getOutputStream());
                        BufferedReader responseReader = new BufferedReader(
                                new InputStreamReader(urlConnection.getInputStream()))) {
                    for (Message message: toSend) {
                        LOG.debug("Sending syslog message \"{}\" to URL {}", message.getContent(), callbackURL);

                        out.write(message.getContent());
                        out.flush();

                        String line;
                        while ((line = responseReader.readLine()) != null) {
                            LOG.debug("Response from URL: {}", line);
                        }
                    }
                }
            } catch (IOException e) {
                LOG.error("Error notifying callback URL {}", callbackURL, e);
            }
        }
    }
}
