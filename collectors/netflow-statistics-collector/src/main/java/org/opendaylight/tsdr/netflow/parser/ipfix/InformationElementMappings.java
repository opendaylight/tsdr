/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.netflow.parser.ipfix;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maintains IPFIX information element mappings as per https://www.iana.org/assignments/ipfix/ipfix.xhtml.
 *
 * @author Thomas Pantelis
 */
class InformationElementMappings {
    private static final Logger LOG = LoggerFactory.getLogger(InformationElementMappings.class);

    @FunctionalInterface
    interface Converter {
        String toStringValue(NetflowIPFIXPacketParser parser, int length);
    }

    static final Converter INTEGRAL_DATA_TYPE_CONVERTER = (parser, length) -> Long.toString(parser.parseLong(length));
    static final Converter OCTETS_CONVERTER = (parser, length) -> parser.parseOctetString(parser.extractLength(length));

    private static final Map<String, Converter> DATA_TYPE_CONVERTERS = ImmutableMap.<String, Converter>builder()
        .put("basicList", (parser, length) -> parser.parseBasicList(length))
        .put("boolean", (parser, length) -> {
            int value = parser.parseByte();
            return value == 1 ? "true" : value == 2 ? "false" : "???";
        })
        .put("float32", (parser, length) -> Float.toString(Float.intBitsToFloat((int) parser.parseLong(length))))
        .put("float64", (parser, length) -> Double.toString(Double.longBitsToDouble(parser.parseLong(length))))
        .put("ipv4Address", (parser, length) -> parser.parseIPv4Address())
        .put("ipv6Address", (parser, length) -> parser.parseIPv6Address())
        .put("macAddress", (parser, length) -> parser.parseMACAddress())
        .put("octetArray", OCTETS_CONVERTER)
        .put("signed8", (parser, length) -> Long.toString(parser.parseSignedLong(length)))
        .put("signed16", (parser, length) -> Long.toString(parser.parseSignedLong(length)))
        .put("signed32", (parser, length) -> Long.toString(parser.parseSignedLong(length)))
        .put("signed64", (parser, length) -> Long.toString(parser.parseSignedLong(length)))
        .put("string", (parser, length) -> new String(parser.parseBytes(parser.extractLength(length)),
                StandardCharsets.UTF_8))
        .put("subTemplateList", (parser, length) -> "not supported")
        .put("subTemplateMultiList", (parser, length) -> "not supported")
        .build();

