/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2015 xFlow Research Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.netflow;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.function.Consumer;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.tsdrlog.RecordAttributes;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.tsdrlog.RecordAttributesBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for parsing Netflow packets.
 *
 * @author <a href="mailto:saichler@gmail.com">Sharon Aicler</a>
 * @author <a href="mailto:muhammad.umair@xflowresearch.com">Umair Bhatti</a>
 * @author Thomas Pantelis
 */
public abstract class NetflowPacketParser {
    private static final Logger LOG = LoggerFactory.getLogger(NetflowPacketParser.class);

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

    private final List<RecordAttributes> headerAttributes = new ArrayList<>();
    private final byte[] data;
    private final int totalRecordCount;
    private int position;

    protected NetflowPacketParser(final byte[] data, final int version, final int initialPosition) {
        this.data = data;
        this.position = initialPosition;

        this.totalRecordCount = parseShort();

        addHeaderAttribute("version", Integer.toString(version));
        addHeaderAttribute("sys_uptime", parseIntString());
        addHeaderAttribute("unix_secs", parseIntString());

        LOG.debug("Packet version: {}, total record count: {}, headers: {}", version, totalRecordCount,
                headerAttributes);
    }

    protected NetflowPacketParser() {
        this.data = new byte[0];
        this.position = -1;
        totalRecordCount = 0;
    }

    public static NetflowPacketParser newInstance(final byte[] bytes) {
        int version = (int) parseLong(bytes, 0, 2);
        switch (version) {
            case 5:
                return new NetflowV5PacketParser(bytes, 2);
            case 9:
                return new NetflowV9PacketParser(bytes, 2);
            default:
                return new UnknownNetflowVersionPacketParser(version);
        }
    }

    public abstract void parseRecords(Consumer<List<RecordAttributes>> callback);

    protected int totalRecordCount() {
        return totalRecordCount;
    }

    protected List<RecordAttributes> headerAttributes() {
        return headerAttributes;
    }

    protected void addHeaderAttribute(String name, String value) {
        headerAttributes.add(newRecordAttributes(name, value));
    }

    protected static RecordAttributes newRecordAttributes(String name, String value) {
        return new RecordAttributesBuilder().setName(name).setValue(value).build();
    }

    protected boolean endOfData() {
        return position >= data.length;
    }

    protected void skip(int num) {
        position += num;
    }

    protected int position() {
        return position;
    }

    protected long parseInt() {
        return parseLong(4);
    }

    protected String parseIntString() {
        return Long.toString(parseInt());
    }

    protected int parseShort() {
        return (int) parseLong(2);
    }

    protected String parseShortString() {
        return Integer.toString(parseShort());
    }

    protected String parseByteString() {
        return Integer.toString((int) parseLong(1));
    }

