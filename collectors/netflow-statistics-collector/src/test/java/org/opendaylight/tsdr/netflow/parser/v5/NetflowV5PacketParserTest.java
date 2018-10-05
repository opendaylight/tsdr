/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.netflow.parser.v5;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.opendaylight.tsdr.netflow.parser.NetflowPacketParserTestBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecord;

/**
 * Unit tests for NetflowV5PacketParser.
 *
 * @author Thomas Pantelis
 */
public class NetflowV5PacketParserTest extends NetflowPacketParserTestBase {
    @Test
    public void testFlowRecords() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bos);

        // Header
        out.writeShort(5);        // version
        out.writeShort(2);        // count
        out.writeInt(29115718);   // sys_uptime
        out.writeInt(1470119622); // unix_secs
        out.writeInt(0);          // unix_nsecs
        out.writeInt(70);         // flow_sequence
        out.writeByte(0);         // engine_type
        out.writeByte(0);         // engine_id
        out.writeShort(123);      // sampling_interval

        // Flow 1
        out.writeInt(0xa0000020); // srcaddr
        out.writeInt(0xa0000030); // dstaddr
        out.writeInt(0);          // nexthop
        out.writeShort(48);       // input
        out.writeShort(80);       // output
        out.writeInt(16);         // dPkts
        out.writeInt(1024);       // dOctets
        out.writeInt(464891488);  // first
        out.writeInt(465851489);  // last
        out.writeShort(2336);     // srcport
        out.writeShort(1280);     // dstport
        out.writeByte(0);         // padding
        out.writeByte(1);         // tcp_flags
        out.writeByte(16);        // prot
        out.writeByte(16);        // tos
        out.writeShort(32);       // src_as
        out.writeShort(50);       // dst_as
        out.writeByte(1);         // src_mask
        out.writeByte(240);       // dst_mask
        out.writeByte(0);         // padding
        out.writeByte(0);         // padding

        // Flow 2
        out.writeInt(0xb0010203); // srcaddr
        out.writeInt(0xc0040506); // dstaddr
        out.writeInt(0x0a000007); // nexthop
        out.writeShort(21);       // input
        out.writeShort(63);       // output
        out.writeInt(1234);       // dPkts
        out.writeInt(4096);       // dOctets
        out.writeInt(464891488);  // first
        out.writeInt(465851489);  // last
        out.writeShort(2336);     // srcport
        out.writeShort(1280);     // dstport
        out.writeByte(2);         // padding
        out.writeByte(5);         // tcp_flags
        out.writeByte(17);        // prot
        out.writeByte(9);         // tos
        out.writeShort(32);       // src_as
        out.writeShort(50);       // dst_as
        out.writeByte(24);        // src_mask
        out.writeByte(128);       // dst_mask
        out.writeByte(0);         // padding
        out.writeByte(0);         // padding
        out.close();

        List<TSDRLogRecord> records = parseRecords(bos.toByteArray());
        assertEquals(2, records.size());

        Map<String, String> attrs = toMap(records.get(0).getRecordAttributes());
        assertEquals(Long.valueOf(1470119622L * 1000), records.get(0).getTimeStamp());
        assertEquals(NetflowV5PacketParser.LOG_RECORD_TEXT, records.get(0).getRecordFullText());
        assertEquals("5", attrs.remove("version"));
        assertEquals("29115718", attrs.remove("sys_uptime"));
        assertEquals("0", attrs.remove("unix_nsecs"));
        assertEquals("70", attrs.remove("flow_sequence"));
        assertEquals("0", attrs.remove("engine_type"));
        assertEquals("0", attrs.remove("engine_id"));
        assertEquals("123", attrs.remove("sampling_interval"));

        assertEquals("160.0.0.32", attrs.remove("srcaddr"));
        assertEquals("160.0.0.48", attrs.remove("dstaddr"));
        assertEquals("0.0.0.0", attrs.remove("nexthop"));
        assertEquals("48", attrs.remove("input"));
        assertEquals("80", attrs.remove("output"));
        assertEquals("16", attrs.remove("dPkts"));
        assertEquals("1024", attrs.remove("dOctets"));
        assertEquals("464891488", attrs.remove("first"));
        assertEquals("465851489", attrs.remove("last"));
        assertEquals("2336", attrs.remove("srcport"));
        assertEquals("1280", attrs.remove("dstport"));
        assertEquals("1", attrs.remove("tcp_flags"));
        assertEquals("16", attrs.remove("prot"));
        assertEquals("16", attrs.remove("tos"));
        assertEquals("32", attrs.remove("src_as"));
        assertEquals("50", attrs.remove("dst_as"));
        assertEquals("1", attrs.remove("src_mask"));
        assertEquals("240", attrs.remove("dst_mask"));
        assertEquals("960001", attrs.remove("flow_duration"));
        assertEmpty(attrs);

        attrs = toMap(records.get(1).getRecordAttributes());
        assertEquals(Long.valueOf(1470119622L * 1000), records.get(1).getTimeStamp());
        assertEquals(NetflowV5PacketParser.LOG_RECORD_TEXT, records.get(1).getRecordFullText());
        assertEquals("5", attrs.remove("version"));
        assertEquals("29115718", attrs.remove("sys_uptime"));
        assertEquals("0", attrs.remove("unix_nsecs"));
        assertEquals("70", attrs.remove("flow_sequence"));
        assertEquals("0", attrs.remove("engine_type"));
        assertEquals("0", attrs.remove("engine_id"));
        assertEquals("123", attrs.remove("sampling_interval"));

        assertEquals("176.1.2.3", attrs.remove("srcaddr"));
        assertEquals("192.4.5.6", attrs.remove("dstaddr"));
        assertEquals("10.0.0.7", attrs.remove("nexthop"));
        assertEquals("21", attrs.remove("input"));
        assertEquals("63", attrs.remove("output"));
        assertEquals("1234", attrs.remove("dPkts"));
        assertEquals("4096", attrs.remove("dOctets"));
        assertEquals("464891488", attrs.remove("first"));
        assertEquals("465851489", attrs.remove("last"));
        assertEquals("2336", attrs.remove("srcport"));
        assertEquals("1280", attrs.remove("dstport"));
        assertEquals("5", attrs.remove("tcp_flags"));
        assertEquals("17", attrs.remove("prot"));
        assertEquals("9", attrs.remove("tos"));
        assertEquals("32", attrs.remove("src_as"));
        assertEquals("50", attrs.remove("dst_as"));
        assertEquals("24", attrs.remove("src_mask"));
        assertEquals("128", attrs.remove("dst_mask"));
        assertEquals("960001", attrs.remove("flow_duration"));
        assertEmpty(attrs);
    }
}
