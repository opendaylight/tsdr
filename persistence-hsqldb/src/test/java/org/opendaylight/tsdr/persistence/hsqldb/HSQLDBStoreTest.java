/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hsqldb;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.tsdr.persistence.hsqldb.HSQLDBStore;
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
public class HSQLDBStoreTest {
    private Connection connection = Mockito.mock(Connection.class);
    private Statement statement = Mockito.mock(Statement.class);
    private HSQLDBStore store = new HSQLDBStore(connection);
    private ResultSet resultSet = Mockito.mock(ResultSet.class);
    private boolean next = false;

    @Before
    public void before() throws SQLException {
        Mockito.when(connection.createStatement()).thenReturn(statement);
        Mockito.when(statement.executeQuery(Mockito.anyString())).thenReturn(resultSet);
        Mockito.when(resultSet.next()).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable {
                next = !next;
                return next;
            }
        });
        Mockito.when(resultSet.getString("KeyPath")).thenReturn(FormatUtil.getTSDRMetricKey(createMetricRecord()));
        Mockito.when(resultSet.getDouble("value")).thenReturn(11d);
        Mockito.when(resultSet.getString("value")).thenReturn(createLogRecord().getRecordFullText());
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
    public void testStoreTSDRMetric() throws SQLException {
        store.store(createMetricRecord());
        Mockito.verify(statement,Mockito.atLeast(1)).execute(Mockito.anyString());
        store.shutdown();
        Mockito.verify(connection,Mockito.atLeast(1)).close();
    }

    @Test
    public void testStoreTSDRLog() throws SQLException {
        store.store(createLogRecord());
        Mockito.verify(statement,Mockito.atLeast(1)).execute(Mockito.anyString());
        store.shutdown();
        Mockito.verify(connection,Mockito.atLeast(1)).close();
    }

    @Test
    public void testGetMetricRecords() throws SQLException {
        TSDRMetricRecord rec = createMetricRecord();
        String key = FormatUtil.getTSDRMetricKey(rec);
        store.store(rec);
        List<TSDRMetricRecord> list = store.getTSDRMetricRecords(key,0L,Long.MAX_VALUE);
        Assert.assertNotNull(list);
        Assert.assertTrue(list.size()==1);
        Assert.assertEquals(rec.getMetricValue(),list.get(0).getMetricValue());
    }

    @Test
    public void testGetLogRecords() throws SQLException {
        TSDRLogRecord rec = createLogRecord();
        String key = FormatUtil.getTSDRLogKey(rec);
        store.store(rec);
        List<TSDRLogRecord> list = store.getTSDRLogRecords(key,0L,Long.MAX_VALUE);
        Assert.assertNotNull(list);
        Assert.assertTrue(list.size()==1);
        Assert.assertEquals(rec.getRecordFullText(),list.get(0).getRecordFullText());
    }

    @Test
    public void testPurge() throws SQLException {
        store.store(createMetricRecord());
        Mockito.verify(statement,Mockito.atLeast(1)).execute(Mockito.anyString());
        store.purge(DataCategory.EXTERNAL,0L);
        Mockito.verify(statement,Mockito.atLeast(1)).execute(Mockito.anyString());
    }

    @Test
    public void testLoadPathsCache() throws SQLException {
        store.store(createMetricRecord());
    }

    @Test
    public void testGetSession(){
        HSQLDBStore s = new HSQLDBStore();
    }
}