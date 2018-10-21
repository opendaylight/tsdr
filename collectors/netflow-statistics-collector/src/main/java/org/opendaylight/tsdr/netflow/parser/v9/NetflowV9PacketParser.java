/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.netflow.parser.v9;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Consumer;
import org.opendaylight.tsdr.netflow.parser.AbstractNetflowPacketParser;
import org.opendaylight.tsdr.netflow.parser.MissingTemplateCache;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.tsdrlog.RecordAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecordBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netflow version 9 packet parser - see https://www.ietf.org/rfc/rfc3954.txt and
 * http://netflow.caligare.com/netflow_v9.htm.
 *
 * @author Thomas Pantelis
 */
class NetflowV9PacketParser extends AbstractNetflowPacketParser {
    static final String FLOW_SET_LOG_TEXT = "Data FlowSet";

    private static final Logger LOG = LoggerFactory.getLogger(NetflowV9PacketParser.class);

    private static final int TEMPLATE_FLOWSET_ID = 0;
    private static final int OPTIONS_TEMPLATE_FLOWSET_ID = 1;

    private final FlowsetTemplateCache flowsetTemplateCache;
    private final OptionsTemplateCache optionsTemplateCache;
    private final MissingTemplateCache missingTemplateCache;
    private final int totalRecordCount;
    private final long sourceId;
    private final String sourceIP;
    private final Long timestamp;
    private int recordCounter;

    NetflowV9PacketParser(byte[] data, int initialPosition, String sourceIP, FlowsetTemplateCache flowsetTemplateCache,
            OptionsTemplateCache optionsTemplateCache, MissingTemplateCache missingTemplateCache,
            TSDRLogRecordBuilder recordBuilder, Consumer<TSDRLogRecordBuilder> callback) {
        super(data, 9, initialPosition, recordBuilder, callback);
        this.sourceIP = sourceIP;
        this.flowsetTemplateCache = flowsetTemplateCache;
        this.optionsTemplateCache = optionsTemplateCache;
        this.missingTemplateCache = missingTemplateCache;

        this.totalRecordCount = parseShort();

        addHeaderAttribute("sys_uptime", parseIntString());
        this.timestamp = parseInt() * 1000;
        addHeaderAttribute("package_sequence", parseIntString());

        this.sourceId = parseInt();
        addHeaderAttribute("source_id", Long.toString(this.sourceId));

        LOG.debug("Packet version 9, total record count {}, headers: {}", totalRecordCount, headerAttributes());
    }

    private NetflowV9PacketParser(NetflowV9PacketParser other, int fromPosition, int bytesToCopy) {
        super(other, fromPosition, bytesToCopy);

        this.sourceIP = other.sourceIP;
        this.flowsetTemplateCache = other.flowsetTemplateCache;
        this.optionsTemplateCache = other.optionsTemplateCache;
        this.missingTemplateCache = other.missingTemplateCache;
        this.timestamp = other.timestamp;
        this.sourceId = other.sourceId;
        this.totalRecordCount = Integer.MAX_VALUE;
    }

    @Override
    public void parseRecords() {
        while (recordCounter < totalRecordCount && !endOfData()) {
            final OptionalInt possibleCount = parseNextRecords();
            if (!possibleCount.isPresent()) {
                return;
            }

            recordCounter += possibleCount.getAsInt();
        }

        missingTemplateCache.checkTemplates();
    }

    private OptionalInt parseNextRecords() {
        int start = position();
        int flowsetId = parseShort();
        int flowsetLength = parseShort();

        LOG.debug("parseNextRecord - flowsetId: {}, flowsetLength: {}, start: {}", flowsetId, flowsetLength, start);

        if (flowsetLength == 0) {
            return OptionalInt.empty();
        }

        switch (flowsetId) {
            case TEMPLATE_FLOWSET_ID:
                return OptionalInt.of(parseDataFlowsetTemplates(start, flowsetLength));

            case OPTIONS_TEMPLATE_FLOWSET_ID:
                return OptionalInt.of(parseOptionsTemplate(start, flowsetLength));

            default:
                return parseDataFlowset(start, flowsetId, flowsetLength);
        }
    }

    private OptionalInt parseDataFlowset(int start, int flowsetId, int flowsetLength) {
        Template flowsetTemplate = flowsetTemplateCache.get(sourceId, flowsetId, sourceIP);
        if (flowsetTemplate != null) {
            return parseDataFlowsetRecords(start, flowsetId, flowsetLength, flowsetTemplate);
        }

        OptionsTemplate optionsTemplate = optionsTemplateCache.get(sourceId, flowsetId, sourceIP);
        if (optionsTemplate != null) {
            return parseOptionDataRecords(start, flowsetId, flowsetLength, optionsTemplate);
        }

        LOG.debug("No template found for source Id {}, template Id {} - caching parser", sourceId, flowsetId);

        missingTemplateCache.put(sourceId, flowsetId, sourceIP, new NetflowV9PacketParser(this, start, flowsetLength));
        skip(flowsetLength - (position() - start));
        return OptionalInt.of(0);
    }

