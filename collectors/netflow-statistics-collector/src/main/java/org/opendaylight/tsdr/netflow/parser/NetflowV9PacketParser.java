/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.netflow.parser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Consumer;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.tsdrlog.RecordAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netflow version 9 packet parser - see https://www.ietf.org/rfc/rfc3954.txt and
 * http://netflow.caligare.com/netflow_v9.htm.
 *
 * @author Thomas Pantelis
 */
public class NetflowV9PacketParser extends AbstractNetflowPacketParser {
    private static final Logger LOG = LoggerFactory.getLogger(NetflowV9PacketParser.class);

    private static final int TEMPLATE_FLOWSET_ID = 0;

    private final FlowsetTemplateCache templateCache;
    private final long sourceId;

    NetflowV9PacketParser(byte[] data, int initialPosition, FlowsetTemplateCache templateCache) {
        super(data, 9, initialPosition);
        this.templateCache = templateCache;

        addHeaderAttribute("flow_sequence", parseIntString());

        sourceId = parseInt();
        addHeaderAttribute("source_id", Long.toString(sourceId));
    }

    @Override
    public void parseRecords(Consumer<List<RecordAttributes>> callback) {
        int recordCounter = 0;
        while (recordCounter < totalRecordCount() && !endOfData()) {
            recordCounter += parseNextRecords(callback).orElse(totalRecordCount());
        }
    }

    private OptionalInt parseNextRecords(Consumer<List<RecordAttributes>> callback) {
        int start = position();
        int flowsetId = parseShort();
        int flowsetLength = parseShort();

        LOG.debug("parseNextRecord - flowsetId: {}, flowsetLength: {}, start: {}", flowsetId, flowsetLength, start);

        if (flowsetId == TEMPLATE_FLOWSET_ID) {
            return OptionalInt.of(parseTemplateFlowset(start, flowsetLength));
        }

        final Map<Integer, Integer> template = templateCache.get(sourceId, flowsetId);
        if (template == null) {
            LOG.warn("No flow set template found for source Id {}, template Id {}", sourceId, flowsetId);
            return OptionalInt.empty();
        }

        int recordLength = template.values().stream().mapToInt(len -> len).sum();

        LOG.debug("Found template {} - recordLength: {}: {}", flowsetId, recordLength, template);

        int recordCount = recordLength > 0 ? (flowsetLength - (position() - start)) / recordLength : 0;

        LOG.debug("Parsing {} records", recordCount);

        for (int i = 0; i < recordCount; i++) {
            callback.accept(parseDataFlowsetRecord(template));
        }

        int padding = flowsetLength - (position() - start);
        skip(padding);

        LOG.debug("Skip padding: {}", padding);

        return OptionalInt.of(recordCount);
    }

    private List<RecordAttributes> parseDataFlowsetRecord(final Map<Integer, Integer> template) {
        List<RecordAttributes> record = new ArrayList<>(headerAttributes());
        NetflowV9Attribs[] attributes = NetflowV9Attribs.values();
        for (Map.Entry<Integer, Integer> entry : template.entrySet()) {
            int attrId = entry.getKey();
            int attrLen = entry.getValue();

            if (attrId < attributes.length) {
                record.add(newRecordAttributes(attributes[attrId].toString(),
                        attributes[attrId].toStringValue(this, attrLen)));
            } else {
                record.add(newRecordAttributes(Integer.toString(attrId), Long.toString(parseLong(attrLen))));
            }
        }

        return record;
    }

    private int parseTemplateFlowset(int start, int flowsetLength) {
        int recordCount = 0;
        do {
            int templateId = parseShort();
            int fieldCount = parseShort();

            Map<Integer, Integer> template = new LinkedHashMap<>();
            for (int i = 0; i < fieldCount; i++) {
                int attrId = parseShort();
                int attrLen = parseShort();

                if (attrLen <= 0) {
                    // This probably should never happen but check anyway
                    LOG.warn("Invalid length {} for attribute Id {} in template Id {}", attrLen, attrId,
                            templateId);
                    continue;
                }

                template.put(Integer.valueOf(attrId), Integer.valueOf(attrLen));
            }

            templateCache.put(sourceId, templateId, template);
            recordCount++;
        } while (position() - start < flowsetLength);

        return recordCount;
    }

    private enum NetflowV9Attribs {
        UNKNOWN,
        IN_BYTES,
        IN_PKTS,
        FLOWS,
        PROTOCOL,
        SRC_TOS,
        TCP_FLAGS,
        L4_SRC_PORT,

        IPV4_SRC_ADDR() {
            @Override
            String toStringValue(NetflowV9PacketParser parser, int length) {
                return parser.parseIPv4Address();
            }
        },

        SRC_MASK,
        INPUT_SNMP,
        L4_DST_PORT,

