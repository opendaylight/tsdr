/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.netflow.parser.v9;

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
 * Unit tests for netflow v9 parsing.
 *
 * @author Thomas Pantelis
 */
public class NetflowV9PacketParserTest extends NetflowPacketParserTestBase {
    @Test
    public void testDataFlowsets() throws IOException {
        // First packet - 2 templates, 2 data flowset records

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

        List<TSDRLogRecord> records = parseRecords(bos.toByteArray());
        assertEquals(2, records.size());

        Map<String, String> attrs = toMap(records.get(0).getRecordAttributes());
        assertEquals(Long.valueOf(691368492L * 1000), records.get(0).getTimeStamp());
        assertEquals(NetflowV9PacketParser.FLOW_SET_LOG_TEXT, records.get(0).getRecordFullText());
        assertEquals("9", attrs.remove("version"));
        assertEquals("289584773", attrs.remove("sys_uptime"));
        assertEquals("168", attrs.remove("package_sequence"));
        assertEquals("20", attrs.remove("source_id"));

        assertEquals("17", attrs.remove("PROTOCOL"));
        assertEquals("23", attrs.remove("L4_SRC_PORT"));
        assertEquals("2857383", attrs.remove("OUT_BYTES"));
        assertEmpty(attrs);

        attrs = toMap(records.get(1).getRecordAttributes());
        assertEquals(Long.valueOf(691368492L * 1000), records.get(1).getTimeStamp());
        assertEquals(NetflowV9PacketParser.FLOW_SET_LOG_TEXT, records.get(1).getRecordFullText());
        assertEquals("9", attrs.remove("version"));
        assertEquals("289584773", attrs.remove("sys_uptime"));
        assertEquals("168", attrs.remove("package_sequence"));
        assertEquals("20", attrs.remove("source_id"));

        assertEquals("10", attrs.remove("PROTOCOL"));
        assertEquals("2551", attrs.remove("L4_SRC_PORT"));
        assertEquals("5137183", attrs.remove("OUT_BYTES"));
        assertEmpty(attrs);

        // Second packet - 1 data flowset record

        bos = new ByteArrayOutputStream();
        out = new DataOutputStream(bos);

        // Header
        out.writeShort(9);         // version
        out.writeShort(5);         // count
        out.writeInt(289584780);   // sys_uptime
        out.writeInt(691368500);   // unix_secs
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

        attrs = toMap(records.get(0).getRecordAttributes());
        assertEquals(Long.valueOf(691368500L * 1000), records.get(0).getTimeStamp());
        assertEquals(NetflowV9PacketParser.FLOW_SET_LOG_TEXT, records.get(0).getRecordFullText());
        assertEquals("9", attrs.remove("version"));
        assertEquals("289584780", attrs.remove("sys_uptime"));
        assertEquals("168", attrs.remove("package_sequence"));
        assertEquals("20", attrs.remove("source_id"));

        assertEquals("160.0.0.32", attrs.remove("IPV4_SRC_ADDR"));
        assertEquals("99", attrs.remove("500"));
        assertEquals("FE1/0", attrs.remove("IF_NAME"));
        assertEquals("1:203:405:607:809:a0b:c0d:e0f", attrs.remove("IPV6_NEXT_HOP"));
        assertEquals("0a:0b:0c:0d:12:04", attrs.remove("IN_DST_MAC"));
        assertEmpty(attrs);
    }

    @Test
    public void testOptions() throws IOException {
        // First packet - 2 options templates, 1 options data record

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bos);

        // Header
        out.writeShort(9);         // version
        out.writeShort(3);         // count
        out.writeInt(372829489);   // sys_uptime
        out.writeInt(582747597);   // unix_secs
        out.writeInt(34);          // package_sequence
        out.writeInt(12);          // source_id

        // Options template 1
        out.writeShort(1);      // flowset_id == 1
        out.writeShort(24);     // length
        out.writeShort(258);    // template_id
        out.writeShort(4);      // Scope length
        out.writeShort(8);      // Option length
        out.writeShort(3);      // Scope field 1 type - "Line Card"
        out.writeShort(2);      // Scope field 1 length
        out.writeShort(41);     // Option field 1 type - TOTAL_PKTS_EXP
        out.writeShort(2);      // Option field 1 length
        out.writeShort(42);     // Option field 2 type - TOTAL_FLOWS_EXP
        out.writeShort(2);      // Option field 2 length

