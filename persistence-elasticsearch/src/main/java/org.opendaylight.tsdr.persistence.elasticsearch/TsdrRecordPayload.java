/*
 * Copyright (c) 2016 Frinx s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.persistence.elasticsearch;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.gson.FieldNamingPolicy;

import org.opendaylight.yang.gen.v1.opendaylight.tsdr.binary.data.rev160325.storetsdrbinaryrecord.input.TSDRBinaryRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.tsdrlog.RecordAttributes;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;

/**
 * Used for deserialization of {@link TSDRRecord} objects stored as JSON in the data store.
 * <p>
 * <p>
 * Note: the name of fields must respect {@link FieldNamingPolicy#UPPER_CAMEL_CASE} naming policy.
 *
 * @author Lukas Beles(lbeles@frinx.io)
 */
final class TsdrRecordPayload {
    public static final String ELK_DATA_CATEGORY = "TSDRDataCategory";
    public static final String ELK_METRIC_NAME = "MetricName";
    public static final String ELK_NODE_ID = "NodeID";
    public static final String ELK_TIMESTAMP = "TimeStamp";
    public static final String ELK_RK_KEY_NAME = "RecordKeys.KeyName";
    public static final String ELK_RK_KEY_VALUE = "RecordKeys.KeyValue";
    public static final String ELK_RA_KEY_NAME = "RecordAttributes.Name";
    public static final String ELK_RA_KEY_VALUE = "RecordAttributes.Value";

    static final class RecordKeysPayload {
        private String KeyName;
        private String KeyValue;

        void setKeyName(String keyName) {
            KeyName = keyName;
        }

        void setKeyValue(String keyValue) {
            KeyValue = keyValue;
        }
    }

    static final class RecordAttributesPayload {
        private String Name;
        private String Value;

        void setName(String name) {
            Name = name;
        }

        void setValue(String value) {
            Value = value;
        }
    }

    // TSDRMetricRecord specific attributes.
    private String MetricName;
    private BigDecimal MetricValue;

    // TSDRLogRecord specific attributes.
    private String RecordFullText;

    // TSDRBinaryRecord specific attributes.
    private byte[] Data;

    // Common attributes for TSDRLogRecord and TSDRBinaryRecord.
    private Integer Index;
    private List<RecordAttributesPayload> RecordAttributes;

    // Common attributes.
    private String NodeID;
    private List<RecordKeysPayload> RecordKeys;
    private DataCategory TSDRDataCategory;
    private Long TimeStamp;

    void setMetricName(String metricName) {
        MetricName = metricName;
    }

    void setMetricValue(BigDecimal metricValue) {
        MetricValue = metricValue;
    }

    void setRecordFullText(String recordFullText) {
        RecordFullText = recordFullText;
    }

    public void setData(byte[] data) {
        Data = data;
    }

    void setIndex(Integer index) {
        Index = index;
    }

    void setRecordAttributes(List<RecordAttributesPayload> recordAttributes) {
        RecordAttributes = recordAttributes;
    }

    void setNodeID(String nodeID) {
        NodeID = nodeID;
    }

    void setRecordKeys(List<RecordKeysPayload> recordKeys) {
        RecordKeys = recordKeys;
    }

    void setTSDRDataCategory(DataCategory TSDRDataCategory) {
        this.TSDRDataCategory = TSDRDataCategory;
    }

    void setTimeStamp(Long timeStamp) {
        TimeStamp = timeStamp;
    }

    /**
     * Convert data from Elasticsearch to TSDR.
     */
    TSDRRecord toRecord(ElasticsearchStore.RecordType type) {
        List<RecordKeys> recordKeys = null;
        if (RecordKeys != null) {
            recordKeys = Lists.newArrayListWithCapacity(RecordKeys.size());
            recordKeys.addAll(RecordKeys.stream()
                    .map(p -> new RecordKeysBuilder().setKeyName(p.KeyName).setKeyValue(p.KeyValue).build())
                    .collect(Collectors.toList()));
        }

        switch (type) {
            case METRIC:
                return new TSDRMetricRecordBuilder()
                        .setMetricName(MetricName)
                        .setMetricValue(MetricValue)
                        .setNodeID(NodeID)
                        .setRecordKeys(recordKeys)
                        .setTSDRDataCategory(TSDRDataCategory)
                        .setTimeStamp(TimeStamp)
                        .build();
            case LOG:
                return new TSDRLogRecordBuilder()
                        .setIndex(Index)
                        .setNodeID(NodeID)
                        .setRecordAttributes(buildLogRecordAttributes())
                        .setRecordFullText(RecordFullText)
                        .setRecordKeys(recordKeys)
                        .setTSDRDataCategory(TSDRDataCategory)
                        .setTimeStamp(TimeStamp)
                        .build();
            case BINARY:
                return new TSDRBinaryRecordBuilder()
                        .setData(Data)
                        .setIndex(Index)
                        .setNodeID(NodeID)
                        .setRecordKeys(recordKeys)
                        .setRecordAttributes(RecordAttributes.stream()
                                .map(p -> new org.opendaylight.yang.gen.v1.opendaylight.tsdr.binary.data.rev160325.tsdrbinary.RecordAttributesBuilder()
                                        .setName(p.Name)
                                        .setValue(p.Value)
                                        .build())
                                .collect(Collectors.toList()))
                        .setTSDRDataCategory(TSDRDataCategory)
                        .setTimeStamp(TimeStamp)
                        .build();
            default:
                throw new IllegalArgumentException("Unknown record type");
        }
    }

    /**
     * Convert Elasticsearch RecordAttributes to TSDR RecordAttributes
     */
    List<RecordAttributes> buildLogRecordAttributes() {
        List<RecordAttributes> attributes = null;
        if (RecordAttributes != null) {
            attributes = RecordAttributes.stream()
                    .map(p -> new org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.tsdrlog.RecordAttributesBuilder()
                            .setName(p.Name)
                            .setValue(p.Value)
                            .build())
                    .collect(Collectors.toList());
        }
        return attributes;
    }
}