    protected String parseIPv4Address() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 4; i++, position++) {
            if (i > 0) {
                builder.append('.');
            }

            builder.append(data[position] & 0xff);
        }

        return builder.toString();
    }

    protected String parseIPv6Address() {
        StringBuilder builder = new StringBuilder(39);
        for (int i = 0; i < 8; i++) {
            builder.append(Integer.toHexString(data[position + (i << 1)] << 8 & 0xff00
                    | data[position + (i << 1) + 1] & 0xff));

            if (i < 7) {
                builder.append(':');
            }
        }

        position += 16;
        return builder.toString();
    }

    protected String parseMACAddress() {
        StringBuilder builder = new StringBuilder(17);
        for (int i = 0; i < 6; i++, position++) {
            if (i > 0) {
                builder.append(':');
            }

            String term = Integer.toString(data[position] & 0xff, 16);
            if (term.length() == 1) {
                builder.append('0');
            }

            builder.append(term);
        }

        return builder.toString();
    }

    protected String parseString(int length) {
        return new String(parseBytes(length), Charset.defaultCharset());
    }

    protected byte[] parseBytes(int len) {
        byte[] ret = new byte[len];
        System.arraycopy(data, position, ret, 0, len);
        position += len;
        return ret;
    }

    protected long parseLong(int len) {
        long value = parseLong(data, position, len);
        position += len;
        return value;
    }

    static long parseLong(byte[] bytes, int off, int len) {
        long ret = 0;
        int done = off + len;
        for (int i = off; i < done; i++) {
            ret = (ret << 8 & 0xffffffff) + (bytes[i] & 0xff);
        }

        return ret;
    }

    /**
     * Netflow version 5 - see http://netflow.caligare.com/netflow_v5.htm.
     */
    private static class NetflowV5PacketParser extends NetflowPacketParser {
        private static final int FLOW_SIZE = 48;

        NetflowV5PacketParser(byte[] data, int initialPosition) {
            super(data, 5, initialPosition);

            addHeaderAttribute("unix_nsecs", parseIntString());
            addHeaderAttribute("flow_sequence", parseIntString());
            addHeaderAttribute("engine_type", parseByteString());
            addHeaderAttribute("engine_id", parseByteString());

            // sampling interval is 14 bits
            addHeaderAttribute("sampling_interval", Integer.toString(parseShort() & 0x3ff));
        }

        @Override
        public void parseRecords(Consumer<List<RecordAttributes>> callback) {
            for (int i = 0; i < totalRecordCount(); i++) {
                parseNextRecord(callback);
            }
        }

        private void parseNextRecord(Consumer<List<RecordAttributes>> callback) {
            final int start = position();

            List<RecordAttributes> record = new ArrayList<>(headerAttributes());
            record.add(newRecordAttributes("srcaddr", parseIPv4Address()));
            record.add(newRecordAttributes("dstaddr", parseIPv4Address()));
            record.add(newRecordAttributes("nexthop", parseIPv4Address()));
            record.add(newRecordAttributes("input", parseShortString()));
            record.add(newRecordAttributes("output", parseShortString()));
            record.add(newRecordAttributes("dPkts", parseIntString()));
            record.add(newRecordAttributes("dOctets", parseIntString()));

            long first = parseInt();
            record.add(newRecordAttributes("first", Long.toString(first)));
            long last = parseInt();
            record.add(newRecordAttributes("last", Long.toString(last)));

            record.add(newRecordAttributes("srcport", parseShortString()));
            record.add(newRecordAttributes("dstport", parseShortString()));

            skip(1);
            record.add(newRecordAttributes("tcp_flags", parseByteString()));
            record.add(newRecordAttributes("prot", parseByteString()));
            record.add(newRecordAttributes("tos", parseByteString()));
            record.add(newRecordAttributes("src_as", parseShortString()));
            record.add(newRecordAttributes("dst_as", parseShortString()));
            record.add(newRecordAttributes("src_mask", parseByteString()));
            record.add(newRecordAttributes("dst_mask", parseByteString()));
            record.add(newRecordAttributes("flow_duration", Long.toString(last - first)));

            skip(FLOW_SIZE - (position() - start));

            callback.accept(record);
        }
    }

    /**
     * Netflow version 9 - see https://www.ietf.org/rfc/rfc3954.txt and http://netflow.caligare.com/netflow_v9.htm.
     */
    private static class NetflowV9PacketParser extends NetflowPacketParser {
        private static final int TEMPLATE_FLOWSET_ID = 0;

        // TODO - this should be shared
        private final Map<TemmplateKey, Map<Integer, Integer>> templateMap = new HashMap<>();
        private final long sourceId;

        NetflowV9PacketParser(byte[] data, int initialPosition) {
            super(data, 9, initialPosition);

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

            final Map<Integer, Integer> template = templateMap.get(new TemmplateKey(sourceId, flowsetId));
            if (template == null) {
                LOG.warn("No flow set template found for source Id {}, template Id {}", sourceId, flowsetId);
                return OptionalInt.empty();
            }

            int recordLength = 0;
            for (Integer len: template.values()) {
                recordLength += len;
            }

            LOG.debug("Found template {} - recordLength: {}: {}", flowsetId, recordLength, template);

            int recordCount = (flowsetLength - (position() - start)) / recordLength;

            LOG.debug("Parsing {} records", recordCount);

            if (recordCount <= 0) {
                // Probably shouldn't happen.
                LOG.debug("No records in the flow set");
                return OptionalInt.of(0);
            }

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

                final TemmplateKey key = new TemmplateKey(sourceId, templateId);
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

                templateMap.put(key, template);
                recordCount++;

                LOG.debug("Parsed template - key: {}, {}", key, template);
            } while (position() - start < flowsetLength);

            return recordCount;
        }
    }

    private static class UnknownNetflowVersionPacketParser extends NetflowPacketParser {
        UnknownNetflowVersionPacketParser(int version) {
            LOG.warn("Received netflow packet with unknown/unsupported version {}", version);
        }

        @Override
        public void parseRecords(Consumer<List<RecordAttributes>> callback) {
        }
    }

    private static class TemmplateKey {
        private final long sourceId;
        private final int templateId;

        TemmplateKey(long sourceId, int templateId) {
            this.sourceId = sourceId;
            this.templateId = templateId;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (sourceId ^ sourceId >>> 32);
            result = prime * result + templateId;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }

            TemmplateKey other = (TemmplateKey) obj;
            return sourceId == other.sourceId && templateId == other.templateId;
        }

        @Override
        public String toString() {
            return "TemmplateKey [sourceId=" + sourceId + ", templateId=" + templateId + "]";
        }
    }
}
