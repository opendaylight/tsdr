/*
 * Copyright (c) 2016 TethrNet Technology Co.Ltd and others.  All rights reserved.
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.syslogs.server.datastore;

import com.google.common.base.Strings;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.SyslogDispatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.SyslogListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.SyslogListenerKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This RegisteredListener handles the listening and notification handling of each registered filter.
 *
 * @author Wei Lai(weilai@tethrnet.com)
 */

public class RegisteredListener implements DataTreeChangeListener<SyslogListener> {
    private static final Logger LOG = LoggerFactory.getLogger(RegisteredListener.class);

    private final DataBroker db;
    private final String listenerId;
    private final String callBackUrl;
    private ListenerRegistration<?> listenerReg;

    public RegisteredListener(DataBroker db, String listenerId, String url) {
        this.db = db;
        this.listenerId = listenerId;
        this.callBackUrl = url;
    }

    private InstanceIdentifier<SyslogListener> toInstanceIdentifier() {
        InstanceIdentifier<SyslogListener> iid = InstanceIdentifier.create(SyslogDispatcher.class)
                .child(SyslogListener.class, new SyslogListenerKey(listenerId));
        return iid;
    }

    /**
     * Monitor the listener node in operational tree.
     */
    public void listen() {
        InstanceIdentifier<SyslogListener> iid = this.toInstanceIdentifier();
        this.listenerReg = db.registerDataTreeChangeListener(DataTreeIdentifier.create(
                LogicalDatastoreType.OPERATIONAL, iid), this);
    }

    /**
     * Close the listener registration.
     */
    public boolean close() {
        this.listenerReg.close();
        return true;
    }

    /**
     * Generate notification when Syslog message match certain filter and sent it to the corresponding client.
     */
    @Override
    @SuppressFBWarnings("DM_DEFAULT_ENCODING")
    public void onDataTreeChanged(Collection<DataTreeModification<SyslogListener>> changes) {
        for (DataTreeModification<SyslogListener> change: changes) {
            DataObjectModification<SyslogListener> rootNode = change.getRootNode();
            switch (rootNode.getModificationType()) {
                case WRITE:
                case SUBTREE_MODIFIED:
                    final SyslogListener listener = rootNode.getDataAfter();
                    if (this.callBackUrl != null && !Strings.isNullOrEmpty(listener.getSyslogMessage())) {
                        LOG.info("Got updated message from {}: {} ", listener.getListenerId(),
                                listener.getSyslogMessage());

                        try {
                            URL url = new URL(callBackUrl);
                            URLConnection urlConnection = url.openConnection();
                            urlConnection.setDoOutput(true);
                            urlConnection.setDoInput(true);
                            urlConnection.setRequestProperty("content-type", "application/x-www-form-urlencoded");

                            try (OutputStreamWriter out = new OutputStreamWriter(urlConnection.getOutputStream())) {
                                out.write("received updated message " + listener.getSyslogMessage());
                                out.flush();
                            }

                            String readLine;
                            try (BufferedReader responseReader = new BufferedReader(
                                    new InputStreamReader(urlConnection.getInputStream()))) {
                                while ((readLine = responseReader.readLine()) != null) {
                                    LOG.info("The updated message response is: " + readLine);
                                }
                            }
                        } catch (IOException e) {
                            LOG.error("Error processing message", e);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
