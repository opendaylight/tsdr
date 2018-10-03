/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.netflow.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.tsdrlog.RecordAttributes;

/**
 * Unit tests for NetflowPacketParserFactory.
 *
 * @author Thomas Pantelis
 */
public class NetflowPacketParserFactoryTest {
    private final NetflowPacketParserFactory factory = new NetflowPacketParserFactory();

    @Test
    public void testVersion5() throws IOException {
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

        List<List<RecordAttributes>> records = parseRecords(bos.toByteArray());
        assertEquals(2, records.size());

        Map<String, String> attrs = toMap(records.get(0));
        assertEquals("5", attrs.remove("version"));
        assertEquals("29115718", attrs.remove("sys_uptime"));
        assertEquals("1470119622", attrs.remove("unix_secs"));
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

        attrs = toMap(records.get(1));
        assertEquals("5", attrs.remove("version"));
        assertEquals("29115718", attrs.remove("sys_uptime"));
        assertEquals("1470119622", attrs.remove("unix_secs"));
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

    @Test
    public void testVersion9() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bos);

        // Header
        out.writeShort(9);         // version
        out.writeShort(5);         // count
        out.writeInt(289584773);   // sys_uptime
        out.writeInt(691368492);   // unix_secs
        out.writeInt(168);         // package_sequence
        out.writeInt(20);          // source_id

        // Template flow set
        out.writeShort(0);    // flowset_id == 0
        out.writeShort(44);   // length

        // Template 1
        out.writeShort(256);  // template_id
        out.writeShort(3);    // field_count
        out.writeShort(4);    // field 1 type - PROTOCOL
        out.writeShort(1);    // field 1 length
        out.writeShort(7);    // field 2 type - L4_SRC_PORT
        out.writeShort(2);    // field 2 length
        out.writeShort(23);   // field 3 type - OUT_BYTES
        out.writeShort(4);    // field 3 length

        // Template 2
        out.writeShort(257);  // template_id
        out.writeShort(5);    // field_count
        out.writeShort(8);    // field 1 type - IPV4_SRC_ADDR
        out.writeShort(4);    // field 1 length
        out.writeShort(500);  // field 2 type - unknown
        out.writeShort(2);    // field 2 length
        out.writeShort(82);   // field 3 type - IF_NAME
        out.writeShort(5);    // field 3 length
        out.writeShort(62);   // field 4 type - IPV6_NEXT_HOP
        out.writeShort(16);   // field 4 length
        out.writeShort(80);   // field 5 type - IN_DST_MAC
        out.writeShort(6);    // field 5 length
        out.close();

        // Data flow set 1
        out.writeShort(256);  // flowset_id == template 1
        out.writeShort(20);   // length

        // Record 1
        out.writeByte(17);
        out.writeShort(23);
        out.writeInt(2857383);

        // Record 2
        out.writeByte(10);
        out.writeShort(2551);
        out.writeInt(5137183);

        out.writeByte(0);     // padding
        out.writeByte(0);     // padding

        List<List<RecordAttributes>> records = parseRecords(bos.toByteArray());
        assertEquals(2, records.size());

        Map<String, String> attrs = toMap(records.get(0));
        assertEquals("9", attrs.remove("version"));
        assertEquals("289584773", attrs.remove("sys_uptime"));
        assertEquals("691368492", attrs.remove("unix_secs"));
        assertEquals("168", attrs.remove("flow_sequence"));
        assertEquals("20", attrs.remove("source_id"));

        assertEquals("17", attrs.remove("PROTOCOL"));
        assertEquals("23", attrs.remove("L4_SRC_PORT"));
        assertEquals("2857383", attrs.remove("OUT_BYTES"));
        assertEmpty(attrs);

        attrs = toMap(records.get(1));
        assertEquals("9", attrs.remove("version"));
        assertEquals("289584773", attrs.remove("sys_uptime"));
        assertEquals("691368492", attrs.remove("unix_secs"));
        assertEquals("168", attrs.remove("flow_sequence"));
        assertEquals("20", attrs.remove("source_id"));

