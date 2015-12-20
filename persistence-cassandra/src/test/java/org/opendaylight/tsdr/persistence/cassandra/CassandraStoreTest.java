/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
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
public class CassandraStoreTest {
    private Cluster cluster = Mockito.mock(Cluster.class);
    private Session session = Mockito.mock(Session.class);
    private CassandraStore store = new CassandraStore(session,cluster);
    private ResultSet resultSet = Mockito.mock(ResultSet.class);
    private Row row = Mockito.mock(Row.class);
    private List<Row> rows = new ArrayList<>();

    @Before
    public void before(){
        Mockito.when(session.execute(Mockito.anyString())).thenReturn(resultSet);
        Mockito.when(resultSet.all()).thenReturn(rows);
        Mockito.when(row.getString("KeyPath")).thenReturn(FormatUtil.getTSDRMetricKey(createMetricRecord()));
        Mockito.when(row.getDouble("value")).thenReturn(11d);
        Mockito.when(row.getString("value")).thenReturn(createLogRecord().getRecordFullText());

        if(rows.isEmpty()){
            rows.add(row);
        }
    }

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
        List<RecordKeys> recs = new ArrayList<>();
        RecordKeysBuilder rb = new RecordKeysBuilder();
        rb.setKeyValue("Test1");
        rb.setKeyName("Test2");
        recs.add(rb.build());
        b.setRecordKeys(recs);
        return b.build();
    }

    @Test
    public void testCreateTables(){
        store.createTSDRTables();
        Mockito.verify(session,Mockito.atLeast(3)).execute(Mockito.anyString());
    }

    @Test
    public void testStoreTSDRMetric(){
        store.store(createMetricRecord());
        Mockito.verify(session,Mockito.atLeast(2)).execute(Mockito.anyString());
        store.shutdown();
        Mockito.verify(session,Mockito.atLeast(1)).close();
    }

    @Test
    public void testStoreTSDRLog(){
        store.store(createLogRecord());
        Mockito.verify(session,Mockito.atLeast(2)).execute(Mockito.anyString());
        store.shutdown();
        Mockito.verify(session,Mockito.atLeast(1)).close();
    }

    @Test
    public void testGetMetricRecords(){
        TSDRMetricRecord rec = createMetricRecord();
        String key = FormatUtil.getTSDRMetricKey(rec);
        store.store(rec);
        List<TSDRMetricRecord> list = store.getTSDRMetricRecords(key,0L,Long.MAX_VALUE);
        Assert.assertNotNull(list);
        Assert.assertTrue(list.size()==1);
        Assert.assertEquals(rec.getMetricValue(),list.get(0).getMetricValue());
    }

    @Test
    public void testGetLogRecords(){
        TSDRLogRecord rec = createLogRecord();
        String key = FormatUtil.getTSDRLogKey(rec);
        store.store(rec);
        List<TSDRLogRecord> list = store.getTSDRLogRecords(key,0L,Long.MAX_VALUE);
        Assert.assertNotNull(list);
        Assert.assertTrue(list.size()==1);
        Assert.assertEquals(rec.getRecordFullText(),list.get(0).getRecordFullText());
    }

    @Test
    public void testPurge(){
        store.store(createMetricRecord());
        Mockito.verify(session,Mockito.atLeast(2)).execute(Mockito.anyString());
        store.purge(DataCategory.EXTERNAL,0L);
        Mockito.verify(session,Mockito.atLeast(1)).execute(Mockito.anyString());
    }

    @Test
    public void testLoadPathsCache(){
        store.store(createMetricRecord());
        store.loadPathCache();
    }

    @Test
    public void testGetSession(){
        CassandraStore s = new CassandraStore();
    }
}