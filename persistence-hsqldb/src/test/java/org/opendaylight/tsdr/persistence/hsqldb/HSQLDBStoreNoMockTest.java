/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hsqldb;

import java.io.File;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.tsdr.spi.util.FormatUtil;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrlogrecord.input.TSDRLogRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;

/**
 * @author <a href="mailto:saichler@gmail.com">Sharon Aicler</a>
 * Created by saichler on 12/16/15.
 */
public class HSQLDBStoreNoMockTest {
    private HSQLDBStore store = new HSQLDBStore();

    public static TSDRMetricRecord createMetricRecord(){
        TSDRMetricRecordBuilder b = new TSDRMetricRecordBuilder();
        b.setNodeID("Test");
        b.setTimeStamp(System.currentTimeMillis());
        b.setMetricName("Test");
        b.setMetricValue(new BigDecimal(11D));
        b.setTSDRDataCategory(DataCategory.EXTERNAL);
        List<RecordKeys> recs = new ArrayList<>();
        RecordKeysBuilder rb = new RecordKeysBuilder();
        rb.setKeyValue("Test1");
        rb.setKeyName("Test2");
        recs.add(rb.build());
        b.setRecordKeys(recs);
        return b.build();
    }

    public static TSDRLogRecord createLogRecord(){
        TSDRLogRecordBuilder b = new TSDRLogRecordBuilder();
        b.setNodeID("Test");
        b.setTimeStamp(System.currentTimeMillis());
        b.setTSDRDataCategory(DataCategory.EXTERNAL);
        b.setRecordFullText("Some syslog text");
        b.setIndex(1);
        List<RecordKeys> recs = new ArrayList<>();
        RecordKeysBuilder rb = new RecordKeysBuilder();
        rb.setKeyValue("Test1");
        rb.setKeyName("Test2");
        recs.add(rb.build());
        b.setRecordKeys(recs);
        return b.build();
    }

    @Test
    public void testStoreMetric() throws SQLException {
        store.purge(DataCategory.EXTERNAL,System.currentTimeMillis());
        TSDRMetricRecord rec = createMetricRecord();
        store.store(rec);
        String key = FormatUtil.getTSDRMetricKey(rec);
        List<TSDRMetricRecord> list = store.getTSDRMetricRecords(key,0L,Long.MAX_VALUE,10);
        Assert.assertNotNull(list);
        Assert.assertTrue(list.size()==1);
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
        Assert.assertTrue(list.size()==1);
        Assert.assertEquals(rec.getRecordFullText(),list.get(0).getRecordFullText());
    }
}