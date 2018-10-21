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
import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;
import org.junit.Test;
import org.opendaylight.tsdr.netflow.parser.MissingTemplateCache;
import org.opendaylight.tsdr.netflow.parser.ipfix.InformationElementMappings.Converter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecordBuilder;

/**
 * Unit tests for InformationElementMappings.
 *
 * @author Thomas Pantelis
 */
public class InformationElementMappingsTest {
    private static final byte[] HEADER = new byte[14];

    private final InformationElementMappings mappings = new InformationElementMappings();

    @Test
    public void testIntegralTypes() throws IOException {
        // unsigned64
        test(2, "packetDeltaCount", Long.toString(1234567890L), 8, out -> out.writeLong(1234567890L));

        // unsigned32
        test(481, "globalAddressMappingHighThreshold", Integer.toString(564539), 4, out -> out.writeInt(564539));

        // unsigned16
        test(458, "sourceTransportPortsLimit", Short.toString((short)3876), 2, out -> out.writeShort(3876));

        // unsigned8
        test(344, "informationElementSemantics", Byte.toString((byte)123), 1, out -> out.writeByte(123));

        // signed32
        test(434, "mibObjectValueInteger", Integer.toString(-1234), 4, out -> out.writeInt(-1234));
    }

    @Test
    public void testMACAddressType() throws IOException {
        test(56, "sourceMacAddress", "0a:0b:0c:0d:12:04", 6,
            out -> out.write(new byte[]{0xa, 0xb, 0xc, 0xd, 0x12, 0x4}));
    }

    @Test
    public void testIpv4AddressType() throws IOException {
        test(43, "ipv4RouterSc", "160.0.0.32", 4, out -> out.writeInt(0xa0000020));
    }

