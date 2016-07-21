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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRLogRecordInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecord;


/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class TSDRSyslogCollectorImplPort514Test {
    private DatagramSocket socket = null;
    private TsdrCollectorSpiService spiService = Mockito.mock(TsdrCollectorSpiService.class);
    private TSDRSyslogCollectorImpl impl = new TSDRSyslogCollectorImpl(spiService);
    private final List<TSDRLogRecord> storedRecords = new ArrayList<>();
    private int numberOfTests=0;
    private boolean testPortIsValid = false;

    @Before
    public void setup() throws SocketException {
        numberOfTests++;
        if(socket==null){
            //Arbitrary port
            socket = new DatagramSocket(23319);
            Mockito.when(spiService.insertTSDRLogRecord(Mockito.any(InsertTSDRLogRecordInput.class))).thenAnswer(new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                    InsertTSDRLogRecordInput input = (InsertTSDRLogRecordInput) invocationOnMock.getArguments()[0];
                    storedRecords.addAll(input.getTSDRLogRecord());
                    return null;
                }
            });
        }
    }

    public void sendSysLog(String message) throws IOException {
        byte[] data = message.getBytes();
        DatagramPacket packet = new DatagramPacket(data,data.length, InetAddress.getByName("127.0.0.1"),impl.getSelectedPort());
        socket.send(packet);
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

    @After
    public void tearDown(){
        if(numberOfTests==1){
            impl.close();
            this.socket.close();
        }
    }
}
