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
import com.google.common.util.concurrent.ListenableFuture;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.ShowThreadpoolConfigurationInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.ShowThreadpoolConfigurationOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.ShowThreadpoolConfigurationOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.SyslogCollectorConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.SyslogDispatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.TsdrSyslogCollectorService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.SyslogFilter;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
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
public class SyslogDatastoreManager implements TsdrSyslogCollectorService, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(SyslogDatastoreManager.class);

    private final ThreadPoolExecutor threadPool;
    private final DataBroker dataBroker;
    private final Map<String, CallbackRegistrationInfo> callbackRegMap = new ConcurrentHashMap<>();
    private ListenerRegistration<?> listenerReg;

    @Inject
    public SyslogDatastoreManager(DataBroker dataBroker, SyslogCollectorConfig collectorConfig) {
        this.dataBroker = Objects.requireNonNull(dataBroker);
        this.threadPool = new ThreadPoolExecutor(collectorConfig.getCoreThreadpoolSize(),
                collectorConfig.getMaxThreadpoolSize(), collectorConfig.getKeepAliveTime(),
                TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(collectorConfig.getQueueSize()));

        LOG.info("SyslogDatastoreManager created: coreThreadPoolSize: {}, maxThreadpoolSize: {}, keepAliveTime: {}, "
            + "queueSize: {}", collectorConfig.getCoreThreadpoolSize(), collectorConfig.getMaxThreadpoolSize(),
            collectorConfig.getKeepAliveTime(), collectorConfig.getQueueSize());
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

        threadPool.shutdown();

        LOG.info("SyslogDatastoreManager closed");
    }

    public void execute(Message message) {
        callbackRegMap.values().forEach(reg -> threadPool.execute(() -> notifyCallback(reg, message)));
    }

    @SuppressFBWarnings("DM_DEFAULT_ENCODING")
    private void notifyCallback(CallbackRegistrationInfo regInfo, Message message) {
        if (!regInfo.messageFilter.equals(message)) {
            LOG.debug("Syslog message \"{}\" does not match filter for URL {}", message.getContent(),
                    regInfo.callbackURL);
            return;
        }

        try {
            LOG.debug("Sending syslog message \"{}\" to URL {}", message.getContent(), regInfo.callbackURL);

            URLConnection urlConnection = regInfo.callbackURL.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setRequestProperty("content-type", "application/x-www-form-urlencoded");

            try (OutputStreamWriter out = new OutputStreamWriter(urlConnection.getOutputStream())) {
                out.write(message.getContent());
                out.flush();
            }

            if (LOG.isDebugEnabled()) {
                String line;
                try (BufferedReader responseReader = new BufferedReader(
                        new InputStreamReader(urlConnection.getInputStream()))) {
                    while ((line = responseReader.readLine()) != null) {
                        LOG.debug("Response from URL: {}", line);
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("Error notifying callback URL {}", regInfo.callbackURL, e);
        }
    }

    private void onFilterUpdated(SyslogFilter filter) {
        LOG.debug("SyslogFilter added/changed: {}", filter);

        final String callbackUrl = filter.getCallbackUrl();
        if (!Strings.isNullOrEmpty(callbackUrl)) {
            try {
                callbackRegMap.put(filter.getFilterId(), new CallbackRegistrationInfo(
                        MessageFilter.FilterBuilder.create(filter.getFilter()), new URL(callbackUrl)));
            } catch (MalformedURLException e) {
                LOG.error("Invalid callback URL {}", callbackUrl, e);
            }
        }
    }

    @Override
    public ListenableFuture<RpcResult<ShowThreadpoolConfigurationOutput>> showThreadpoolConfiguration(
            ShowThreadpoolConfigurationInput input) {

        int currentThreadpoolQueueSize = threadPool.getQueue().size();
        int currentThreadpoolQueueRemainingCapacity = threadPool.getQueue().remainingCapacity();
        long currentThreadpoolKeepAliveTime = threadPool.getKeepAliveTime(TimeUnit.SECONDS);

        ShowThreadpoolConfigurationOutput output = new ShowThreadpoolConfigurationOutputBuilder()
                .setCoreThreadNumber(threadPool.getCorePoolSize())
                .setMaxThreadNumber(threadPool.getMaximumPoolSize())
                .setCurrentAliveThreadNumber(threadPool.getPoolSize())
                .setKeepAliveTime((int) currentThreadpoolKeepAliveTime)
                .setQueueRemainingCapacity(currentThreadpoolQueueRemainingCapacity)
                .setQueueUsedCapacity(currentThreadpoolQueueSize)
                .build();

        return RpcResultBuilder.success(output).buildFuture();
    }

    private static class CallbackRegistrationInfo {
        MessageFilter messageFilter;
        URL callbackURL;

        CallbackRegistrationInfo(MessageFilter messageFilter, URL callbackURL) {
            this.messageFilter = messageFilter;
            this.callbackURL = callbackURL;
        }
    }
}