    private static final Map<Integer, Converter> ELEMENT_CONVERTERS = ImmutableMap.<Integer, Converter>builder()
        // protocolIdentifier
        .put(Integer.valueOf(4), (parser, length) ->
            parser.parseMapping(length, SpecialElementMappings.PROTOCOL_NUMBERS))
        // tcpControlBits
        .put(Integer.valueOf(6), (parser, length) ->
            parser.parseBitFlags(length, SpecialElementMappings.TCP_CONTROL_BITS))
        // mplsTopLabelType
        .put(Integer.valueOf(46), (parser, length) ->
            parser.parseMapping(length, SpecialElementMappings.MPLS_LABEL_TYPE))
        // flowDirection
        .put(Integer.valueOf(61), (parser, length) ->
            parser.parseMapping(length, SpecialElementMappings.FLOW_DIRECTION))
        // ipv6ExtensionHeaders
        .put(Integer.valueOf(64), (parser, length) ->
            parser.parseBitFlags(length, SpecialElementMappings.IPV6_EXTENSION_HEADERS))
        // Forwarding Status
        .put(Integer.valueOf(89), (parser, length) ->
            parser.parseMapping(length, SpecialElementMappings.FORWARDING_STATUS))
        // flowEndReason
        .put(Integer.valueOf(136), (parser, length) ->
            parser.parseBitFlags(length, SpecialElementMappings.FLOW_END_REASON))
        // fragmentFlags
        .put(Integer.valueOf(197), (parser, length) ->
            parser.parseBitFlags(length, SpecialElementMappings.FRAGMENT_FLAGS_ON,
                Optional.of(SpecialElementMappings.FRAGMENT_FLAGS_OFF)))
        // ipv4Options
        .put(Integer.valueOf(208), (parser, length) ->
            parser.parseBitFlags(length, SpecialElementMappings.IPV4_OPTIONS))
        // natOriginatingAddressRealm
        .put(Integer.valueOf(229), (parser, length) ->
            parser.parseMapping(length, SpecialElementMappings.NAT_ORIGINATING_ADDRESS_REALM))
        // natEvent
        .put(Integer.valueOf(230), (parser, length) ->
            parser.parseMapping(length, SpecialElementMappings.NAT_EVENT_TYPE))
        // firewallEvent
        .put(Integer.valueOf(233), (parser, length) ->
            parser.parseMapping(length, SpecialElementMappings.FIREWALL_EVENT))
        // biflowDirection
        .put(Integer.valueOf(239), (parser, length) ->
            parser.parseMapping(length, SpecialElementMappings.BIFLOW_DIRECTION))
        // observationPointType
        .put(Integer.valueOf(277), (parser, length) ->
            parser.parseMapping(length, SpecialElementMappings.OBSERVATION_POINT_TYPE))
        // anonymizationFlags
        .put(Integer.valueOf(285), (parser, length) -> {
            int value = (int) parser.parseLong(length);
            String stabilityClass = parser.parseMapping(value & 0b11, length, SpecialElementMappings.STABILITY_CLASS);
            String anonymizationFlags = parser.parseBitFlags(value & ~0b11, length,
                    SpecialElementMappings.ANONYMIZATION_FLAGS, Optional.empty());
            return "SC=" + stabilityClass + (!anonymizationFlags.isEmpty() ? "|" : "") + anonymizationFlags;
        })
        // anonymizationTechnique
        .put(Integer.valueOf(286), (parser, length) ->
            parser.parseMapping(length, SpecialElementMappings.ANONYMIZATION_TECHNIQUE))
        // natType
        .put(Integer.valueOf(297), (parser, length) ->
            parser.parseMapping(length, SpecialElementMappings.NAT_TYPE))
        // selectorAlgorithm
        .put(Integer.valueOf(304), (parser, length) ->
            parser.parseMapping(length, SpecialElementMappings.SELECTOR_ALGORITHM))
        // ingressInterfaceType
        .put(Integer.valueOf(368), (parser, length) ->
            parser.parseMapping(length, SpecialElementMappings.INTERFACE_TYPES))
        // egressInterfaceType
        .put(Integer.valueOf(369), (parser, length) ->
            parser.parseMapping(length, SpecialElementMappings.INTERFACE_TYPES))
        // valueDistributionMethod
        .put(Integer.valueOf(384), (parser, length) ->
            parser.parseMapping(length, SpecialElementMappings.VALUE_DISTRIBUTION_METHOD))
        // flowSelectorAlgorithm
        .put(Integer.valueOf(390), (parser, length) ->
            parser.parseMapping(length, SpecialElementMappings.FLOW_SELECTOR_ALGORITHM))
        // mibCaptureTimeSemantics
        .put(Integer.valueOf(448), (parser, length) ->
            parser.parseMapping(length, SpecialElementMappings.MIB_CAPTURE_TIME_SEMANTICS))
        // natQuotaExceededEvent
        .put(Integer.valueOf(466), (parser, length) ->
            parser.parseMapping(length, SpecialElementMappings.NAT_QUOTA_EXCEEDED_EVENT_TYPE))
        // natThresholdEvent
        .put(Integer.valueOf(467), (parser, length) ->
            parser.parseMapping(length, SpecialElementMappings.NAT_THRESHOLD_EVENT_TYPE))
        .build();

    private final Map<Integer, Entry<String, Converter>> mappings;

    InformationElementMappings() {
        mappings = loadElementMappings();
    }

