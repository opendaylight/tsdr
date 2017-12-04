/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.tsdr.syslogs.server.datastore.SyslogDatastoreManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRLogRecordInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecord;

public class TSDRSyslogCollectorImplPort514Test {
    private DatagramSocket socket;
    private final TsdrCollectorSpiService spiService = Mockito.mock(TsdrCollectorSpiService.class);
    private TSDRSyslogCollectorImpl impl;
    private final SyslogDatastoreManager manager = Mockito.mock(SyslogDatastoreManager.class);
    private final List<TSDRLogRecord> storedRecords = new ArrayList<>();

    @Before
    public void before() throws SocketException {
        // Arbitrary port
        socket = new DatagramSocket(23319);
        impl = new TSDRSyslogCollectorImpl(spiService, manager, 514, 6514);
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
        impl.close();
        this.socket.close();
    }

    public void sendSysLog(String message) throws IOException, InterruptedException {
        byte[] data = message.getBytes();
        socket.send(new DatagramPacket(data,data.length, InetAddress.getByName("127.0.0.1"), impl.getUdpPort()));
        Thread.sleep(250);
    }

    @Test
    public void testSingleSyslog() throws IOException, InterruptedException {
        this.storedRecords.clear();
        sendSysLog("Hello");
        sendSysLog("World");
        sendSysLog("This is a Syslog Test");
        sendSysLog("Original Address = 19.19.19.19 This is a syslog with originator");
        //sleep 5 seconds as Syslog collector flush the buffer every 2.5 seconds
        Thread.sleep(5000);
        Assert.assertEquals(4,this.storedRecords.size());
    }
}
