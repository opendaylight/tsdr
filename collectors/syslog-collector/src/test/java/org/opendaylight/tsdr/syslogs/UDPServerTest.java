/*
 * Copyright (c) 2016 TethrNet Technology Co.Ltd and others.  All rights reserved.
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.syslogs;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.Deque;
import java.util.LinkedList;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.tsdr.syslogs.server.SyslogUDPServer;
import org.opendaylight.tsdr.syslogs.server.datastore.SyslogDatastoreManager;
import org.opendaylight.tsdr.syslogs.server.decoder.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.SyslogCollectorConfigBuilder;

/**
 * This is the test for UDP Server.
 *
 * @author Kun Chen(kunch@tethrnet.com)
 */
public class UDPServerTest {
    private final Deque<Message> messageList = new LinkedList<>();
    private final SyslogDatastoreManager manager = new SyslogDatastoreManager(
            mock(DataBroker.class), new SyslogCollectorConfigBuilder().setCoreThreadpoolSize(1)
            .setMaxThreadpoolSize(1).setKeepAliveTime(10).setQueueSize(10).build());
    private final SyslogUDPServer server = new SyslogUDPServer(messageList, manager);

    @Before
    public void setUp() throws InterruptedException {
        server.startServer(8989);
    }

    @After
    public void tearDown() throws InterruptedException {
        server.stopServer();
        manager.close();
    }

    @Test
    public void testMessageHandle() throws InterruptedException, IOException {
        Assert.assertTrue(server.isRunning());
        Assert.assertEquals("UDP",server.getProtocol());

        SyslogGenerator generator = new SyslogGenerator("localhost",8989);
        generator.sendSyslog("This is a test message.",4);

        Thread.sleep(10000);
        Assert.assertEquals(4,messageList.size());
    }
}
