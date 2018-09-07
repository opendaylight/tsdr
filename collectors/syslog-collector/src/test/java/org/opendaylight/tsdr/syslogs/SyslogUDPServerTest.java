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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.tsdr.syslogs.server.SyslogUDPServer;
import org.opendaylight.tsdr.syslogs.server.decoder.Message;
import org.opendaylight.tsdr.syslogs.server.decoder.MessageQueue;

/**
 * Unit tests for SyslogUDPServer.
 *
 * @author Thomas Pantelis
 * @author Kun Chen(kunch@tethrnet.com)
 */
public class SyslogUDPServerTest {
    private static int PORT = 8989;

    private final MessageQueue mockMessageQueue = mock(MessageQueue.class);
    private final SyslogUDPServer server = new SyslogUDPServer(mockMessageQueue);

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
            generator.sendSyslog(messageText, 1);
        }

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(mockMessageQueue).enqueue(messageCaptor.capture());
        assertEquals(messageText, messageCaptor.getValue().getContent());
    }
}
