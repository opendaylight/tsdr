/*
 * Copyright (c) 2016 TethrNet Technology Co.Ltd and others.  All rights reserved.
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.syslogs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.tsdr.syslogs.server.SyslogUDPServer;
import org.opendaylight.tsdr.syslogs.server.datastore.SyslogDatastoreManager;
import org.opendaylight.tsdr.syslogs.server.decoder.Message;

/**
 * Unit tests for SyslogUDPServer.
 *
 * @author Thomas Pantelis
 * @author Kun Chen(kunch@tethrnet.com)
 */
public class SyslogUDPServerTest {
    private static int PORT = 8989;

    private final Deque<Message> messageList = new LinkedList<>();
    private final SyslogDatastoreManager mockManager = mock(SyslogDatastoreManager.class);
    private final SyslogUDPServer server = new SyslogUDPServer(messageList, mockManager);

    @Before
    public void setUp() throws InterruptedException {
        server.startServer(PORT);
    }

    @After
    public void tearDown() throws InterruptedException {
        server.stopServer();
    }

    @Test
    public void testMessageHandling() throws InterruptedException, IOException {
        assertTrue(server.isRunning());
        assertEquals("UDP", server.getProtocol());

        final String messageText = "This is a test message.";
        try (SyslogGenerator generator = new SyslogGenerator("localhost", PORT)) {
            generator.sendSyslog(messageText, 4);
        }

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
            return messageList.size() == 4;
        });

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(mockManager, times(4)).execute(any(), messageCaptor.capture());
        final Message message = messageCaptor.getAllValues().get(0);
        assertEquals(messageText, message.getContent());

        assertEquals(4,messageList.size());
    }
}