    @Test
    public void testIpv6AddressType() throws IOException {
        test(62, "ipNextHopIPv6Address", "1:203:405:607:809:a0b:c0d:e0f", 16,
            out -> out.write(new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0xa, 0xb, 0xc, 0xd, 0xe, 0xf}));
    }

    @Test
    public void testBooleanType() throws IOException {
        test(276, "dataRecordsReliability", "true", 1, out -> out.writeByte(1));
        test(276, "dataRecordsReliability", "false", 1, out -> out.writeByte(2));
    }

    @Test
    public void testOctetArrayType() throws IOException {
        // Fixed length
        test(313, "ipHeaderPacketSection", "10.20.30.40.50", 5, out -> out.write(new byte[]{10, 20, 30, 40, 50}));

        // Variable length < 255
        test(313, "ipHeaderPacketSection", "10.20.30", 65535, out -> {
            out.writeByte(3); // length
            out.write(new byte[]{10, 20, 30});
        });

        // Variable length > 255
        test(313, "ipHeaderPacketSection", "10.20.30", 65535, out -> {
            out.writeByte(255);
            out.writeShort(3); // length
            out.write(new byte[]{10, 20, 30});
        });
    }

    @Test
    public void testStringType() throws IOException {
        // Fixed length
        test(371, "userName", "tpantelis", 9, out -> out.write("tpantelis".getBytes(StandardCharsets.UTF_8)));

        // Variable length < 255
        test(82, "interfaceName", "FE1/0", 65535, out -> {
            out.writeByte(5); // length
            out.write("FE1/0".getBytes(StandardCharsets.UTF_8));
        });
    }

    @Test
    public void testNonExistentMapping() throws IOException {
        test(Integer.MAX_VALUE, Integer.toString(Integer.MAX_VALUE), "1.2.3", 3, out -> out.write(new byte[]{1, 2, 3}));
    }

    @Test
    public void testBasicList() throws IOException {
        test(291, "basicList", "egressInterface: allOf {1, 4, 8}", 17, out -> {
            out.writeByte(3);   // semantic - allOf
            out.writeShort(14); // information element ID - "egressInterface"
            out.writeShort(4);  // field length
            out.writeInt(1);    // value 1
            out.writeInt(4);    // value 2
            out.writeInt(8);    // value 3
        });

        test(291, "basicList", "12.123: noneOf {10}", 13, out -> {
            out.writeByte(0);    // semantic - noneOf
            out.writeShort(123 | 0x8000); // information element ID
            out.writeShort(4);   // field length
            out.writeInt(12);    // enterprise number
            out.writeInt(10);    // value
        });
    }

    @Test
    public void testFloat64Type() throws IOException {
        test(320, "absoluteError", "123.45", 8, out -> out.writeDouble(123.45));
    }

    @Test
    public void testDateTimeSecondsType() throws IOException {
        test(322, "observationTimeSeconds", "12345", 4, out -> out.writeInt(12345));
    }

    @Test
    public void testDateTimeMillisecondsType() throws IOException {
        test(323, "observationTimeMilliseconds", "123456789", 8, out -> out.writeLong(123456789));
    }

    @Test
    public void testForwardingStatusElement() throws IOException {
        test(89, "forwardingStatus", "Forwarded/Fragmented", 1, out -> out.writeByte(0b01000001));
        test(89, "forwardingStatus", "Dropped/ACL drop", 1, out -> out.writeByte(0b10000010));
        test(89, "forwardingStatus", "Dropped/Hardware", 1, out -> out.writeByte(0b10001111));
        test(89, "forwardingStatus", "Consumed/Incomplete Adjacency", 1, out -> out.writeByte(0b11000010));
        test(89, "forwardingStatus", "123", 1, out -> out.writeByte(123));
    }

    @Test
    public void testTcpControlBitsElement() throws IOException {
        test(6, "tcpControlBits", "FIN|ACK|CWR", 2, out -> out.writeShort(0x0001 | 0x0010 | 0x0080));
        test(6, "tcpControlBits", "FIN|SYN|RST|PSH|ACK|URG|ECE|CWR", 2, out -> out.writeShort(0x00ff));
        test(6, "tcpControlBits", "SYN", 1, out -> out.writeByte(0x0002));
        test(6, "tcpControlBits", "RST|0x400|0x8000", 2, out -> out.writeShort(0x0400 | 0x8000 | 0x0004));
    }

    @Test
    public void testFragmentFlagsElement() throws IOException {
        test(197, "fragmentFlags", "May Fragment|Last Fragment", 1, out -> out.writeByte(0b00));
        test(197, "fragmentFlags", "May Fragment|More Fragments", 1, out -> out.writeByte(0b10));
        test(197, "fragmentFlags", "Don't Fragment|Last Fragment", 1, out -> out.writeByte(0b01));
        test(197, "fragmentFlags", "Don't Fragment|More Fragments", 1, out -> out.writeByte(0b11));
    }

    @Test
    public void testIngressInterfaceTypeElement() throws IOException {
        test(368, "ingressInterfaceType", "ethernetCsmacd", 4, out -> out.writeInt(6));
        test(368, "ingressInterfaceType", "gigabitEthernet", 4, out -> out.writeInt(117));
        test(368, "ingressInterfaceType", "otnOtsig", 4, out -> out.writeInt(294));
    }

    @Test
    public void testProtocolIdentifierElement() throws IOException {
        test(4, "protocolIdentifier", "ICMP", 1, out -> out.writeByte(1));
        test(4, "protocolIdentifier", "KRYPTOLAN", 1, out -> out.writeByte(65));
        test(4, "protocolIdentifier", "ROHC", 1, out -> out.writeByte(142));
    }

    @Test
    public void testAnonymizationFlagsElement() throws IOException {
        test(285, "anonymizationFlags", "SC=Session|PmA|LOR", 2, out -> out.writeShort(0b1101));
        test(285, "anonymizationFlags", "SC=Exporter-Collector Pair|LOR", 2, out -> out.writeShort(0b1010));
        test(285, "anonymizationFlags", "SC=Stable", 2, out -> out.writeShort(0b0011));
    }

    private void test(int id, String name, String value, int length, StreamConsmer streamConsmer) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bos);
        out.write(HEADER);
        streamConsmer.accept(out);

        NetflowIPFIXPacketParser parser = new NetflowIPFIXPacketParser(bos.toByteArray() , 0, "",
                new FlowTemplateCache(), new OptionsTemplateCache(), new MissingTemplateCache(k -> true), mappings,
                new TSDRLogRecordBuilder(), r -> { });

        Entry<String, Converter> entry = mappings.get(id);
        assertEquals(name, entry.getKey());
        assertEquals(value, entry.getValue().toStringValue(parser, length));
    }

    interface StreamConsmer {
        void accept(DataOutputStream out) throws IOException;
    }
}
