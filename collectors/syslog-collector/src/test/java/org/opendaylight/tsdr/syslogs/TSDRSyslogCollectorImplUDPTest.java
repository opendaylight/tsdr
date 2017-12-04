/*
 * Copyright (c) 2016 TethrNet Technology Co.Ltd and others.  All rights reserved.
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.syslogs;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.tsdr.syslogs.server.datastore.SyslogDatastoreManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRLogRecordInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the udp messages sending and receving
 * test. In this test 4 mocked UDP messages will be
 * inserted into TSDR database.
 *
 * @author Sharon Aicler(saichler@gmail.com)
 * @author Kun Chen(kunch@tethrnet.com)
 */
public class TSDRSyslogCollectorImplUDPTest {
    private static final Logger LOG = LoggerFactory.getLogger(TSDRSyslogCollectorImplUDPTest.class);

    static final int UDP_PORT = 1514;
    static final int TCP_PORT = 6514;

    private DatagramSocket socket;
    private final TsdrCollectorSpiService spiService = Mockito.mock(TsdrCollectorSpiService.class);
    private final SyslogDatastoreManager manager = Mockito.mock(SyslogDatastoreManager.class);
    private final TSDRSyslogCollectorImpl impl = new TSDRSyslogCollectorImpl(spiService, manager, UDP_PORT, TCP_PORT);
    private final List<TSDRLogRecord> storedRecords = new ArrayList<>();

    @Before
    public void setup() throws SocketException {
        LOG.info("Please make sure ports 6514, 514 and 1234 on your machine are available before the test.");
        //Arbitrary port
        socket = new DatagramSocket(1234);
        impl.init();
        Mockito.when(spiService.insertTSDRLogRecord(Mockito.any(InsertTSDRLogRecordInput.class)))
            .thenAnswer(invocationOnMock -> {
                InsertTSDRLogRecordInput input = (InsertTSDRLogRecordInput) invocationOnMock.getArguments()[0];
                storedRecords.addAll(input.getTSDRLogRecord());
                return null;
            });
    }

    @After
    public void tearDown() {
        this.socket.close();
        this.impl.close();
    }

    public void sendSysLog(String message) throws IOException {
        byte[] data = message.getBytes();
        LOG.info("Messages send to: " + impl.getUdpPort());
        socket.send(new DatagramPacket(data, data.length, InetAddress.getByName("127.0.0.1"), impl.getUdpPort()));
    }

    @Test
    public void testSingleSyslog() throws IOException, InterruptedException {
        //Assert.assertTrue(impl.isRunning());
        this.storedRecords.clear();
        sendSysLog("<30>1:quentin:May 24 12:22:25:TestProcess[1787]:%3-6-1:This is a test log of cisco.");
        sendSysLog("<30>2:quentin:May 24 12:22:25:TestProcess[1787]:%3-6-1:This is a test log of cisco.");
        sendSysLog("<30>3:quentin:May 24 12:22:25:TestProcess[1787]:%3-6-1:This is a test log of cisco.");
        sendSysLog("This is a unformat message.");
        //sleep 5 seconds as Syslog collector flush the buffer every 2.5 seconds
        //Thread.sleep(10000);
        //Assert.assertEquals(4,this.storedRecords.size());
    }
}
