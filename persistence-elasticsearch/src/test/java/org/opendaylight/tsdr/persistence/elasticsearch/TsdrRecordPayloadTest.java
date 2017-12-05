/*
 * Copyright (c) 2016 Frinx s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.persistence.elasticsearch;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.Lists;
import java.math.BigDecimal;
import org.junit.Test;
import org.opendaylight.tsdr.persistence.elasticsearch.TsdrRecordPayload.RecordAttributesPayload;
import org.opendaylight.tsdr.persistence.elasticsearch.TsdrRecordPayload.RecordKeysPayload;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.binary.data.rev160325.storetsdrbinaryrecord.input.TSDRBinaryRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRRecord;

/**
 * Test method toStore of {@link TsdrRecordPayload}. All possible cases
 *
 * @author Lukas Beles(lbeles@frinx.io)
 */
public class TsdrRecordPayloadTest {

    /**
     * Test conversion of Metric Record.
     */
    @Test
    public void toMetricRecord() throws Exception {
        final String nodeId = "TestNodeID";
        final Long timestamp = 1257894000000000000L;
        final DataCategory category = DataCategory.EXTERNAL;
        final String name = "TestName";
        final BigDecimal value = new BigDecimal(8128);
        final String rkName = "TestRKName";
        final String rkValue = "TestRKValue";

        RecordKeysPayload recordKeys = new RecordKeysPayload(rkName, rkValue);
        TsdrRecordPayload payload = new TsdrRecordPayload();
        payload.setNodeID(nodeId);
        payload.setTimeStamp(timestamp);
        payload.setTSDRDataCategory(category);
        payload.setMetricName(name);
        payload.setMetricValue(value);
        payload.setRecordKeys(Lists.newArrayList(recordKeys));

        TSDRRecord record = payload.toRecord(ElasticSearchStore.RecordType.METRIC);
        assertThat(record).isInstanceOf(TSDRMetricRecord.class);

        TSDRMetricRecord mr = (TSDRMetricRecord) record;
        assertThat(mr.getNodeID()).isEqualTo(nodeId);
        assertThat(mr.getTimeStamp()).isEqualTo(timestamp);
        assertThat(mr.getTSDRDataCategory()).isEqualTo(category);
        assertThat(mr.getMetricName()).isEqualTo(name);
        assertThat(mr.getMetricValue()).isEqualTo(value);
        assertThat(mr.getRecordKeys()).isNotEmpty();
        assertThat(mr.getRecordKeys().size()).isEqualTo(1);
        assertThat(mr.getRecordKeys().get(0).getKeyName()).isEqualTo(rkName);
        assertThat(mr.getRecordKeys().get(0).getKeyValue()).isEqualTo(rkValue);
    }

    /**
     * Test conversion of Log Record.
     */
    @Test
    public void toLogRecord() throws Exception {
        final String nodeId = "TestNodeID";
        final Long timestamp = 1257894000000000000L;
        final DataCategory category = DataCategory.EXTERNAL;
        final Integer index = 1;
        final String text = "Test Text";
        final String rkName = "TestRKName";
        final String rkValue = "TestRKValue";
        final String raName = "TestRAName";
        final String raValue = "TestRAValue";

        RecordAttributesPayload recordAttributes = new RecordAttributesPayload(raName, raValue);
        RecordKeysPayload recordKeys = new RecordKeysPayload(rkName, rkValue);
        TsdrRecordPayload payload = new TsdrRecordPayload();
        payload.setNodeID(nodeId);
        payload.setTimeStamp(timestamp);
        payload.setTSDRDataCategory(category);
        payload.setIndex(index);
        payload.setRecordFullText(text);
        payload.setRecordAttributes(Lists.newArrayList(recordAttributes));
        payload.setRecordKeys(Lists.newArrayList(recordKeys));

        TSDRRecord record = payload.toRecord(ElasticSearchStore.RecordType.LOG);
        assertThat(record).isInstanceOf(TSDRLogRecord.class);

        TSDRLogRecord lr = (TSDRLogRecord) record;
        assertThat(lr.getNodeID()).isEqualTo(nodeId);
        assertThat(lr.getTimeStamp()).isEqualTo(timestamp);
        assertThat(lr.getTSDRDataCategory()).isEqualTo(category);
        assertThat(lr.getIndex()).isEqualTo(index);
        assertThat(lr.getRecordFullText()).isEqualTo(text);
        assertThat(lr.getRecordAttributes()).isNotEmpty();
        assertThat(lr.getRecordAttributes().size()).isEqualTo(1);
        assertThat(lr.getRecordAttributes().get(0).getName()).isEqualTo(raName);
        assertThat(lr.getRecordAttributes().get(0).getValue()).isEqualTo(raValue);
        assertThat(lr.getRecordKeys()).isNotEmpty();
        assertThat(lr.getRecordKeys().size()).isEqualTo(1);
        assertThat(lr.getRecordKeys().get(0).getKeyName()).isEqualTo(rkName);
        assertThat(lr.getRecordKeys().get(0).getKeyValue()).isEqualTo(rkValue);
    }

    /**
     * Test conversion of Binary Record.
     */
    @Test
    public void toBinaryRecord() throws Exception {
        final String nodeId = "TestNodeID";
        final Long timestamp = 1257894000000000000L;
        final DataCategory category = DataCategory.EXTERNAL;
        final Integer index = 1;
        final byte[] data = "Test Data".getBytes();
        final String rkName = "TestRKName";
        final String rkValue = "TestRKValue";
        final String raName = "TestRAName";
        final String raValue = "TestRAValue";

        RecordAttributesPayload recordAttributes = new RecordAttributesPayload(raName, raValue);
        RecordKeysPayload recordKeys = new RecordKeysPayload(rkName, rkValue);
        TsdrRecordPayload payload = new TsdrRecordPayload();
        payload.setNodeID(nodeId);
        payload.setTimeStamp(timestamp);
        payload.setTSDRDataCategory(category);
        payload.setIndex(index);
        payload.setData(data);
        payload.setRecordAttributes(Lists.newArrayList(recordAttributes));
        payload.setRecordKeys(Lists.newArrayList(recordKeys));

        TSDRRecord record = payload.toRecord(ElasticSearchStore.RecordType.BINARY);
        assertThat(record).isInstanceOf(TSDRBinaryRecord.class);

        TSDRBinaryRecord br = (TSDRBinaryRecord) record;
        assertThat(br.getNodeID()).isEqualTo(nodeId);
        assertThat(br.getTimeStamp()).isEqualTo(timestamp);
        assertThat(br.getTSDRDataCategory()).isEqualTo(category);
        assertThat(br.getIndex()).isEqualTo(index);
        assertThat(br.getData()).isEqualTo(data);
        assertThat(br.getRecordAttributes()).isNotEmpty();
        assertThat(br.getRecordAttributes().size()).isEqualTo(1);
        assertThat(br.getRecordAttributes().get(0).getName()).isEqualTo(raName);
        assertThat(br.getRecordAttributes().get(0).getValue()).isEqualTo(raValue);
        assertThat(br.getRecordKeys()).isNotEmpty();
        assertThat(br.getRecordKeys().size()).isEqualTo(1);
        assertThat(br.getRecordKeys().get(0).getKeyName()).isEqualTo(rkName);
        assertThat(br.getRecordKeys().get(0).getKeyValue()).isEqualTo(rkValue);
    }
}
