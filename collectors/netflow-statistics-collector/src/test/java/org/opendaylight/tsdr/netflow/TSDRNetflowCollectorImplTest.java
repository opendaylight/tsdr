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
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.tsdrlog.RecordAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRLogRecordInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRLogRecordOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

/**
 * Unit test for Netflow statistics collector.
 *
 * @author Muhammad Umair(muhammad.umair@xflowresearch.com)
*/
public class TSDRNetflowCollectorImplTest {
    private final TsdrCollectorSpiService mockSpiService = Mockito.mock(TsdrCollectorSpiService.class);
    private final TSDRNetflowCollectorImpl implObj = new TSDRNetflowCollectorImpl(mockSpiService);
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
    }

    public void sendNetflowData(byte[] data) throws IOException {
        DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName("127.0.0.1"), 2055);
        socket.send(packet);
    }

    public static byte[] hexStringToByteArray(String str) {
        int len = str.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len - 1; i += 2) {
            data[i / 2] = (byte) ((Character.digit(str.charAt(i), 16) << 4) + Character.digit(str.charAt(i + 1), 16));
        }
        return data;
    }

    @Test
    public void sendPacketsTest() throws InterruptedException, IOException {
        List<TSDRLogRecord> storedRecords = Collections.synchronizedList(new ArrayList<>());
        doAnswer(invocation -> {
            storedRecords.addAll(((InsertTSDRLogRecordInput) invocation.getArguments()[0]).getTSDRLogRecord());
            return RpcResultBuilder.success(new InsertTSDRLogRecordOutputBuilder().build()).buildFuture();
        }).when(mockSpiService).insertTSDRLogRecord(any());

        // netflow v5 data
        byte[] data = hexStringToByteArray(
                "0005000101bc454657a03ec60000000000000046000000000a0000020a0000030000000000030005"
                + "000000010000004001bb5ae601bc4546109200500000110100020003201f0000");
        sendNetflowData(data);

        // netflow v9 packet
        data = hexStringToByteArray(
                "000900124a3d1d5857961622000168b700000000010003644a3cb4984a3cb498000000490000000100090000c0a80110080"
                + "8080811009062003500000000000000181000000000004a3cb9f84a3cb9f80000003b00000001000a0000ac10047173ba"
                + "bc031100cd20003500000000000000161000000000004a3cbcc84a3cba140000130200000007000a0002ac1004716e5dc"
                + "2261100cd2101bb000073bab0ad00161000000000004a3cbf884a3cbf880000002800000001000a0002ac1004714ad005"
                + "060600e46103e1000073bab0ad00161000000000004a3cc1684a3cc1680000002900000001000a0002ac1004716e5dc23"
                + "90600e49101bb000073bab0ad00161000000000004a3cc6844a3cc6840000004500000001000a0000ac10047173babc03"
                + "1100e448003500000000000000161000000000004a3cc8504a3cc8500000004b00000001000a0000ac10047173babc031"
                + "100fed0003500000000000000161000000000004a3ccc684a3cc684000000ea00000003000a0000ac100471ac1007ff11"
                + "000089008900000000000016161000000000004a3ccc344a3cc68c00000c7600000009000a0002ac1004716e5dc20f110"
                + "0fcf001bb000073bab0ad00161000000000004a3ccd204a3ccd200000003c0000000100090000c0a8011f73babc031100"
                + "ba09003500000000000000181000000000004a3ccd244a3ccd240000003c0000000100090000c0a8011f73babc0311008"
                + "6aa003500000000000000181000000000004a3ccd284a3ccd28000000320000000100090000c0a8011f73babc031100b1"
                + "fb003500000000000000181000000000004a3ccd284a3ccd28000000320000000100090000c0a8011f73babc031100db8"
                + "d003500000000000000181000000000004a3d06704a3ccbf40000007900000003000a0002ac1004716e5dc21e0600e48b"
                + "01bb000073bab0ad00161100000000004a3cd3b04a3cd3b00000002900000001000a0002ac1004716e5dc21e0600e4920"
                + "1bb000073bab0ad00161000000000004a3cdc304a3cd9a40000057600000003000a0002ac1004714137df210600e4509c"
                + "59000073bab0ad00161800000000004a3ce1c84a3cde900000013200000003000a0002ac10047189744bf40600e0d701b"
                + "b000073bab0ad00161800000000004a3ce2a44a3ce2a40000005900000001000a0002ac1004719d3890d71100ec390dd8"
                + "000073bab0ad0016100000000000");
        sendNetflowData(data);

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
            return storedRecords.size() == 19;
        });

        List<RecordAttributes> recordAttributes = storedRecords.get(0).getRecordAttributes();
        assertEquals(27, recordAttributes.size());
        assertEquals("version", recordAttributes.get(0).getName());
        assertEquals("5", recordAttributes.get(0).getValue());
        assertEquals("flowDuration", recordAttributes.get(26).getName());
        assertEquals("60000", recordAttributes.get(26).getValue());

        recordAttributes = storedRecords.get(1).getRecordAttributes();
        assertEquals(26, recordAttributes.size());
        assertEquals("version", recordAttributes.get(0).getName());
        assertEquals("9", recordAttributes.get(0).getValue());
        assertEquals("Packets", recordAttributes.get(8).getName());
        assertEquals("73", recordAttributes.get(8).getValue());
        assertEquals("dstAS", recordAttributes.get(24).getName());
        assertEquals("24", recordAttributes.get(24).getValue());

        recordAttributes = storedRecords.get(storedRecords.size() - 1).getRecordAttributes();
        assertEquals(26, recordAttributes.size());
        assertEquals("version", recordAttributes.get(0).getName());
        assertEquals("9", recordAttributes.get(0).getValue());
        assertEquals("Packets", recordAttributes.get(8).getName());
        assertEquals("89", recordAttributes.get(8).getValue());
        assertEquals("dstAS", recordAttributes.get(24).getName());
        assertEquals("22", recordAttributes.get(24).getValue());
    }
}
