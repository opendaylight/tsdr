/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.netflow.parser.ipfix;

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
 * UNit tests for NetflowIPFIXPacketParser.
 *
 * @author Thomas Pantelis
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class NetflowIPFIXPacketParserTest extends NetflowPacketParserTestBase {
    @Test
    public void testDataRecords() throws IOException {
        // First packet - 2 templates, 2 data records

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bos);

        // Header
        out.writeShort(10);           // Version Number
        out.writeShort(16 + 42 + 20); // Length
        out.writeInt(289584773);      // Export Time
        out.writeInt(1);              // Sequence Number
        out.writeInt(20);             // Observation Domain ID

        // Template record
        out.writeShort(2);    // Set ID == 2
        out.writeShort(42);   // Length

        // Template 1
        out.writeShort(256);  // Template ID
        out.writeShort(3);    // Field Count
        out.writeShort(4);    // Information Element ID 1 - "protocolIdentifier"
        out.writeShort(1);    // Field Length 1
        out.writeShort(7);    // Information Element ID 2 - "sourceTransportPort"
        out.writeShort(2);    // Field Length 2
        out.writeShort(10);   // Information Element ID 3 - "ingressInterface"
        out.writeShort(4);    // Field Length 3

        // Template 2
        out.writeShort(257);  // Template ID
        out.writeShort(3);    // Field Count
        out.writeShort(12);   // Information Element ID 1 - "destinationIPv4Address"
        out.writeShort(4);    // Field Length 1
        out.writeShort(456 | 0x8000);  // Information Element ID 2
        out.writeShort(8);    // Field Length 2
        out.writeInt(123);    // Enterprise Number
        out.writeShort(82);   // Information Element ID 3 - "interfaceName"
        out.writeShort(5);    // Field Length 3

        out.writeByte(0);     // padding
        out.writeByte(0);     // padding

        // Data record
        out.writeShort(256);  // Set ID == template 1
        out.writeShort(20);   // length

        // Record 1
        out.writeByte(6);
        out.writeShort(23);
        out.writeInt(2857383);

        // Record 2
        out.writeByte(17);
        out.writeShort(2551);
        out.writeInt(5137183);

        out.writeByte(0);     // padding
        out.writeByte(0);     // padding

        out.close();
        List<TSDRLogRecord> records = parseRecords(bos.toByteArray());
        assertEquals(2, records.size());

        Map<String, String> attrs = toMap(records.get(0).getRecordAttributes());
        assertEquals(Long.valueOf(289584773L * 1000), records.get(0).getTimeStamp());
        assertEquals(NetflowIPFIXPacketParser.DATA_RECORD_TEXT, records.get(0).getRecordFullText());
        assertEquals("10", attrs.remove("version"));
        assertEquals("1", attrs.remove("Sequence_Number"));
        assertEquals("20", attrs.remove("Observation_Domain_ID"));

        assertEquals("TCP", attrs.remove("protocolIdentifier"));
        assertEquals("23", attrs.remove("sourceTransportPort"));
        assertEquals("2857383", attrs.remove("ingressInterface"));
        assertEmpty(attrs);

        attrs = toMap(records.get(1).getRecordAttributes());
        assertEquals(Long.valueOf(289584773L * 1000), records.get(1).getTimeStamp());
        assertEquals(NetflowIPFIXPacketParser.DATA_RECORD_TEXT, records.get(1).getRecordFullText());
        assertEquals("10", attrs.remove("version"));
        assertEquals("1", attrs.remove("Sequence_Number"));
        assertEquals("20", attrs.remove("Observation_Domain_ID"));

        assertEquals("UDP", attrs.remove("protocolIdentifier"));
        assertEquals("2551", attrs.remove("sourceTransportPort"));
        assertEquals("5137183", attrs.remove("ingressInterface"));
        assertEmpty(attrs);

        // Second packet - 1 data record

        bos = new ByteArrayOutputStream();
        out = new DataOutputStream(bos);

        // Header
        out.writeShort(10);           // Version Number
        out.writeShort(16 + 21); // Length
        out.writeInt(289584800);      // Export Time
        out.writeInt(2);              // Sequence Number
        out.writeInt(20);             // Observation Domain ID

        // Data record
        out.writeShort(257);  // Set ID == template 2
        out.writeShort(21);   // length

        // Record
        out.writeInt(0xa0000020);
        out.writeLong(1234567890L);
        out.writeBytes("FE1/0");

        out.close();
        records = parseRecords(bos.toByteArray());
        assertEquals(1, records.size());

        attrs = toMap(records.get(0).getRecordAttributes());
        assertEquals(Long.valueOf(289584800L * 1000), records.get(0).getTimeStamp());
        assertEquals(NetflowIPFIXPacketParser.DATA_RECORD_TEXT, records.get(0).getRecordFullText());
        assertEquals("10", attrs.remove("version"));
        assertEquals("2", attrs.remove("Sequence_Number"));
        assertEquals("20", attrs.remove("Observation_Domain_ID"));

        assertEquals("160.0.0.32", attrs.remove("destinationIPv4Address"));
        assertEquals("1234567890", attrs.remove("123.456"));
        assertEquals("FE1/0", attrs.remove("interfaceName"));
        assertEmpty(attrs);
    }

    @Test
    public void testMissingDataRecordTemplate() throws IOException {

        // First packet - data record with missing template

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bos);

        // Header
        out.writeShort(10);           // Version Number
        out.writeShort(16 + 8);       // Length
        out.writeInt(2);              // Export Time
        out.writeInt(1);              // Sequence Number
        out.writeInt(11);             // Observation Domain ID

        // Data record
        out.writeShort(256);    // Set ID == missing template
        out.writeShort(8);      // length

        // Record 1
        out.writeShort(111);

        // Record 2
        out.writeShort(222);

        List<TSDRLogRecord> records1 = parseRecords(bos.toByteArray());
        assertEquals(0, records1.size());

        // Second packet - another data record with missing template plus a second template

        bos = new ByteArrayOutputStream();
        out = new DataOutputStream(bos);

        // Header
        out.writeShort(10);           // Version Number
        out.writeShort(16 + 6 + 12);  // Length
        out.writeInt(3);              // Export Time
        out.writeInt(2);              // Sequence Number
        out.writeInt(11);             // Observation Domain ID

        // Data record
        out.writeShort(256);    // Set ID == missing template
        out.writeShort(6);      // length

        // Record
        out.writeShort(333);

        // Template 2 record
        out.writeShort(2);    // Set ID == 2
        out.writeShort(12);   // Length

        // Template 2
        out.writeShort(257);  // Template ID
        out.writeShort(1);    // Field Count
        out.writeShort(14);   // Information Element ID - "egressInterface"
        out.writeShort(4);    // Field Length

        List<TSDRLogRecord> records2 = parseRecords(bos.toByteArray());
        assertEquals(0, records2.size());
        assertEquals(0, records1.size());

        // Third packet - third template

        bos = new ByteArrayOutputStream();
        out = new DataOutputStream(bos);

        // Header
        out.writeShort(10);           // Version Number
        out.writeShort(16 + 12);      // Length
        out.writeInt(4);              // Export Time
        out.writeInt(3);              // Sequence Number
        out.writeInt(11);             // Observation Domain ID

        // Template record
        out.writeShort(2);    // Set ID == 2
        out.writeShort(12);   // Length

        // Template
        out.writeShort(258);  // Template ID
        out.writeShort(1);    // Field Count
        out.writeShort(29);   // Information Element ID - "sourceIPv6PrefixLength"
        out.writeShort(1);    // Field Length

        assertEquals(0, parseRecords(bos.toByteArray()).size());
        assertEquals(0, records1.size());
        assertEquals(0, records2.size());

        // Fourth packet - data record for second template

        bos = new ByteArrayOutputStream();
        out = new DataOutputStream(bos);

        // Header
        out.writeShort(10);           // Version Number
        out.writeShort(16 + 8);       // Length
        out.writeInt(5);              // Export Time
        out.writeInt(4);              // Sequence Number
        out.writeInt(11);             // Observation Domain ID

        // Data record
        out.writeShort(257);    // Set ID == second template
        out.writeShort(8);      // length

        // Record
        out.writeInt(999);

        List<TSDRLogRecord> records3 = parseRecords(bos.toByteArray());
        assertEquals(1, records3.size());

        Map<String, String> attrs = toMap(records3.get(0).getRecordAttributes());
        assertEquals(Long.valueOf(5 * 1000), records3.get(0).getTimeStamp());
        assertEquals(NetflowIPFIXPacketParser.DATA_RECORD_TEXT, records3.get(0).getRecordFullText());
        assertEquals("10", attrs.remove("version"));
        assertEquals("4", attrs.remove("Sequence_Number"));
        assertEquals("11", attrs.remove("Observation_Domain_ID"));
        assertEquals("999", attrs.remove("egressInterface"));

        // Fifth packet - missing template

        bos = new ByteArrayOutputStream();
        out = new DataOutputStream(bos);

        // Header
        out.writeShort(10);           // Version Number
        out.writeShort(16 + 12);      // Length
        out.writeInt(1);              // Export Time
        out.writeInt(5);              // Sequence Number
        out.writeInt(11);             // Observation Domain ID

        // Template record
        out.writeShort(2);    // Set ID == 2
        out.writeShort(12);   // length

        // Template
        out.writeShort(256);  // Template ID
        out.writeShort(1);    // Field Count
        out.writeShort(36);   // Information Element ID - "flowActiveTimeout"
        out.writeShort(2);    // Field Length

        assertEquals(0, parseRecords(bos.toByteArray()).size());

        assertEquals(2, records1.size());
        attrs = toMap(records1.get(0).getRecordAttributes());
        assertEquals(Long.valueOf(2 * 1000), records1.get(0).getTimeStamp());
        assertEquals(NetflowIPFIXPacketParser.DATA_RECORD_TEXT, records1.get(0).getRecordFullText());
        assertEquals("10", attrs.remove("version"));
        assertEquals("1", attrs.remove("Sequence_Number"));
        assertEquals("11", attrs.remove("Observation_Domain_ID"));
        assertEquals("111", attrs.remove("flowActiveTimeout"));

        attrs = toMap(records1.get(1).getRecordAttributes());
        assertEquals(Long.valueOf(2 * 1000), records1.get(1).getTimeStamp());
        assertEquals(NetflowIPFIXPacketParser.DATA_RECORD_TEXT, records1.get(1).getRecordFullText());
        assertEquals("10", attrs.remove("version"));
        assertEquals("1", attrs.remove("Sequence_Number"));
        assertEquals("11", attrs.remove("Observation_Domain_ID"));
        assertEquals("222", attrs.remove("flowActiveTimeout"));

        assertEquals(1, records2.size());
        attrs = toMap(records2.get(0).getRecordAttributes());
        assertEquals(Long.valueOf(3 * 1000), records2.get(0).getTimeStamp());
        assertEquals(NetflowIPFIXPacketParser.DATA_RECORD_TEXT, records2.get(0).getRecordFullText());
        assertEquals("10", attrs.remove("version"));
        assertEquals("2", attrs.remove("Sequence_Number"));
        assertEquals("11", attrs.remove("Observation_Domain_ID"));
        assertEquals("333", attrs.remove("flowActiveTimeout"));
    }

    @Test
    public void testOptions() throws IOException {
        // First packet - 2 options templates, 1 options data record

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bos);

        // Header
        out.writeShort(10);           // Version Number
        out.writeShort(16 + 24 + 26 + 12); // Length
        out.writeInt(1);              // Export Time
        out.writeInt(1);              // Sequence Number
        out.writeInt(11);             // Observation Domain ID

        // Options template 1
        out.writeShort(3);      // Set ID == 3
        out.writeShort(24);     // length
        out.writeShort(258);    // Template ID
        out.writeShort(3);      // Field Count
        out.writeShort(1);      // Scope Field Count
        out.writeShort(141);    // Scope 1 Information Element ID - "lineCardId"
        out.writeShort(4);      // Scope 1 Field Length
        out.writeShort(41);     // Option 1 Information Element ID - "exportedMessageTotalCount"
        out.writeShort(2);      // Option 1 Field Length
        out.writeShort(42);     // Option 1 Information Element ID - "exportedFlowRecordTotalCount"
        out.writeShort(2);      // Option 1 Field Length

        out.writeByte(0);   // padding
        out.writeByte(0);   // padding

        // Options template 2
        out.writeShort(3);      // Set ID == 3
        out.writeShort(26);     // length
        out.writeShort(259);    // Template ID
        out.writeShort(3);      // Field Count
        out.writeShort(2);      // Scope Field Count
        out.writeShort(141);    // Scope 1 Information Element ID - "lineCardId"
        out.writeShort(4);      // Scope 1 Field Length
        out.writeShort(456 | 0x8000);  // Scope 2 Information Element ID
        out.writeShort(2);      // Scope 2 Field Length
        out.writeInt(123);      // Scope 2 Enterprise Number
        out.writeShort(41);     // Option 1 Information Element ID - "exportedMessageTotalCount"
        out.writeShort(2);      // Option 1 Field Length

        // Options data record
        out.writeShort(259);  // Set ID == template 2
        out.writeShort(12);   // length

        // Record
        out.writeInt(5);
        out.writeShort(7);
        out.writeShort(3567);

        List<TSDRLogRecord> records = parseRecords(bos.toByteArray());
        assertEquals(1, records.size());

        Map<String, String> attrs = toMap(records.get(0).getRecordAttributes());
        assertEquals(Long.valueOf(1 * 1000), records.get(0).getTimeStamp());
        assertEquals("10", attrs.remove("version"));
        assertEquals("1", attrs.remove("Sequence_Number"));
        assertEquals("11", attrs.remove("Observation_Domain_ID"));
        assertEquals("3567", attrs.remove("exportedMessageTotalCount"));
        assertEquals("Options record for lineCardId 5, 123.456 7", records.get(0).getRecordFullText());
        assertEmpty(attrs);

        // Second packet - 2 options data records

        bos = new ByteArrayOutputStream();
        out = new DataOutputStream(bos);

        // Header
        out.writeShort(10);           // Version Number
        out.writeShort(16 + 20);      // Length
        out.writeInt(2);              // Export Time
        out.writeInt(2);              // Sequence Number
        out.writeInt(11);             // Observation Domain ID

        // Options data record
        out.writeShort(258);  // Set ID == template 1
        out.writeShort(20);   // length

        // Record 1
        out.writeInt(1);
        out.writeShort(345);
        out.writeShort(10201);

        // Record 2
        out.writeInt(2);
        out.writeShort(690);
        out.writeShort(20402);

        records = parseRecords(bos.toByteArray());
        assertEquals(2, records.size());

        attrs = toMap(records.get(0).getRecordAttributes());
        assertEquals(Long.valueOf(2 * 1000), records.get(0).getTimeStamp());
        assertEquals("10", attrs.remove("version"));
        assertEquals("2", attrs.remove("Sequence_Number"));
        assertEquals("11", attrs.remove("Observation_Domain_ID"));
        assertEquals("345", attrs.remove("exportedMessageTotalCount"));
        assertEquals("10201", attrs.remove("exportedFlowRecordTotalCount"));
        assertEquals("Options record for lineCardId 1", records.get(0).getRecordFullText());
        assertEmpty(attrs);

        attrs = toMap(records.get(1).getRecordAttributes());
        assertEquals(Long.valueOf(2 * 1000), records.get(1).getTimeStamp());
        assertEquals("10", attrs.remove("version"));
        assertEquals("2", attrs.remove("Sequence_Number"));
        assertEquals("11", attrs.remove("Observation_Domain_ID"));
        assertEquals("690", attrs.remove("exportedMessageTotalCount"));
        assertEquals("20402", attrs.remove("exportedFlowRecordTotalCount"));
        assertEquals("Options record for lineCardId 2", records.get(1).getRecordFullText());
        assertEmpty(attrs);
    }

    @Test
    public void testMissingOptionsTemplate() throws IOException {
        // First packet - options data record with missing template

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bos);

        // Header
        out.writeShort(10);           // Version Number
        out.writeShort(16 + 8);       // Length
        out.writeInt(2);              // Export Time
        out.writeInt(1);              // Sequence Number
        out.writeInt(11);             // Observation Domain ID

        // Options data record
        out.writeShort(260);  // Set ID == missing template
        out.writeShort(8);    // length

        // Record
        out.writeShort(7);
        out.writeShort(765);

        List<TSDRLogRecord> records = parseRecords(bos.toByteArray());
        assertEquals(0, records.size());

        // Second packet - missing template

        bos = new ByteArrayOutputStream();
        out = new DataOutputStream(bos);

        // Header
        out.writeShort(10);           // Version Number
        out.writeShort(16 + 18);       // Length
        out.writeInt(1);              // Export Time
        out.writeInt(2);              // Sequence Number
        out.writeInt(11);             // Observation Domain ID

        // Options template
        out.writeShort(3);      // Set ID == 3
        out.writeShort(18);     // length
        out.writeShort(260);    // Template ID
        out.writeShort(2);      // Field Count
        out.writeShort(1);      // Scope Field Count
        out.writeShort(141);    // Scope 1 Information Element ID - "lineCardId"
        out.writeShort(2);      // Scope 1 Field Length
        out.writeShort(41);     // Option 1 Information Element ID - "exportedMessageTotalCount"
        out.writeShort(2);      // Option 1 Field Length

        assertEquals(0, parseRecords(bos.toByteArray()).size());

        assertEquals(1, records.size());
        Map<String, String> attrs = toMap(records.get(0).getRecordAttributes());
        assertEquals(Long.valueOf(2L * 1000), records.get(0).getTimeStamp());
        assertEquals("10", attrs.remove("version"));
        assertEquals("1", attrs.remove("Sequence_Number"));
        assertEquals("11", attrs.remove("Observation_Domain_ID"));
        assertEquals("765", attrs.remove("exportedMessageTotalCount"));
        assertEquals("Options record for lineCardId 7", records.get(0).getRecordFullText());
    }
}
