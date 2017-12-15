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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
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
public class HsqlDBStoreTest {
    private final Connection connection = Mockito.mock(Connection.class);
    private final Statement statement = Mockito.mock(Statement.class);
    private HsqlDBStore store = null;
    private final ResultSet resultSet = Mockito.mock(ResultSet.class);
    private boolean next = false;

    @Before
    public void before() throws SQLException {
        store = new HsqlDBStore(connection);
        Mockito.when(connection.createStatement()).thenReturn(statement);
        Mockito.when(statement.executeQuery(Mockito.anyString())).thenReturn(resultSet);
        Mockito.when(resultSet.next()).thenAnswer(invocationOnMock -> {
            next = !next;
            return next;
        });
        Mockito.when(resultSet.getString("KeyPath")).thenReturn(FormatUtil.getTSDRMetricKey(createMetricRecord()));
        Mockito.when(resultSet.getDouble("value")).thenReturn(11d);
        Mockito.when(resultSet.getString("value")).thenReturn(createLogRecord().getRecordFullText());
    }

    @After
    public void after() {
        store.close();
        File dir = new File("./tsdr");
        File[] files = dir.listFiles();
        for (File f : files) {
            f.delete();
        }
        dir.delete();
    }

    @AfterClass
    public static void afterClass() {
        File dir = new File("./tsdr");
        if (!dir.exists()) {
            return;
        }
        File[] files = dir.listFiles();
        for (File f : files) {
            f.delete();
        }
        dir.delete();
    }

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
    public void testStoreTSDRMetric() throws SQLException {
        store.store(createMetricRecord());
        Mockito.verify(statement, Mockito.atLeast(1)).execute(Mockito.anyString());
        store.close();
        Mockito.verify(connection, Mockito.atLeast(1)).close();
    }

    @Test
    public void testStoreTSDRLog() throws SQLException {
        store.store(createLogRecord());
        Mockito.verify(statement, Mockito.atLeast(1)).execute(Mockito.anyString());
        store.close();
        Mockito.verify(connection, Mockito.atLeast(1)).close();
    }

    @Test
    public void testGetMetricRecords() throws SQLException {
        TSDRMetricRecord rec = createMetricRecord();
        String key = FormatUtil.getTSDRMetricKey(rec);
        store.store(rec);
        List<TSDRMetricRecord> list = store.getTSDRMetricRecords(key, 0L, Long.MAX_VALUE, 10);
        Assert.assertNotNull(list);
        Assert.assertTrue(list.size() == 1);
        Assert.assertEquals(rec.getMetricValue(), list.get(0).getMetricValue());
    }

    @Test
    public void testGetMetricRecordsJob() throws SQLException {
        TSDRMetricRecord rec = createMetricRecord();
        String key = FormatUtil.getTSDRMetricKey(rec);
        store.store(rec);
        key = "[NID=Test]";
        List<TSDRMetricRecord> list = store.getTSDRMetricRecords(key, 0L, Long.MAX_VALUE, 10);
        Assert.assertNotNull(list);
        Assert.assertTrue(list.size() == 1);
        Assert.assertEquals(rec.getMetricValue(), list.get(0).getMetricValue());
    }

    @Test
    public void testGetLogRecords() throws SQLException {
        TSDRLogRecord rec = createLogRecord();
        String key = FormatUtil.getTSDRLogKey(rec);
        store.store(rec);
        List<TSDRLogRecord> list = store.getTSDRLogRecords(key, 0L, Long.MAX_VALUE, 10);
        Assert.assertNotNull(list);
        Assert.assertTrue(list.size() == 1);
        Assert.assertEquals(rec.getRecordFullText(), list.get(0).getRecordFullText());
    }

    @Test
    public void testGetLogRecordsJob() throws SQLException {
        TSDRLogRecord rec = createLogRecord();
        String key = FormatUtil.getTSDRLogKey(rec);
        store.store(rec);
        key = "[NID=Test]";
        List<TSDRLogRecord> list = store.getTSDRLogRecords(key, 0L, Long.MAX_VALUE, 10);
        Assert.assertNotNull(list);
        Assert.assertTrue(list.size() == 1);
        Assert.assertEquals(rec.getRecordFullText(), list.get(0).getRecordFullText());
    }

    @Test
    public void testPurge() throws SQLException {
        store.store(createMetricRecord());
        Mockito.verify(statement, Mockito.atLeast(1)).execute(Mockito.anyString());
        store.purge(DataCategory.EXTERNAL, 0L);
        Mockito.verify(statement, Mockito.atLeast(1)).execute(Mockito.anyString());
    }

    @Test
    public void testLoadPathsCache() throws SQLException {
        store.store(createMetricRecord());
    }
}