    private OptionalInt parseOptionDataRecords(int start, int templateId, int flowsetLength, OptionsTemplate template) {
        LOG.debug("Found options template {} - {}", templateId, template);

        int recordCount = template.getTotalLength() > 0
                ? (flowsetLength - (position() - start)) / template.getTotalLength() : 0;

        LOG.debug("Parsing {} options data records", recordCount);

        for (int i = 0; i < recordCount; i++) {
            StringBuilder recordText = new StringBuilder("Options record for ");
            int count = 0;
            for (TemplateField field : template.getScopeTemplate().getFields()) {
                int type = field.getType();
                int length = field.getLength();

                if (count > 0) {
                    recordText.append(", ");
                }

                count++;
                if (type < ScopeFieldType.values().length) {
                    recordText.append(ScopeFieldType.values()[type].displayName());
                } else {
                    recordText.append(ScopeFieldType.UNKNOWN.displayName()).append('(').append(type).append(')');
                }

                recordText.append(' ').append(parseLong(length));
            }

            callback().accept(recordBuilder().setRecordFullText(recordText.toString()).setTimeStamp(timestamp)
                    .setRecordAttributes(parseRecordAttributes(template.getOptionTemplate())));
        }

        skipPadding(start, flowsetLength);

        return OptionalInt.of(1);
    }

    private OptionalInt parseDataFlowsetRecords(int start, int templateId, int flowsetLength, Template template) {
        LOG.debug("Found data flowset template {} - {}", templateId, template);

        int recordCount = template.getTotalLength() > 0
                ? (flowsetLength - (position() - start)) / template.getTotalLength() : 0;

        LOG.debug("Parsing {} data flowset records", recordCount);

        for (int i = 0; i < recordCount; i++) {
            callback().accept(recordBuilder().setRecordFullText(FLOW_SET_LOG_TEXT).setTimeStamp(timestamp)
                    .setRecordAttributes(parseRecordAttributes(template)));
        }

        skipPadding(start, flowsetLength);

        return OptionalInt.of(recordCount);
    }

    private List<RecordAttributes> parseRecordAttributes(Template template) {
        List<RecordAttributes> recordAttrs = new ArrayList<>(headerAttributes());
        FieldType[] fieldTypes = FieldType.values();
        for (TemplateField field : template.getFields()) {
            int type = field.getType();
            int length = field.getLength();

            if (type < fieldTypes.length) {
                recordAttrs.add(newRecordAttributes(fieldTypes[type].toString(),
                        fieldTypes[type].toStringValue(this, length)));
            } else {
                recordAttrs.add(newRecordAttributes(Integer.toString(type), Long.toString(parseLong(length))));
            }
        }
        return recordAttrs;
    }

    private int parseDataFlowsetTemplates(int start, int totalLength) {
        int recordCount = 0;
        do {
            int templateId = parseShort();
            int fieldCount = parseShort();

            LOG.debug("Parsing data flowset template - id: {}, fieldCount: {}", templateId, fieldCount);

            Template.Builder templateBuilder = new Template.Builder();
            for (int i = 0; i < fieldCount; i++) {
                int type = parseShort();
                int length = parseShort();

                if (length <= 0) {
                    // This probably should never happen but check anyway
                    LOG.warn("Invalid length {} for type {} in template Id {}", length, type,
                            templateId);
                    continue;
                }

                templateBuilder.addField(type, length);
            }

            flowsetTemplateCache.put(sourceId, templateId, sourceIP, templateBuilder.build());
            recordCount++;
        } while (position() - start < totalLength);

        return recordCount;
    }

    private int parseOptionsTemplate(int start, int totalLength) {
        final int templateId = parseShort();
        final int scopeLength = parseShort();
        final int scopeFieldCount = scopeLength / 4;
        final int optionLength = parseShort();
        final int optionFieldCount = optionLength / 4;

        LOG.debug("Parsing options template - id: {}, scope fields: {}/{}, option fields: {}/{}", templateId,
                scopeLength, scopeFieldCount, optionLength, optionFieldCount);

        OptionsTemplate.Builder templateBuilder = new OptionsTemplate.Builder();
        for (int i = 0; i < scopeFieldCount; i++) {
            templateBuilder.addScopeField(parseShort(), parseShort());
        }

        for (int i = 0; i < optionFieldCount; i++) {
            templateBuilder.addOptionField(parseShort(), parseShort());
        }

        skipPadding(start, totalLength);

        optionsTemplateCache.put(sourceId, templateId, sourceIP, templateBuilder.build());
        return 1;
    }

    private enum FieldType {
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

    private enum ScopeFieldType {
        UNKNOWN("Unknown"),
        SYSTEM("System"),
        INTERFACE("Interface"),
        LINE_CARD("Line Card"),
        CACHE("Cache"),
        TEMPLATE("Template");

        private String displayName;

        ScopeFieldType(String displayName) {
            this.displayName = displayName;
        }

        String displayName() {
            return displayName;
        }
    }
}
