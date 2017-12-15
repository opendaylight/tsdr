/*
 * Copyright (c) 2016 Frinx s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.persistence.elasticsearch;

import com.google.common.collect.Lists;
import com.google.gson.FieldNamingPolicy;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
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
 *
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
        private final String keyName;
        private final String keyValue;

        RecordKeysPayload(String keyName, String keyValue) {
            this.keyName = keyName;
            this.keyValue = keyValue;
        }
    }

    static final class RecordAttributesPayload {
        private final String name;
        private final String value;

        RecordAttributesPayload(String name, String value) {
            this.name = name;
            this.value = value;
        }
    }

    // TSDRMetricRecord specific attributes.
    private String metricName;
    private BigDecimal metricValue;

    // TSDRLogRecord specific attributes.
    private String recordFullText;

    // TSDRBinaryRecord specific attributes.
    private byte[] data;

    // Common attributes for TSDRLogRecord and TSDRBinaryRecord.
    private Integer index;
    private List<RecordAttributesPayload> recordAttributes;

    // Common attributes.
    private String nodeID;
    private List<RecordKeysPayload> recordKeyPayloadList;
    private DataCategory dataCategory;
    private Long timeStamp;

    void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    void setMetricValue(BigDecimal metricValue) {
        this.metricValue = metricValue;
    }

    void setRecordFullText(String recordFullText) {
        this.recordFullText = recordFullText;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    void setIndex(Integer index) {
        this.index = index;
    }

    void setRecordAttributes(List<RecordAttributesPayload> recordAttributes) {
        this.recordAttributes = recordAttributes;
    }

    void setNodeID(String nodeID) {
        this.nodeID = nodeID;
    }

    void setRecordKeys(List<RecordKeysPayload> recordKeys) {
        this.recordKeyPayloadList = recordKeys;
    }

    void setTSDRDataCategory(DataCategory category) {
        this.dataCategory = category;
    }

    void setTimeStamp(Long timeStamp) {
        this.timeStamp = timeStamp;
    }

    /**
     * Convert data from Elasticsearch to TSDR.
     */
    TSDRRecord toRecord(ElasticSearchStore.RecordType type) {
        List<RecordKeys> recordKeys = null;
        if (recordKeyPayloadList != null) {
            recordKeys = Lists.newArrayListWithCapacity(recordKeyPayloadList.size());
            recordKeys.addAll(recordKeyPayloadList.stream()
                    .map(p -> new RecordKeysBuilder().setKeyName(p.keyName).setKeyValue(p.keyValue).build())
                    .collect(Collectors.toList()));
        }

        switch (type) {
            case METRIC:
                return new TSDRMetricRecordBuilder()
                        .setMetricName(metricName)
                        .setMetricValue(metricValue)
                        .setNodeID(nodeID)
                        .setRecordKeys(recordKeys)
                        .setTSDRDataCategory(dataCategory)
                        .setTimeStamp(timeStamp)
                        .build();
            case LOG:
                return new TSDRLogRecordBuilder()
                        .setIndex(index)
                        .setNodeID(nodeID)
                        .setRecordAttributes(buildLogRecordAttributes())
                        .setRecordFullText(recordFullText)
                        .setRecordKeys(recordKeys)
                        .setTSDRDataCategory(dataCategory)
                        .setTimeStamp(timeStamp)
                        .build();
            case BINARY:
                return new TSDRBinaryRecordBuilder()
                        .setData(data)
                        .setIndex(index)
                        .setNodeID(nodeID)
                        .setRecordKeys(recordKeys)
                        .setRecordAttributes(recordAttributes.stream()
                                .map(p -> new org.opendaylight.yang.gen.v1.opendaylight.tsdr.binary.data.rev160325
                                            .tsdrbinary.RecordAttributesBuilder()
                                        .setName(p.name)
                                        .setValue(p.value)
                                        .build())
                                .collect(Collectors.toList()))
                        .setTSDRDataCategory(dataCategory)
                        .setTimeStamp(timeStamp)
                        .build();
            default:
                throw new IllegalArgumentException("Unknown record type");
        }
    }

    /**
     * Convert Elasticsearch RecordAttributes to TSDR RecordAttributes.
     */
    List<RecordAttributes> buildLogRecordAttributes() {
        List<RecordAttributes> attributes = null;
        if (recordAttributes != null) {
            attributes = recordAttributes.stream()
                    .map(p -> new org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.tsdrlog
                                .RecordAttributesBuilder()
                            .setName(p.name)
                            .setValue(p.value)
                            .build())
                    .collect(Collectors.toList());
        }
        return attributes;
    }
}
