/*
 * Copyright (c) 2015 xFlow Research Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.netflow;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.tsdr.spi.scheduler.impl.SchedulerServiceImpl;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRLogRecordInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRLogRecordOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

/**
 * Unit test for Netflow statistics collector.
 *
 * @author Muhammad Umair(muhammad.umair@xflowresearch.com)
 * @author Thomas Pantelis
*/
public class TSDRNetflowCollectorImplTest {
    private static final String SOURCE_IP = "127.0.0.1";

    private final TsdrCollectorSpiService mockSpiService = Mockito.mock(TsdrCollectorSpiService.class);
    private final SchedulerServiceImpl schedulerService = new SchedulerServiceImpl();
    private final TSDRNetflowCollectorImpl implObj = new TSDRNetflowCollectorImpl(mockSpiService, schedulerService);
    private DatagramSocket socket;

    @Before
    public void setup() throws Exception {
        socket = new DatagramSocket();
        implObj.init();
    }

    @After
    public void tearDown() {
        this.socket.close();

        implObj.close();
        schedulerService.close();
    }

    public void sendNetflowData(byte[] data) throws IOException {
        DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(SOURCE_IP), 2055);
        socket.send(packet);
    }

    @Test
    public void sendPacketsTest() throws InterruptedException, IOException {
        List<TSDRLogRecord> storedRecords = Collections.synchronizedList(new ArrayList<>());
        doAnswer(invocation -> {
            storedRecords.addAll(((InsertTSDRLogRecordInput) invocation.getArguments()[0]).getTSDRLogRecord());
            return RpcResultBuilder.success(new InsertTSDRLogRecordOutputBuilder().build()).buildFuture();
        }).when(mockSpiService).insertTSDRLogRecord(any());

        int timestamp = 1234567;
        sendNetflowData(generateV5Data(timestamp));
        sendNetflowData(generateV5Data(timestamp));

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
            return storedRecords.size() == 2;
        });

        assertEquals(26, storedRecords.get(0).getRecordAttributes().size());
        assertEquals("version", storedRecords.get(0).getRecordAttributes().get(0).getName());
        assertEquals("5", storedRecords.get(0).getRecordAttributes().get(0).getValue());
        assertEquals(SOURCE_IP, storedRecords.get(0).getNodeID());
        assertEquals(Long.valueOf(timestamp * 1000L), storedRecords.get(0).getTimeStamp());
        assertEquals(DataCategory.NETFLOW, storedRecords.get(0).getTSDRDataCategory());

        assertEquals(26, storedRecords.get(1).getRecordAttributes().size());
        assertEquals("version", storedRecords.get(0).getRecordAttributes().get(0).getName());
        assertEquals("5", storedRecords.get(0).getRecordAttributes().get(0).getValue());
        assertEquals(SOURCE_IP, storedRecords.get(1).getNodeID());
        assertEquals(Long.valueOf(timestamp * 1000L), storedRecords.get(1).getTimeStamp());
        assertEquals(DataCategory.NETFLOW, storedRecords.get(1).getTSDRDataCategory());
    }

    private byte[] generateV5Data(int timestamp) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bos);

        out.writeShort(5);        // version
        out.writeShort(1);        // flow count
        out.writeInt(0);          // sys_uptime
        out.writeInt(timestamp);  // unix_secs
        out.write(new byte[60]);  // fill the rest with 0's
        return bos.toByteArray();
    }
}
