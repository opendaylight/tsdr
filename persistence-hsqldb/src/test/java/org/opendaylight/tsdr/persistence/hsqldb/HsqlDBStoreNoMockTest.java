/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hsqldb;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.tsdr.spi.util.FormatUtil;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;

/**
 * Unit tests for HsqlDBStore.
 *
 * @author <a href="mailto:saichler@gmail.com">Sharon Aicler</a>
 */
public class HsqlDBStoreNoMockTest {
    private final HsqlDBStore store = new HsqlDBStore();

    public static TSDRMetricRecord createMetricRecord() {
        TSDRMetricRecordBuilder builder = new TSDRMetricRecordBuilder();
        builder.setNodeID("Test");
        builder.setTimeStamp(System.currentTimeMillis());
        builder.setMetricName("Test");
        builder.setMetricValue(new BigDecimal(11D));
        builder.setTSDRDataCategory(DataCategory.EXTERNAL);
        List<RecordKeys> recs = new ArrayList<>();
        RecordKeysBuilder rb = new RecordKeysBuilder();
        rb.setKeyValue("Test1");
        rb.setKeyName("Test2");
        recs.add(rb.build());
        builder.setRecordKeys(recs);
        return builder.build();
    }

    public static TSDRLogRecord createLogRecord() {
        TSDRLogRecordBuilder builder = new TSDRLogRecordBuilder();
        builder.setNodeID("Test");
        builder.setTimeStamp(System.currentTimeMillis());
        builder.setTSDRDataCategory(DataCategory.EXTERNAL);
        builder.setRecordFullText("Some syslog text");
        builder.setIndex(1);
        List<RecordKeys> recs = new ArrayList<>();
        RecordKeysBuilder rb = new RecordKeysBuilder();
        rb.setKeyValue("Test1");
        rb.setKeyName("Test2");
        recs.add(rb.build());
        builder.setRecordKeys(recs);
        return builder.build();
    }

    @Test
    public void testStoreMetric() throws SQLException {
        store.purge(DataCategory.EXTERNAL,System.currentTimeMillis());
        TSDRMetricRecord rec = createMetricRecord();
        store.store(rec);
        String key = FormatUtil.getTSDRMetricKey(rec);
        List<TSDRMetricRecord> list = store.getTSDRMetricRecords(key,0L,Long.MAX_VALUE,10);
        Assert.assertNotNull(list);
        Assert.assertTrue(list.size() == 1);
        Assert.assertEquals(rec.getMetricValue(),list.get(0).getMetricValue());
    }

    @Test
    public void testGetLogRecords() throws SQLException {
        store.purge(DataCategory.EXTERNAL,System.currentTimeMillis());
        TSDRLogRecord rec = createLogRecord();
        store.store(rec);
        String key = FormatUtil.getTSDRLogKey(rec);
        List<TSDRLogRecord> list = store.getTSDRLogRecords(key,0L,Long.MAX_VALUE,10);
        Assert.assertNotNull(list);
        Assert.assertTrue(list.size() == 1);
        Assert.assertEquals(rec.getRecordFullText(),list.get(0).getRecordFullText());
    }
}
