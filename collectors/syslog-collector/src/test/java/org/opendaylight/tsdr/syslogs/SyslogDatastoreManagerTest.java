/*
 * Copyright (c) 2018 Inocybe Technologies, TethrNet Technology Co.Ltd and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.syslogs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.SettableFuture;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.tsdr.syslogs.server.datastore.SyslogDatastoreManager;
import org.opendaylight.tsdr.syslogs.server.decoder.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.SyslogCollectorConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.SyslogCollectorConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.SyslogDispatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.SyslogFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.SyslogFilterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.SyslogFilterKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.syslog.filter.FilterBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;

/**
 * Unit tests for SyslogDatastoreManager.
 *
 * @author Thomas Pantelis
 * @author Wei Lai(weilai@tetthrnet.com)
 */
public class SyslogDatastoreManagerTest {

    private final SyslogCollectorConfig config = new SyslogCollectorConfigBuilder()
            .setMaxDispatcherExecutorPoolSize(1).setMaxDispatcherExecutorQueueSize(10)
            .setMaxDispatcherNotificationQueueSize(20).build();
    private SyslogDatastoreManager manager;
    private DataBroker dataBroker;

    @Before
    public void setUp() throws Exception {
        AbstractConcurrentDataBrokerTest dataBrokerTest = new AbstractConcurrentDataBrokerTest(false) {
            @Override
            protected Set<YangModuleInfo> getModuleInfos() throws Exception {
                return ImmutableSet.of(BindingReflections.getModuleInfo(SyslogDispatcher.class));
            }
        };
        dataBrokerTest.setup();
        dataBroker = dataBrokerTest.getDataBroker();

        manager = new SyslogDatastoreManager(dataBroker, config);
    }

    @After
    public void tearDown() {
        manager.close();
    }

    @Test
    public void testCallback() throws InterruptedException, ExecutionException, TimeoutException {
        AtomicReference<SettableFuture<String>> outputFuture = new AtomicReference<>(SettableFuture.create());
        URL.setURLStreamHandlerFactory(protocol -> "test".equals(protocol) ? new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL url) throws IOException {
                return new URLConnection(url) {
                    @Override
                    public InputStream getInputStream() {
                        return new ByteArrayInputStream("goodbye".getBytes());
                    }

                    @Override
                    public OutputStream getOutputStream() {
                        return new ByteArrayOutputStream() {
                            @Override
                            public void flush() throws IOException {
                                try {
                                    super.flush();
                                } finally {
                                    outputFuture.get().set(new String(toByteArray()));
                                }
                            }
                        };
                    }

                    @Override
                    public void connect() {
                    }
                };
            }
        } : null);

        manager.init();

        String filterId = "test-filter";
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        InstanceIdentifier<SyslogFilter> filterIID = InstanceIdentifier.create(SyslogDispatcher.class)
                .child(SyslogFilter.class, new SyslogFilterKey(filterId));
        SyslogFilter filter = new SyslogFilterBuilder().setFilterId(filterId).setCallbackUrl("test://localhost")
                .setFilter(new FilterBuilder().setContent("^\\[test\\].*").build()).build();

        tx.merge(LogicalDatastoreType.CONFIGURATION, filterIID, filter);
        tx.commit().get(5, TimeUnit.SECONDS);

        // Send matching message

        String content = "[test] : Hello";
        manager.execute(Message.MessageBuilder.create().applicationName("app").hostname("host")
                .processId("pid").sequenceId("sid").content(content).build());

        final String message = outputFuture.get().get(5, TimeUnit.SECONDS);
        assertEquals(content, message);

        // Send non-matching message

        outputFuture.set(SettableFuture.create());

        manager.execute(Message.MessageBuilder.create().applicationName("app").hostname("host")
                .processId("pid").sequenceId("sid").content("hello").build());

        try {
            outputFuture.get().get(500, TimeUnit.MILLISECONDS);
            fail("Callback received unexpected message");
        } catch (TimeoutException e) {
            // expected
        }

        // Delete the filter

        tx = dataBroker.newWriteOnlyTransaction();
        tx.delete(LogicalDatastoreType.CONFIGURATION, filterIID);
        tx.commit().get(5, TimeUnit.SECONDS);

        outputFuture.set(SettableFuture.create());

        manager.execute(Message.MessageBuilder.create().applicationName("app").hostname("host")
                .processId("pid").sequenceId("sid").content(content).build());

        try {
            outputFuture.get().get(500, TimeUnit.MILLISECONDS);
            fail("Callback received unexpected message");
        } catch (TimeoutException e) {
            // expected
        }
    }
}
