/*
 * Copyright (c) 2015 xFlow Research Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.netflow;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;

/**
 * Unit test for Netflow statistics collector
 * @author Muhammad Umair(muhammad.umair@xflowresearch.com)
 * Created: Dec 30, 2015
 * Modified: Feb 08, 2016
**/

public class TSDRNetflowCollectorImplTest {
    private TsdrCollectorSpiService collectorSPIService = Mockito.mock(TsdrCollectorSpiService.class);
    private TSDRNetflowCollectorImpl implObj;
    private DatagramSocket socket;
    @Before
    public void setup() throws Exception{
        implObj = new TSDRNetflowCollectorImpl(collectorSPIService);
        socket = new DatagramSocket();
    }
    public void sendNetflowData(byte[] data) throws IOException{
        DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName("127.0.0.1"), 2055);
        socket.send(packet);
    }
    @Test
    public void sendPacketsTest() throws InterruptedException, IOException{
        byte[] a = "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000".getBytes();
        sendNetflowData(a);
        a = "0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000".getBytes();
        sendNetflowData(a);
        Thread.sleep(500);//0.5 sec delay for packet(s) transmission
        Assert.assertEquals(2, (int)implObj.getPacketCount());
    }
    @After
    public void teardown(){
        this.socket.close();
        implObj = null;
        collectorSPIService = null;
    }
}