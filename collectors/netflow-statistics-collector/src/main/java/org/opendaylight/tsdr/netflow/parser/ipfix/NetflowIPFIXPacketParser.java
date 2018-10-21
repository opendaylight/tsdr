/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.netflow.parser.ipfix;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.opendaylight.tsdr.netflow.parser.AbstractNetflowPacketParser;
import org.opendaylight.tsdr.netflow.parser.MissingTemplateCache;
import org.opendaylight.tsdr.netflow.parser.ipfix.InformationElementMappings.Converter;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.tsdrlog.RecordAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecordBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netflow IPFIX (version 10) packet parser - see https://tools.ietf.org/search/rfc7011.
 *
 * @author Thomas Pantelis
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class NetflowIPFIXPacketParser extends AbstractNetflowPacketParser {
    static final String DATA_RECORD_TEXT = "Data Record";

    private static final Logger LOG = LoggerFactory.getLogger(NetflowIPFIXPacketParser.class);

    private static final int TEMPLATE_RECORD_ID = 2;
    private static final int OPTIONS_TEMPLATE_RECORD_ID = 3;

    private static final Map<Integer, String> SEMANTIC = ImmutableMap.<Integer, String>builder()
        .put(Integer.valueOf(0), "noneOf")
        .put(Integer.valueOf(1), "exactlyOneOf")
        .put(Integer.valueOf(2), "oneOrMoreOf")
        .put(Integer.valueOf(3), "allOf")
        .put(Integer.valueOf(4), "ordered")
        .put(Integer.valueOf(0xff), "")
        .build();

    private final FlowTemplateCache flowTemplateCache;
    private final OptionsTemplateCache optionsTemplateCache;
    private final MissingTemplateCache missingTemplateCache;
    private final InformationElementMappings informationElementMappings;
    private final long observationDomainID;
    private final String sourceIP;
    private final Long timestamp;
    private final int totalPacketLength;

    NetflowIPFIXPacketParser(byte[] data, int initialPosition, String sourceIP,
            FlowTemplateCache flowTemplateCache, OptionsTemplateCache optionsTemplateCache,
            MissingTemplateCache missingTemplateCache, InformationElementMappings informationElementMappings,
            TSDRLogRecordBuilder recordBuilder, Consumer<TSDRLogRecordBuilder> callback) {
        super(data, 10, initialPosition, recordBuilder, callback);
        this.sourceIP = sourceIP;
        this.flowTemplateCache = flowTemplateCache;
        this.optionsTemplateCache = optionsTemplateCache;
        this.missingTemplateCache = missingTemplateCache;
        this.informationElementMappings = informationElementMappings;

        totalPacketLength = parseShort();
        timestamp = parseInt() * 1000;

        addHeaderAttribute("Sequence_Number", parseIntString());

        observationDomainID = parseInt();
        addHeaderAttribute("Observation_Domain_ID", Long.toString(observationDomainID));

        LOG.debug("Packet version IPFIX, total length {}, headers: {}", totalPacketLength, headerAttributes());
    }

    private NetflowIPFIXPacketParser(NetflowIPFIXPacketParser other, int fromPosition, int bytesToCopy) {
        super(other, fromPosition, bytesToCopy);

        this.sourceIP = other.sourceIP;
        this.flowTemplateCache = other.flowTemplateCache;
        this.optionsTemplateCache = other.optionsTemplateCache;
        this.missingTemplateCache = other.missingTemplateCache;
        this.informationElementMappings = other.informationElementMappings;
        this.timestamp = other.timestamp;
        this.observationDomainID = other.observationDomainID;
        this.totalPacketLength = other.totalPacketLength - fromPosition;
    }

    @Override
    public void parseRecords() {
        while (position() < totalPacketLength && !endOfData()) {
            if (!parseNextRecords()) {
                break;
            }
        }

        missingTemplateCache.checkTemplates();
    }

    private boolean parseNextRecords() {
        int start = position();
        int setId = parseShort();
        int setLength = parseShort();

        LOG.debug("parseNextRecords - set ID: {}, set length: {}, start: {}", setId, setLength, start);

        if (setLength == 0) {
            return false;
        }

        switch (setId) {
            case TEMPLATE_RECORD_ID:
                return parseTemplatesRecords(start, setLength);

            case OPTIONS_TEMPLATE_RECORD_ID:
                return parseOptionsTemplates(start, setLength);

            default:
                return parseDataRecord(start, setId, setLength);
        }
    }

    private boolean parseDataRecord(int start, int setId, int setLength) {
        Template flowTemplate = flowTemplateCache.get(observationDomainID, setId, sourceIP);
        if (flowTemplate != null) {
            return parseDataRecords(start, setId, setLength, flowTemplate);
        }

        OptionsTemplate optionsTemplate = optionsTemplateCache.get(observationDomainID, setId, sourceIP);
        if (optionsTemplate != null) {
            return parseOptionDataRecords(start, setId, setLength, optionsTemplate);
        }

        LOG.debug("No template found for observation Domain ID {}, template Id {} - caching parser",
                observationDomainID, setId);

        missingTemplateCache.put(observationDomainID, setId, sourceIP,
                new NetflowIPFIXPacketParser(this, start, setLength));
        skip(setLength - (position() - start));
        return true;
    }

    private boolean parseDataRecords(int start, int templateId, int setLength, Template template) {
        LOG.debug("Found data flow template {} - {}", templateId, template);

        if (template.getTimestamp() > timestamp) {
            LOG.debug("Template timestamp {} is newer than the data record timestamp {} - ignoring data record",
                    template.getTimestamp(), timestamp);
            skip(setLength - (position() - start));
            return true;
        }

        int endOfSet = start + setLength;
        int count = 1;
        while (position() + template.getEstimatedLength() <= endOfSet) {
            LOG.debug("Parsing data record {}", count++);

            callback().accept(recordBuilder().setRecordFullText(DATA_RECORD_TEXT).setTimeStamp(timestamp)
                    .setRecordAttributes(parseRecordAttributes(template)));
        }

        skipPadding(start, setLength);

        return true;
    }

    private List<RecordAttributes> parseRecordAttributes(Template template) {
        return template.getFields().stream().map(field -> newRecordAttributes(field.getIdentifier(),
            field.getDataConverter().toStringValue(this, field.getLength())))
                .collect(() -> new ArrayList<>(headerAttributes()), ArrayList::add, ArrayList::addAll);
    }

    private boolean parseOptionDataRecords(int start, int templateId, int setLength, OptionsTemplate template) {
        LOG.debug("Found options template {} - {}", templateId, template);

        if (template.getScopeTemplate().getTimestamp() > timestamp) {
            LOG.debug("Template timestamp {} is newer than the data record timestamp {} - ignoring options record",
                    template.getScopeTemplate().getTimestamp(), timestamp);
            skip(setLength - (position() - start));
            return true;
        }

        int endOfSet = start + setLength;
        int estimatedLength = template.getScopeTemplate().getEstimatedLength()
                + template.getOptionTemplate().getEstimatedLength();
        int recordCount = 1;
        while (position() + estimatedLength <= endOfSet) {
            LOG.debug("Parsing options record {}", recordCount++);

            String recordText = "Options record for " + template.getScopeTemplate().getFields().stream().map(
                field -> field.getIdentifier() + " "  + field.getDataConverter().toStringValue(this, field.getLength()))
                    .collect(Collectors.joining(", "));

            callback().accept(recordBuilder().setRecordFullText(recordText).setTimeStamp(timestamp)
                    .setRecordAttributes(parseRecordAttributes(template.getOptionTemplate())));
        }

        skipPadding(start, setLength);

        return true;
    }

    private boolean parseTemplatesRecords(int start, int setLength) {
        int endOfSet = start + setLength;
        do {
            int templateId = parseShort();
            if (templateId == 0) {
                // Probably hit padding
                break;
            }

            int fieldCount = parseShort();
            if (fieldCount == 0) {
                // Indicates a template withdrawal - ignore with UDP transport
                continue;
            }

            LOG.debug("Parsing template record - id: {}, fieldCount: {}", templateId, fieldCount);

            Template.Builder templateBuilder = new Template.Builder(timestamp);
            for (int i = 0; i < fieldCount; i++) {
                templateBuilder.addField(parseTemplateField());
            }

            flowTemplateCache.put(observationDomainID, templateId, sourceIP, templateBuilder.build());
        } while (position() + 4 < endOfSet);

        skipPadding(start, setLength);

        return true;
    }

    private boolean parseOptionsTemplates(int start, int setLength) {
        int endOfSet = start + setLength;
        do {
            parseOptionsTemplateRecord();
        } while (position() + 4 < endOfSet);

        skipPadding(start, setLength);
        return true;
    }

    private void parseOptionsTemplateRecord() {
        final int templateId = parseShort();
        if (templateId == 0) {
            // Probably hit padding
            return;
        }

        final int fieldCount = parseShort();
        if (fieldCount == 0) {
            // Indicates a template withdrawal - ignore with UDP transport
            return;
        }

        final int scopeFieldCount = parseShort();
        final int optionFieldCount = fieldCount - scopeFieldCount;

        LOG.debug("Parsing options template - id: {}, scope fields: {}, option fields: {}", templateId,
                scopeFieldCount, optionFieldCount);

        OptionsTemplate.Builder templateBuilder = new OptionsTemplate.Builder(timestamp);
        for (int i = 0; i < scopeFieldCount; i++) {
            templateBuilder.addScopeField(parseTemplateField());
        }

        for (int i = 0; i < optionFieldCount; i++) {
            templateBuilder.addOptionField(parseTemplateField());
        }

        optionsTemplateCache.put(observationDomainID, templateId, sourceIP, templateBuilder.build());
    }

    TemplateField parseTemplateField() {
        int informationElementID = parseShort();
        int length = parseShort();

        boolean isEnterpriseID = (informationElementID & 0x8000) != 0;
        informationElementID &= ~0x8000;

        String identifier;
        Converter converter;
        if (isEnterpriseID) {
            identifier = parseIntString() + "." + Integer.toString(informationElementID);
            converter = InformationElementMappings.INTEGRAL_DATA_TYPE_CONVERTER;
        } else {
            Entry<String, Converter> entry = informationElementMappings.get(informationElementID);
            identifier = entry.getKey();
            converter = entry.getValue();
        }

        return new TemplateField(identifier, converter, length);
    }

    String parseBasicList(int length) {
        int start = position();

        String semantic = parseMapping(1, SEMANTIC);
        TemplateField templateField = parseTemplateField();

        LOG.debug("parseBasicList - length: {}, start {}, templateField: {}", length, start, templateField);

        StringBuilder builder = new StringBuilder();
        builder.append(templateField.getIdentifier()).append(": ").append(semantic).append(" {");
        int count = 0;
        do {
            if (count++ > 0) {
                builder.append(", ");
            }

            builder.append(templateField.getDataConverter().toStringValue(this, templateField.getLength()));
        } while (position() - start < length);

        return builder.append('}').toString();
    }

    int extractLength(int length) {
        if (length == 65535) {
            // Variable length - try 1 byte length first
            length = (int) parseLong(1);
            if (length == 255) {
                // 3 byte length - next 2 bytes are the actual length
                length = parseShort();
            }
        }
        return length;
    }

    String parseMapping(int length, Map<Integer, String> mappings) {
        return parseMapping((int) parseLong(length), length, mappings);
    }

    String parseMapping(int val, int length, Map<Integer, String> mappings) {
        Integer value = Integer.valueOf(val);
        String ret = mappings.get(value);
        return ret != null ? ret : value.toString();
    }

    String parseBitFlags(int length, Map<Integer, String> bitOnMappings) {
        return parseBitFlags(length, bitOnMappings, Optional.empty());
    }

    String parseBitFlags(int length, Map<Integer, String> bitOnMappings,
            Optional<Map<Integer, String>> bitOffMappings) {
        return parseBitFlags((int) parseLong(length), length, bitOnMappings, bitOffMappings);
    }

    String parseBitFlags(int value, int length, Map<Integer, String> bitOnMappings,
            Optional<Map<Integer, String>> bitOffMappings) {
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (int i = 0, mask = 1; i < length * 8; i++, mask <<= 1) {
            int bit = value & mask;
            if (bitOffMappings.isPresent()) {
                String str = bit == 0 ? bitOffMappings.get().get(Integer.valueOf(mask))
                        : bitOnMappings.get(Integer.valueOf(mask));
                if (str != null) {
                    if (count++ > 0) {
                        builder.append('|');
                    }

                    builder.append(str);
                }
            } else if (bit != 0) {
                if (count++ > 0) {
                    builder.append('|');
                }

                String str = bitOnMappings.get(Integer.valueOf(mask));
                builder.append(str != null ? str : "0x" + Integer.toHexString(mask));
            }
        }

        return builder.toString();
    }
}