        out.writeByte(0);   // padding
        out.writeByte(0);   // padding

        // Options template 2
        out.writeShort(1);      // flowset_id == 1
        out.writeShort(26);     // length
        out.writeShort(259);    // template_id
        out.writeShort(8);      // Scope length
        out.writeShort(8);      // Option length
        out.writeShort(1);      // Scope field 1 type - "System"
        out.writeShort(4);      // Scope field 1 length
        out.writeShort(2);      // Scope field 2 type - "Interface"
        out.writeShort(2);      // Scope field 2 length
        out.writeShort(41);     // Option field 1 type - TOTAL_PKTS_EXP
        out.writeShort(4);      // Option field 1 length
        out.writeShort(42);     // Option field 2 type - TOTAL_FLOWS_EXP
        out.writeShort(2);      // Option field 2 length

        // Options data record set
        out.writeShort(259);  // flowset_id == template_id 2
        out.writeShort(16);   // length

        // Record
        out.writeInt(5);
        out.writeShort(7);
        out.writeInt(123);
        out.writeShort(3567);

        List<TSDRLogRecord> records = parseRecords(bos.toByteArray());
        assertEquals(1, records.size());

        Map<String, String> attrs = toMap(records.get(0).getRecordAttributes());
        assertEquals(Long.valueOf(582747597L * 1000), records.get(0).getTimeStamp());
        assertEquals("9", attrs.remove("version"));
        assertEquals("372829489", attrs.remove("sys_uptime"));
        assertEquals("34", attrs.remove("package_sequence"));
        assertEquals("12", attrs.remove("source_id"));

        assertEquals("123", attrs.remove("TOTAL_PKTS_EXP"));
        assertEquals("3567", attrs.remove("TOTAL_FLOWS_EXP"));
        assertEquals("Options record for System 5, Interface 7", records.get(0).getRecordFullText());
        assertEmpty(attrs);

        // Second packet - 2 options data records

        bos = new ByteArrayOutputStream();
        out = new DataOutputStream(bos);

        // Header
        out.writeShort(9);         // version
        out.writeShort(1);         // count
        out.writeInt(372829490);   // sys_uptime
        out.writeInt(582747598);   // unix_secs
        out.writeInt(35);          // package_sequence
        out.writeInt(12);          // source_id

        // Options data record set
        out.writeShort(258);  // flowset_id == template_id 1
        out.writeShort(16);   // length

        // Record 1
        out.writeShort(1);
        out.writeShort(345);
        out.writeShort(10201);

        // Record 2
        out.writeShort(2);
        out.writeShort(690);
        out.writeShort(20402);

        records = parseRecords(bos.toByteArray());
        assertEquals(2, records.size());

        attrs = toMap(records.get(0).getRecordAttributes());
        assertEquals(Long.valueOf(582747598L * 1000), records.get(0).getTimeStamp());
        assertEquals("9", attrs.remove("version"));
        assertEquals("372829490", attrs.remove("sys_uptime"));
        assertEquals("35", attrs.remove("package_sequence"));
        assertEquals("12", attrs.remove("source_id"));

        assertEquals("345", attrs.remove("TOTAL_PKTS_EXP"));
        assertEquals("10201", attrs.remove("TOTAL_FLOWS_EXP"));
        assertEquals("Options record for Line Card 1", records.get(0).getRecordFullText());
        assertEmpty(attrs);

        attrs = toMap(records.get(1).getRecordAttributes());
        assertEquals("9", attrs.remove("version"));
        assertEquals("372829490", attrs.remove("sys_uptime"));
        assertEquals("35", attrs.remove("package_sequence"));
        assertEquals("12", attrs.remove("source_id"));

        assertEquals("690", attrs.remove("TOTAL_PKTS_EXP"));
        assertEquals("20402", attrs.remove("TOTAL_FLOWS_EXP"));
        assertEquals("Options record for Line Card 2", records.get(1).getRecordFullText());
        assertEmpty(attrs);

    }

    @Test
    public void testUnknownTemplate() throws IOException {
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

        List<TSDRLogRecord> records = parseRecords(bos.toByteArray());
        assertEquals(0, records.size());
    }
}
