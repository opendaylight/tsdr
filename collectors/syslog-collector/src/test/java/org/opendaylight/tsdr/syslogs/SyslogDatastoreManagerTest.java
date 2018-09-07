/*
 * Copyright (c) 2018 Inocybe Technologies, TethrNet Technology Co.Ltd and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.syslogs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.common.api.ReadFailedException;
import org.opendaylight.tsdr.syslogs.server.datastore.SyslogDatastoreManager;
import org.opendaylight.tsdr.syslogs.server.decoder.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.DeleteRegisteredFilterInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.DeleteRegisteredFilterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.RegisterFilterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.RegisterFilterInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.RegisterFilterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.SeverityId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.ShowRegisterFilterInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.ShowRegisterFilterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.ShowThreadpoolConfigurationInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.ShowThreadpoolConfigurationOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.SyslogCollectorConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.SyslogCollectorConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.SyslogDispatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.show.register.filter.output.RegisteredSyslogFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.SyslogListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.SyslogListenerKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.common.RpcResult;

/**
 * Unit tests for SyslogDatastoreManager.
 *
 * @author Thomas Pantelis
 * @author Wei Lai(weilai@tetthrnet.com)
 */
public class SyslogDatastoreManagerTest {

    private final SyslogCollectorConfig config = new SyslogCollectorConfigBuilder().setCoreThreadpoolSize(1)
            .setMaxThreadpoolSize(1).setKeepAliveTime(10).setQueueSize(10).build();
    private SyslogDatastoreManager manager;
    private DataBroker dataBroker;

    @Before
    public void setUp() throws Exception {
        AbstractConcurrentDataBrokerTest dataBrokerTest = new AbstractConcurrentDataBrokerTest(true) {
            @Override
            protected Set<YangModuleInfo> getModuleInfos() throws Exception {
                return ImmutableSet.of(BindingReflections.getModuleInfo(SyslogDispatcher.class));
            }
        };
        dataBrokerTest.setup();
        dataBroker = spy(dataBrokerTest.getDataBroker());

        manager = new SyslogDatastoreManager(dataBroker, config);
    }

    @After
    public void tearDown() {
        manager.close();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testFilterRegistration() throws InterruptedException, ExecutionException, TimeoutException {
        ListenerRegistration mockListenerReg = mock(ListenerRegistration.class);
        doReturn(mockListenerReg).when(dataBroker).registerDataTreeChangeListener(any(), any());

        RegisterFilterInput input = new RegisterFilterInputBuilder().setCallbackUrl("http://localhost:9001/server")
                .setContent(".*foo.*").setApplication("app").setHost("host").setPid("123")
                .setSeverity(new SeverityId(1)).setSid("sid").build();

        // Test registerFilter

        RpcResult<RegisterFilterOutput> regResult = manager.registerFilter(input).get(5, TimeUnit.SECONDS);
        assertTrue(regResult.isSuccessful());
        final String listenerId = regResult.getResult().getListenerId();
        assertNotNull(listenerId);

        verify(dataBroker).registerDataTreeChangeListener(eq(DataTreeIdentifier.create(LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifier.create(SyslogDispatcher.class).child(SyslogListener.class,
                        new SyslogListenerKey(listenerId)))), any());

        // Test showRegisterFilter

        RpcResult<ShowRegisterFilterOutput> showResult =
                manager.showRegisterFilter(new ShowRegisterFilterInputBuilder().build()).get(5, TimeUnit.SECONDS);
        assertTrue(showResult.isSuccessful());

        assertEquals(1, showResult.getResult().getRegisteredSyslogFilter().size());

        RegisteredSyslogFilter filter = showResult.getResult().getRegisteredSyslogFilter().get(0);
        assertEquals(input.getCallbackUrl(), filter.getCallbackUrl());
        assertEquals(input.getApplication(), filter.getRegisteredFilterEntity().getApplication());
        assertEquals(input.getContent(), filter.getRegisteredFilterEntity().getContent());
        assertEquals(input.getHost(), filter.getRegisteredFilterEntity().getHost());
        assertEquals(input.getPid(), filter.getRegisteredFilterEntity().getPid());
        assertEquals(input.getSeverity(), filter.getRegisteredFilterEntity().getSeverity());
        assertEquals(input.getSid(), filter.getRegisteredFilterEntity().getSid());

        // Test deleteRegisteredFilter

        RpcResult<DeleteRegisteredFilterOutput> deleteResult = manager.deleteRegisteredFilter(
            new DeleteRegisteredFilterInputBuilder().setFilterId(filter.getFilterId()).build())
                .get(5, TimeUnit.SECONDS);
        assertTrue(deleteResult.isSuccessful());

        showResult = manager.showRegisterFilter(new ShowRegisterFilterInputBuilder().build()).get(5, TimeUnit.SECONDS);
        assertTrue(showResult.isSuccessful());
        assertNull(showResult.getResult().getRegisteredSyslogFilter());

        verify(mockListenerReg).close();
    }

    @Test
    public void testShowRegisterFilterWithReadException()
            throws InterruptedException, ExecutionException, TimeoutException {
        ReadTransaction mockReadTx = mock(ReadTransaction.class);
        doReturn(Futures.immediateFailedCheckedFuture(new ReadFailedException("mock")))
            .when(mockReadTx).read(any(), any());
        doReturn(mockReadTx).when(dataBroker).newReadOnlyTransaction();

        RpcResult<ShowRegisterFilterOutput> result =
                manager.showRegisterFilter(new ShowRegisterFilterInputBuilder().build()).get(5, TimeUnit.SECONDS);
        assertEquals("Reading Filter failed", result.getResult().getResult());
    }

    @Test
    public void testShowThreadpoolConfiguration() throws InterruptedException, ExecutionException, TimeoutException {
        RpcResult<ShowThreadpoolConfigurationOutput> showResult = manager.showThreadpoolConfiguration(
                new ShowThreadpoolConfigurationInputBuilder().build()).get(5, TimeUnit.SECONDS);

        assertTrue(showResult.isSuccessful());
        assertEquals(config.getCoreThreadpoolSize(), showResult.getResult().getCoreThreadNumber());
        assertEquals(config.getMaxThreadpoolSize(), showResult.getResult().getMaxThreadNumber());
        assertEquals(config.getKeepAliveTime(), showResult.getResult().getKeepAliveTime());
        assertEquals(config.getQueueSize().intValue(), showResult.getResult().getQueueUsedCapacity().intValue()
                + showResult.getResult().getQueueRemainingCapacity().intValue());
    }

    @Test
    public void testExecute() throws InterruptedException, ExecutionException, TimeoutException {
        SettableFuture<String> outputFuture = SettableFuture.create();
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
                                    outputFuture.set(new String(toByteArray()));
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

        RegisterFilterInput input = new RegisterFilterInputBuilder().setCallbackUrl("test://localhost")
                .setContent(".*").setApplication(".*").setHost(".*").setPid(".*").setSid(".*").build();

        RpcResult<RegisterFilterOutput> regResult = manager.registerFilter(input).get(5, TimeUnit.SECONDS);
        assertTrue(regResult.isSuccessful());

        String content = "Hello";
        manager.execute(Message.MessageBuilder.create().applicationName("app").hostname("host")
                .processId("pid").sequenceId("sid").content(content).build());

        final String message = outputFuture.get(5, TimeUnit.SECONDS);
        assertTrue("Received unexpected message: " + message, message.endsWith(content));
    }
}