        IPV4_DST_ADDR() {
            @Override
            String toStringValue(NetflowV9PacketParser parser, int length) {
                return parser.parseIPv4Address();
            }
        },

        DST_MASK,
        OUTPUT_SNMP,

        IPV4_NEXT_HOP() {
            @Override
            String toStringValue(NetflowV9PacketParser parser, int length) {
                return parser.parseIPv4Address();
            }
        },

        SRC_AS,
        DST_AS,

        BGP_IPV4_NEXT_HOP() {
            @Override
            String toStringValue(NetflowV9PacketParser parser, int length) {
                return parser.parseIPv4Address();
            }
        },

        MUL_DST_PKTS,
        MUL_DST_BYTES,
        LAST_SWITCHED,
        FIRST_SWITCHED,
        OUT_BYTES,
        OUT_PKTS,
        MIN_PKT_LNGTH,
        MAX_PKT_LNGTH,

        IPV6_SRC_ADDR() {
            @Override
            String toStringValue(NetflowV9PacketParser parser, int length) {
                return parser.parseIPv6Address();
            }
        },

        IPV6_DST_ADDR() {
            @Override
            String toStringValue(NetflowV9PacketParser parser, int length) {
                return parser.parseIPv6Address();
            }
        },

        IPV6_SRC_MASK,
        IPV6_DST_MASK,
        IPV6_FLOW_LABEL,
        ICMP_TYPE,
        MUL_IGMP_TYPE,
        SAMPLING_INTERVAL,
        SAMPLING_ALGORITHM,
        FLOW_ACTIVE_TIMEOUT,
        FLOW_INACTIVE_TIMEOUT,
        ENGINE_TYPE, ENGINE_ID,
        TOTAL_BYTES_EXP,
        TOTAL_PKTS_EXP,
        TOTAL_FLOWS_EXP,
        VENDOR_PRPRIETARY_1,
        IPV4_SRC_PREFIX,
        IPV4_DST_PREFIX,
        MPLS_TOP_LABEL_TYPE,
        MPLS_TOP_LABEL_IP_ADDR,
        FLOW_SAMPLER_ID,
        FLOW_SAMPLER_MODE,
        FLOW_SAMPLER_RANDOM_INTERVAL,
        VENDOR_PRPRIETARY_2,
        MIN_TTL,
        MAX_TTL,
        IPV4_IDENT,
        DST_TOS,

        IN_SRC_MAC() {
            @Override
            String toStringValue(NetflowV9PacketParser parser, int length) {
                return parser.parseMACAddress();
            }
        },

        OUT_DST_MAC() {
            @Override
            String toStringValue(NetflowV9PacketParser parser, int length) {
                return parser.parseMACAddress();
            }
        },

        SRC_VLAN,
        DST_VLAN,
        IP_PROTOCOL_VERSION,
        DIRECTION,

        IPV6_NEXT_HOP() {
            @Override
            String toStringValue(NetflowV9PacketParser parser, int length) {
                return parser.parseIPv6Address();
            }
        },

        BPG_IPV6_NEXT_HOP() {
            @Override
            String toStringValue(NetflowV9PacketParser parser, int length) {
                return parser.parseIPv6Address();
            }
        },

        IPV6_OPTION_HEADERS,
        VENDOR_PRPRIETARY_3,
        VENDOR_PRPRIETARY_4,
        VENDOR_PRPRIETARY_5,
        VENDOR_PRPRIETARY_6,
        VENDOR_PRPRIETARY_7,
        MPLS_LABEL_1,
        MPLS_LABEL_2,
        MPLS_LABEL_3,
        MPLS_LABEL_4,
        MPLS_LABEL_5,
        MPLS_LABEL_6,
        MPLS_LABEL_7,
        MPLS_LABEL_8,
        MPLS_LABEL_9,
        MPLS_LABEL_10,

        IN_DST_MAC() {
            @Override
            String toStringValue(NetflowV9PacketParser parser, int length) {
                return parser.parseMACAddress();
            }
        },

        OUT_SRC_MAC() {
            @Override
            String toStringValue(NetflowV9PacketParser parser, int length) {
                return parser.parseMACAddress();
            }
        },

        IF_NAME() {
            @Override
            String toStringValue(NetflowV9PacketParser parser, int length) {
                return parser.parseString(length);
            }
        },

        IF_DESC() {
            @Override
            String toStringValue(NetflowV9PacketParser parser, int length) {
                return parser.parseString(length);
            }
        },

        SAMPLER_NAME() {
            @Override
            String toStringValue(NetflowV9PacketParser parser, int length) {
                return parser.parseString(length);
            }
        },

        IN_PERMANENT_BYTES,
        IN_PERMANENT_PKTS;

        String toStringValue(NetflowV9PacketParser parser, int length) {
            return Long.toString(parser.parseLong(length));
        }
    }
}
