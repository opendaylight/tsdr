/*
 * Copyright (c) 2016 xFlow Research Inc. and others.  All rights reserved
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hbase;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.Test;
import org.opendaylight.tsdr.spi.model.TSDRConstants;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.tsdrlog.RecordAttributes;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;

public class HBasePersistenceUtilTest {
    private final HBasePersistenceUtil hbasePersistenceUtil = new HBasePersistenceUtil();

    @Test
    public void testGetEntityFromMetricStats() {
        String timeStamp = new Long(new Date().getTime()).toString();
        List<RecordKeys> recordKeys = new ArrayList<>();
        RecordKeys recordKey = new RecordKeysBuilder()
            .setKeyName(TSDRConstants.FLOW_TABLE_KEY_NAME)
            .setKeyValue("table1").build();
        recordKeys.add(recordKey);
        TSDRMetricRecordBuilder builder1 = new TSDRMetricRecordBuilder();
        final TSDRMetricRecord tsdrMetric1 = builder1.setMetricName("PacketsMatched")
                .setMetricValue(new BigDecimal(Double.parseDouble("20000000"))).setNodeID("node1")
                .setRecordKeys(recordKeys).setTSDRDataCategory(DataCategory.FLOWTABLESTATS)
                .setTimeStamp(new Long(timeStamp)).build();
        HBasePersistenceUtil.getEntityFromMetricStats(null, DataCategory.FLOWTABLESTATS);
        HBasePersistenceUtil.getEntityFromMetricStats(
                builder1.setMetricName("PMD").setMetricValue(null).setNodeID("node2").build(),
                DataCategory.FLOWTABLESTATS);
        HBasePersistenceUtil.getEntityFromMetricStats(
                builder1.setMetricName("PMD").setMetricValue(new BigDecimal(Double.parseDouble("20000000")))
                        .setNodeID("node2").setTimeStamp(null).build(),
                DataCategory.FLOWTABLESTATS);
        HBaseEntity entity = HBasePersistenceUtil.getEntityFromMetricStats(tsdrMetric1, DataCategory.FLOWTABLESTATS);
        assertTrue(entity != null);
    }

    @Test
    public void testGetEntityFromLogRecord() {
        final String timeStamp = new Long(new Date().getTime()).toString();
        List<RecordKeys> recordKeys = new ArrayList<>();
        RecordKeys recordKey1 = new RecordKeysBuilder()
            .setKeyName(DataCategory.SYSLOG.name())
            .setKeyValue("log1").build();
        recordKeys.add(recordKey1);
        TSDRLogRecordBuilder builder1 = new TSDRLogRecordBuilder();
        List<RecordAttributes> value = new ArrayList<>();
        value.add(mock(RecordAttributes.class));
        value.add(mock(RecordAttributes.class));
        TSDRLogRecord tsdrLog1 =   builder1.setIndex(1)
            .setRecordFullText("su root failed for lonvick")
            .setNodeID("node1.example.com")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.SYSLOG)
            .setRecordAttributes(value)
            .setTimeStamp(new Long(timeStamp)).build();
        HBasePersistenceUtil.getEntityFromLogRecord(null, DataCategory.SYSLOG);
        HBasePersistenceUtil.getEntityFromLogRecord(builder1.setNodeID("node1.example.com")
                .setTimeStamp(null).build(), DataCategory.SYSLOG);
        HBaseEntity entity = HBasePersistenceUtil.getEntityFromLogRecord(tsdrLog1, DataCategory.SYSLOG);
        assertTrue(entity != null);
    }
}
