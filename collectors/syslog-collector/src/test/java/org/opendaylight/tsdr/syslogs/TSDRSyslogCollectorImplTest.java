/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.syslogs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.tsdr.spi.scheduler.impl.SchedulerServiceImpl;
import org.opendaylight.tsdr.syslogs.server.datastore.SyslogDatastoreManager;
import org.opendaylight.tsdr.syslogs.server.decoder.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRLogRecordInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRLogRecordOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.SyslogCollectorConfigBuilder;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

/**
 * Unit tests for TSDRSyslogCollectorImpl.
 *
 * @author Thomas Pantelis
 */
public class TSDRSyslogCollectorImplTest {
    static final int UDP_PORT = 1514;

    private final TsdrCollectorSpiService mockSpiService = mock(TsdrCollectorSpiService.class);
    private final SyslogDatastoreManager mockManager = mock(SyslogDatastoreManager.class);
    private final SchedulerServiceImpl schedulerService = new SchedulerServiceImpl();
    private final TSDRSyslogCollectorImpl impl = new TSDRSyslogCollectorImpl(mockSpiService, mockManager,
        schedulerService, new SyslogCollectorConfigBuilder().setUdpport(UDP_PORT).setTcpport(6514)
            .setStoreFlushInterval(100).build());

    @After
    public void tearDown() {
        schedulerService.close();
        impl.close();

    }

    @Test
    public void testSendSyslogMessages() throws IOException, InterruptedException {
        List<TSDRLogRecord> storedRecords = Collections.synchronizedList(new ArrayList<>());
        doAnswer(invocation -> {
            storedRecords.addAll(((InsertTSDRLogRecordInput) invocation.getArguments()[0]).getTSDRLogRecord());
            return RpcResultBuilder.success(new InsertTSDRLogRecordOutputBuilder().build()).buildFuture();
        }).when(mockSpiService).insertTSDRLogRecord(any());

        impl.init();
        assertTrue(impl.isRunning());

        try (SyslogGenerator generator = new SyslogGenerator("localhost", impl.getUdpPort())) {
            List<String> messages = ImmutableList.of("Hello", "World", "This is a syslog message",
                    "This is another syslog message");

            for (String msg : messages) {
                generator.sendSyslog(msg, 1);
            }

            Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
                return storedRecords.size() == messages.size();
            });

            final List<String> storedMessages = storedRecords.stream().map(
                rec -> rec.getRecordFullText()).collect(Collectors.toList());
            assertEquals(storedMessages, messages);

            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            verify(mockManager, times(4)).execute(messageCaptor.capture());
            final List<String> executedMessages = messageCaptor.getAllValues().stream().map(
                msg -> msg.getContent()).collect(Collectors.toList());
            assertEquals(executedMessages, messages);
        }
    }

    @Test
    public void testBindToAlternateUDPPort() throws IOException, InterruptedException {
        // Make sure the ports is occupied
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(UDP_PORT);
        } catch (SocketException e) {
            // Don't care
        }

        try {
            impl.init();
            assertTrue(impl.isRunning());
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

    @Test
    public void testFailToBindToUDPPort() throws IOException, InterruptedException {
        DatagramSocket socket1 = null;
        DatagramSocket socket2 = null;

        // Make sure the ports are occupied
        try {
            socket1 = new DatagramSocket(UDP_PORT);
        } catch (SocketException e) {
            // Don't care
        }

        try {
            socket2 = new DatagramSocket(UDP_PORT + 1000);
        } catch (SocketException e) {
            // Don't care
        }

        try {
            impl.init();
            assertFalse(impl.isRunning());
        } finally {
            if (socket1 != null) {
                socket1.close();
            }
            if (socket2 != null) {
                socket2.close();
            }
        }
    }
}