        assertEquals("10", attrs.remove("PROTOCOL"));
        assertEquals("2551", attrs.remove("L4_SRC_PORT"));
        assertEquals("5137183", attrs.remove("OUT_BYTES"));
        assertEmpty(attrs);

        bos = new ByteArrayOutputStream();
        out = new DataOutputStream(bos);

        // Header
        out.writeShort(9);         // version
        out.writeShort(5);         // count
        out.writeInt(289584773);   // sys_uptime
        out.writeInt(691368492);   // unix_secs
        out.writeInt(168);         // package_sequence
        out.writeInt(20);          // source_id

        // Data flow set 2
        out.writeShort(257);  // flowset_id == template 2
        out.writeShort(38);   // length

        // Record
        out.writeInt(0xa0000020);
        out.writeShort(99);
        out.writeBytes("FE1/0");
        out.write(new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0xa, 0xb, 0xc, 0xd, 0xe, 0xf});
        out.write(new byte[]{0xa, 0xb, 0xc, 0xd, 0x12, 0x4});

        out.writeByte(0);   // padding

        records = parseRecords(bos.toByteArray());
        assertEquals(1, records.size());

        attrs = toMap(records.get(0));
        assertEquals("9", attrs.remove("version"));
        assertEquals("289584773", attrs.remove("sys_uptime"));
        assertEquals("691368492", attrs.remove("unix_secs"));
        assertEquals("168", attrs.remove("flow_sequence"));
        assertEquals("20", attrs.remove("source_id"));

        assertEquals("160.0.0.32", attrs.remove("IPV4_SRC_ADDR"));
        assertEquals("99", attrs.remove("500"));
        assertEquals("FE1/0", attrs.remove("IF_NAME"));
        assertEquals("1:203:405:607:809:a0b:c0d:e0f", attrs.remove("IPV6_NEXT_HOP"));
        assertEquals("0a:0b:0c:0d:12:04", attrs.remove("IN_DST_MAC"));
        assertEmpty(attrs);
    }

    @Test
    public void testVersion9UnknownTemplate() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bos);

        // Header
        out.writeShort(9);         // version
        out.writeShort(1);         // count
        out.writeInt(123);         // sys_uptime
        out.writeInt(456);         // unix_secs
        out.writeInt(168);         // package_sequence
        out.writeInt(20);          // source_id

        // Data flow set
        out.writeShort(256);    // flowset_id == unknown template
        out.writeShort(5);      // length
        out.writeByte(1);

        List<List<RecordAttributes>> records = parseRecords(bos.toByteArray());
        assertEquals(0, records.size());
    }

    @Test
    public void testUnknownVersion() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bos);

        // Header
        out.writeShort(1);        // version
        out.writeShort(0);        // count
        out.writeInt(29115718);   // sys_uptime
        out.writeInt(1470119622); // unix_secs
        out.close();

        NetflowPacketParser parser = factory.newInstance(bos.toByteArray());

        List<List<RecordAttributes>> records = parseRecords(bos.toByteArray());
        assertEquals(0, records.size());
    }

    private List<List<RecordAttributes>> parseRecords(byte[] data) {
        NetflowPacketParser parser = factory.newInstance(data);

        final List<List<RecordAttributes>> records = new ArrayList<>();
        parser.parseRecords(r -> records.add(r));
        return records;
    }

    private static void assertEmpty(Map<String, String> attrs) {
        assertTrue("Unexpected record attributes: " + attrs, attrs.isEmpty());
    }

    public static Map<String, String> toMap(List<RecordAttributes> attrs) {
        Map<String, String> map = new HashMap<>();
        attrs.forEach(rec -> map.put(rec.getName(), rec.getValue()));
        return map;
    }
}