    @SuppressFBWarnings("DM_DEFAULT_ENCODING")
    private static Map<Integer, Entry<String, Converter>> loadElementMappings() {
        Builder<Integer, Entry<String, Converter>> builder = ImmutableMap.builder();
        InputStream stream = InformationElementMappings.class.getClassLoader()
                .getResourceAsStream("ipfix-information-elements");
        if (stream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] terms = line.trim().split(" ");
                    Integer id = Integer.valueOf(terms[0]);
                    builder.put(id, new SimpleEntry<>(terms[1],
                        ELEMENT_CONVERTERS.getOrDefault(id,
                                DATA_TYPE_CONVERTERS.getOrDefault(terms[2], INTEGRAL_DATA_TYPE_CONVERTER))));
                }
            } catch (IOException e) {
                LOG.warn("Error reading ipfix-information-elements", e);
            } finally {
                try {
                    stream.close();
                } catch (IOException e) {
                    LOG.debug("Error closing stream", e);
                }
            }
        } else {
            LOG.warn("ipfix-information-elements file not found");
        }

        return builder.build();
    }

    Entry<String, Converter> get(int id) {
        final Entry<String, Converter> entry = mappings.get(Integer.valueOf(id));
        return entry != null ? entry : new SimpleEntry<>(Integer.toString(id), OCTETS_CONVERTER);
    }

    private abstract static class SpecialElementMappings {
        // https://www.iana.org/assignments/ipfix/ipfix.xhtml#forwarding-status
        static final Map<Integer, String> FORWARDING_STATUS = ImmutableMap.<Integer, String>builder()
            .put(Integer.valueOf(0b01000000), "Forwarded/Unknown")
            .put(Integer.valueOf(0b01000001), "Forwarded/Fragmented")
            .put(Integer.valueOf(0b01000010), "Forwarded/Not Fragmented")
            .put(Integer.valueOf(0b01000011), "Forwarded/Tunneled")

            .put(Integer.valueOf(0b10000000), "Dropped/Unknown")
            .put(Integer.valueOf(0b10000001), "Dropped/ACL deny")
            .put(Integer.valueOf(0b10000010), "Dropped/ACL drop")
            .put(Integer.valueOf(0b10000011), "Dropped/Unroutable")
            .put(Integer.valueOf(0b10000100), "Dropped/Adjacency")
            .put(Integer.valueOf(0b10000101), "Dropped/Fragmentation and DF set")
            .put(Integer.valueOf(0b10000110), "Dropped/Bad header checksum")
            .put(Integer.valueOf(0b10000111), "Dropped/Bad total Length")
            .put(Integer.valueOf(0b10001000), "Dropped/Bad header length")
            .put(Integer.valueOf(0b10001001), "Dropped/bad TTL")
            .put(Integer.valueOf(0b10001010), "Dropped/Policer")
            .put(Integer.valueOf(0b10001011), "Dropped/WRED")
            .put(Integer.valueOf(0b10001100), "Dropped/RPF")
            .put(Integer.valueOf(0b10001101), "Dropped/For us")
            .put(Integer.valueOf(0b10001110), "Dropped/Bad output interface")
            .put(Integer.valueOf(0b10001111), "Dropped/Hardware")

            .put(Integer.valueOf(0b11000000), "Consumed/Unknown")
            .put(Integer.valueOf(0b11000001), "Consumed/Punt Adjacency")
            .put(Integer.valueOf(0b11000010), "Consumed/Incomplete Adjacency")
            .put(Integer.valueOf(0b11000011), "Consumed/For us")
            .build();

        // https://www.iana.org/assignments/ipfix/ipfix.xml#ipfix-mpls-label-type
        static final Map<Integer, String> MPLS_LABEL_TYPE = ImmutableMap.<Integer, String>builder()
            .put(Integer.valueOf(0), "Unknown")
            .put(Integer.valueOf(1), "TE-MIDPT")
            .put(Integer.valueOf(2), "Pseudowire")
            .put(Integer.valueOf(3), "VPN")
            .put(Integer.valueOf(4), "BGP")
            .put(Integer.valueOf(5), "LDP")
            .build();

        static final Map<Integer, String> FLOW_DIRECTION = ImmutableMap.<Integer, String>builder()
            .put(Integer.valueOf(0), "ingress flow")
            .put(Integer.valueOf(1), "egress flow")
            .build();

        static final Map<Integer, String> NAT_ORIGINATING_ADDRESS_REALM = ImmutableMap.<Integer, String>builder()
            .put(Integer.valueOf(1), "Private")
            .put(Integer.valueOf(2), "Public")
            .build();

        // https://www.iana.org/assignments/ipfix/ipfix.xml#ipfix-nat-event-type
        static final Map<Integer, String> NAT_EVENT_TYPE = ImmutableMap.<Integer, String>builder()
            .put(Integer.valueOf(1), "NAT translation create")
            .put(Integer.valueOf(2), "NAT translation delete")
            .put(Integer.valueOf(3), "NAT Addresses exhausted")
            .put(Integer.valueOf(4), "NAT44 session create")
            .put(Integer.valueOf(5), "NAT44 session delete")
            .put(Integer.valueOf(6), "NAT64 session create")
            .put(Integer.valueOf(7), "NAT64 session delete")
            .put(Integer.valueOf(8), "NAT44 BIB create")
            .put(Integer.valueOf(9), "NAT44 BIB delete")
            .put(Integer.valueOf(10), "NAT64 BIB create")
            .put(Integer.valueOf(11), "NAT64 BIB delete")
            .put(Integer.valueOf(12), "NAT ports exhausted")
            .put(Integer.valueOf(13), "Quota Exceeded")
            .put(Integer.valueOf(14), "Address binding create")
            .put(Integer.valueOf(15), "Address binding delete")
            .put(Integer.valueOf(16), "Port block allocation")
            .put(Integer.valueOf(17), "Port block de-allocation")
            .put(Integer.valueOf(18), "Threshold Reached")
            .build();

        static final Map<Integer, String> FLOW_SELECTOR_ALGORITHM = ImmutableMap.<Integer, String>builder()
            .put(Integer.valueOf(1), "Systematic count-based Sampling")
            .put(Integer.valueOf(2), "Systematic time-based Sampling")
            .put(Integer.valueOf(3), "Random n-out-of-N Sampling")
            .put(Integer.valueOf(4), "Uniform probabilistic Sampling")
            .put(Integer.valueOf(5), "Property Match Filtering")
            .put(Integer.valueOf(6), "Hash-based Filtering using BOB")
            .put(Integer.valueOf(7), "Hash-based Filtering using IPSX")
            .put(Integer.valueOf(8), "Hash-based Filtering using CRC")
            .put(Integer.valueOf(9), "Flow-state Dependent Intermediate Flow Selection Process")
            .build();

        static final Map<Integer, String> NAT_QUOTA_EXCEEDED_EVENT_TYPE = ImmutableMap.<Integer, String>builder()
            .put(Integer.valueOf(1), "Maximum session entries")
            .put(Integer.valueOf(2), "Maximum BIB entries")
            .put(Integer.valueOf(3), "Maximum entries per user")
            .put(Integer.valueOf(4), "Maximum active hosts or subscribers")
            .put(Integer.valueOf(5), "Maximum fragments pending reassembly")
            .build();

        static final Map<Integer, String> NAT_THRESHOLD_EVENT_TYPE = ImmutableMap.<Integer, String>builder()
            .put(Integer.valueOf(1), "Address pool high threshold event")
            .put(Integer.valueOf(2), "Address pool low threshold event")
            .put(Integer.valueOf(3), "Address and port mapping high threshold event")
            .put(Integer.valueOf(4), "Address and port mapping per user high threshold event")
            .put(Integer.valueOf(5), "Global Address mapping high threshold event")
            .build();

        static final Map<Integer, String> TCP_CONTROL_BITS = ImmutableMap.<Integer, String>builder()
            .put(Integer.valueOf(0x0001), "FIN")
            .put(Integer.valueOf(0x0002), "SYN")
            .put(Integer.valueOf(0x0004), "RST")
            .put(Integer.valueOf(0x0008), "PSH")
            .put(Integer.valueOf(0x0010), "ACK")
            .put(Integer.valueOf(0x0020), "URG")
            .put(Integer.valueOf(0x0040), "ECE")
            .put(Integer.valueOf(0x0080), "CWR")
            .build();

        static final Map<Integer, String> IPV6_EXTENSION_HEADERS = ImmutableMap.<Integer, String>builder()
            .put(Integer.valueOf(0), "DST")
            .put(Integer.valueOf(1), "HOP")
            .put(Integer.valueOf(1 << 2), "UNK")
            .put(Integer.valueOf(1 << 3), "FRA0")
            .put(Integer.valueOf(1 << 4), "RH")
            .put(Integer.valueOf(1 << 5), "FRA1")
            .put(Integer.valueOf(1 << 11), "MOB")
            .put(Integer.valueOf(1 << 12), "ESP")
            .put(Integer.valueOf(1 << 13), "AH")
            .put(Integer.valueOf(1 << 14), "PAY")
            .build();

        static final Map<Integer, String> FLOW_END_REASON = ImmutableMap.<Integer, String>builder()
            .put(Integer.valueOf(1), "idle timeout")
            .put(Integer.valueOf(2), "active timeout")
            .put(Integer.valueOf(3), "end of Flow detected")
            .put(Integer.valueOf(4), "forced end")
            .put(Integer.valueOf(5), "lack of resources")
            .build();

        static final Map<Integer, String> FRAGMENT_FLAGS_ON = ImmutableMap.<Integer, String>builder()
            .put(Integer.valueOf(0x01), "Don't Fragment")
            .put(Integer.valueOf(0x02), "More Fragments")
            .build();

        static final Map<Integer, String> FRAGMENT_FLAGS_OFF = ImmutableMap.<Integer, String>builder()
            .put(Integer.valueOf(0x01), "May Fragment")
            .put(Integer.valueOf(0x02), "Last Fragment")
            .build();

        static final Map<Integer, String> IPV4_OPTIONS = ImmutableMap.<Integer, String>builder()
            .put(Integer.valueOf(0), "RR")
            .put(Integer.valueOf(1), "CIPSO")
            .put(Integer.valueOf(1 << 1), "E-SEC")
            .put(Integer.valueOf(1 << 2), "TS")
            .put(Integer.valueOf(1 << 3), "LSR")
            .put(Integer.valueOf(1 << 4), "SEC")
            .put(Integer.valueOf(1 << 5), "NOP")
            .put(Integer.valueOf(1 << 6), "EOOL")
            .put(Integer.valueOf(1 << 7), "ENCODE")
            .put(Integer.valueOf(1 << 8), "VISA")
            .put(Integer.valueOf(1 << 9), "FINN")
            .put(Integer.valueOf(1 << 10), "MTUR")
            .put(Integer.valueOf(1 << 11), "MTUP")
            .put(Integer.valueOf(1 << 12), "ZSU")
            .put(Integer.valueOf(1 << 13), "SSR")
            .put(Integer.valueOf(1 << 14), "SID")
            .put(Integer.valueOf(1 << 15), "DPS")
            .put(Integer.valueOf(1 << 16), "NSAPA")
            .put(Integer.valueOf(1 << 17), "SDB")
            .put(Integer.valueOf(1 << 18), "ADDEXT")
            .put(Integer.valueOf(1 << 19), "RTRALT")
            .put(Integer.valueOf(1 << 20), "TR")
            .put(Integer.valueOf(1 << 21), "EIP")
            .put(Integer.valueOf(1 << 22), "IMITD")
            .put(Integer.valueOf(1 << 24), "EXP")
            .put(Integer.valueOf(1 << 29), "QS")
            .put(Integer.valueOf(1 << 30), "UMP")
            .build();

        static final Map<Integer, String> FIREWALL_EVENT = ImmutableMap.<Integer, String>builder()
            .put(Integer.valueOf(0), "")
            .put(Integer.valueOf(1), "Flow Created")
            .put(Integer.valueOf(2), "Flow Deleted")
            .put(Integer.valueOf(3), "Flow Denied")
            .put(Integer.valueOf(4), "Flow Alert")
            .put(Integer.valueOf(5), "Flow Update")
            .build();

        static final Map<Integer, String> BIFLOW_DIRECTION = ImmutableMap.<Integer, String>builder()
            .put(Integer.valueOf(0), "arbitrary")
            .put(Integer.valueOf(1), "initiator")
            .put(Integer.valueOf(2), "reverseInitiator")
            .put(Integer.valueOf(3), "perimeter")
            .build();

        static final Map<Integer, String> OBSERVATION_POINT_TYPE = ImmutableMap.<Integer, String>builder()
            .put(Integer.valueOf(1), "Physical port")
            .put(Integer.valueOf(2), "Port channel")
            .put(Integer.valueOf(3), "Vlan")
            .build();

        static final Map<Integer, String> ANONYMIZATION_TECHNIQUE = ImmutableMap.<Integer, String>builder()
             .put(Integer.valueOf(0), "Undefined")
             .put(Integer.valueOf(1), "None")
             .put(Integer.valueOf(2), "Precision Degradation/Truncation")
             .put(Integer.valueOf(3), "Binning")
             .put(Integer.valueOf(4), "Enumeration")
             .put(Integer.valueOf(5), "Permutation")
             .put(Integer.valueOf(6), "Structured Permutation")
             .put(Integer.valueOf(7), "Reverse Truncation")
             .put(Integer.valueOf(8), "Noise")
             .put(Integer.valueOf(9), "Offset")
             .build();

        static final Map<Integer, String> MIB_CAPTURE_TIME_SEMANTICS = ImmutableMap.<Integer, String>builder()
             .put(Integer.valueOf(0), "undefined")
             .put(Integer.valueOf(1), "begin")
             .put(Integer.valueOf(2), "end")
             .put(Integer.valueOf(3), "export")
             .put(Integer.valueOf(4), "average")
             .build();

        static final Map<Integer, String> VALUE_DISTRIBUTION_METHOD = ImmutableMap.<Integer, String>builder()
             .put(Integer.valueOf(0), "Unspecified")
             .put(Integer.valueOf(1), "Start Interval")
             .put(Integer.valueOf(2), "End Interval")
             .put(Integer.valueOf(3), "Mid Interval")
             .put(Integer.valueOf(4), "Simple Uniform Distribution")
             .put(Integer.valueOf(5), "Proportional Uniform Distribution")
             .put(Integer.valueOf(6), "Simulated Process")
             .put(Integer.valueOf(7), "Direct")
             .build();

        static final Map<Integer, String> NAT_TYPE = ImmutableMap.<Integer, String>builder()
             .put(Integer.valueOf(0), "unknown")
             .put(Integer.valueOf(1), "NAT44 translated")
             .put(Integer.valueOf(2), "NAT64 translated")
             .put(Integer.valueOf(3), "NAT46 translated")
             .put(Integer.valueOf(4), "IPv4-->IPv4 (no NAT)")
             .put(Integer.valueOf(5), "NAT66 translated")
             .put(Integer.valueOf(6), "IPv6-->IPv6 (no NAT)")
             .build();

        // https://www.iana.org/assignments/psamp-parameters/psamp-parameters.xhtml
        static final Map<Integer, String> SELECTOR_ALGORITHM = ImmutableMap.<Integer, String>builder()
            .put(Integer.valueOf(0), "Reserved")
            .put(Integer.valueOf(1), "Systematic count-based Sampling")
            .put(Integer.valueOf(2), "Systematic time-based Sampling")
            .put(Integer.valueOf(3), "Random n-out-of-N Sampling")
            .put(Integer.valueOf(4), "Uniform probabilistic Sampling")
            .put(Integer.valueOf(5), "Property match Filtering")
            .put(Integer.valueOf(6), "Hash based Filtering using BOB")
            .put(Integer.valueOf(7), "Hash based Filtering using IPSX")
            .put(Integer.valueOf(8), "Hash based Filtering using CRC")
            .build();

        static final Map<Integer, String> ANONYMIZATION_FLAGS = ImmutableMap.<Integer, String>builder()
            .put(Integer.valueOf(1 << 2), "PmA")
            .put(Integer.valueOf(1 << 3), "LOR")
            .build();

        static final Map<Integer, String> STABILITY_CLASS = ImmutableMap.<Integer, String>builder()
            .put(Integer.valueOf(0b00), "Undefined")
            .put(Integer.valueOf(0b01), "Session")
            .put(Integer.valueOf(0b10), "Exporter-Collector Pair")
            .put(Integer.valueOf(0b11), "Stable")
            .build();

        static final Map<Integer, String> INTERFACE_TYPES;
        static final Map<Integer, String> PROTOCOL_NUMBERS;

        static {
            INTERFACE_TYPES = loadMappingsFile("if-types");
            PROTOCOL_NUMBERS = loadMappingsFile("protocol-numbers");
        }

        private static Map<Integer, String> loadMappingsFile(String file) {
            Properties props = new Properties();
            try {
                props.load(Optional.ofNullable(InformationElementMappings.class.getClassLoader()
                        .getResourceAsStream(file)).orElseGet(() -> {
                            LOG.warn("{} file not found", file);
                            return new ByteArrayInputStream(new byte[0]);
                        }));
            } catch (IOException e) {
                LOG.warn("Error reading {}", file, e);
            }

            final Builder<Integer, String> builder = ImmutableMap.builder();
            props.forEach((key, value) -> builder.put(Integer.valueOf(key.toString()), value.toString()));
            return builder.build();
        }
    }
}
