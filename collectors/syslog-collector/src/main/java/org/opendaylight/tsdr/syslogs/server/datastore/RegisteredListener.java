/*
 * Copyright (c) 2016 TethrNet Technology Co.Ltd and others.  All rights reserved.
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.syslogs.server.datastore;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.SyslogDispatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.SyslogListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.SyslogListenerKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * This RegisteredListener handles the listening and notification
 * handling of each registered filter
 *
 * @author Wei Lai(weilai@tethrnet.com)
 */

public class RegisteredListener implements DataChangeListener {
    private DataBroker db;
    private final Logger LOG = LoggerFactory.getLogger(RegisteredListener.class);
    private String listenerId;
    private String callBackUrl;
    private ListenerRegistration<DataChangeListener> listener;
    private OutputStreamWriter out;
    private BufferedReader responseReader;

    public RegisteredListener(DataBroker db, String listenerId, String url) {
        this.db = db;
        this.listenerId = listenerId;
        this.callBackUrl = url;
    }

    private InstanceIdentifier<SyslogListener> toInstanceIdentifier(String listenerId) {
        InstanceIdentifier<SyslogListener> iid = InstanceIdentifier.create(SyslogDispatcher.class)
                .child(SyslogListener.class, new SyslogListenerKey(listenerId));
        return iid;
    }

    /**
     * monitor the listener node in operational tree
     */
    public void listen() {
        InstanceIdentifier<SyslogListener> iid = this.toInstanceIdentifier(this.listenerId);
        this.listener = db.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, iid,
                this, AsyncDataBroker.DataChangeScope.SUBTREE);
    }

    /**
     * close the listener registration
     */
    public boolean close() {
        try {
            this.listener.close();
        } catch (Exception e) {
            LOG.error("unable to close listener");
            return false;
        }
        return true;
    }

    /**
     * generate notification when Syslog message match certain filter and sent it to the corresponding client
     */
    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        InstanceIdentifier<SyslogListener> iid = this.toInstanceIdentifier(this.listenerId);
        SyslogListener listener = (SyslogListener) change.getUpdatedData().get(iid);
        if (listener != null) {
            LOG.info("  get updated message from: " + listener.getListenerId());
            LOG.info("  the updated message is: " + listener.getSyslogMessage());

            if (this.callBackUrl != null) {
                try {
                    URL url = new URL(callBackUrl);
                    URLConnection urlConnection = url.openConnection();
                    urlConnection.setDoOutput(true);
                    urlConnection.setDoInput(true);
                    urlConnection.setRequestProperty("content-type", "application/x-www-form-urlencoded");

                    out = new OutputStreamWriter(urlConnection.getOutputStream());
                    out.write("received updated message " + listener.getSyslogMessage());
                    out.flush();

                    int responseCode = ((HttpURLConnection) urlConnection).getResponseCode();
                    if (HttpURLConnection.HTTP_OK == responseCode) {
                        String readLine;
                        responseReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                        while ((readLine = responseReader.readLine()) != null) {
                            LOG.info("the updated message is: " + readLine);
                        }
                    }
                } catch (IOException e) {
                    LOG.error("connection refused");
                } finally {
                    try {
                        out.close();
                        responseReader.close();
                    } catch (IOException e) {
                        LOG.error("unable to close the stream");
                    }
                }
            }

        }
    }
}
