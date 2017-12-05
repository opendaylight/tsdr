/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hsqldb;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;

/**
 * Unit tests for TsdrHsqlDBPersistenceServiceImpl.
 *
 * @author <a href="mailto:saichler@gmail.com">Sharon Aicler</a>
 */
public class TsdrHsqlDBPersistenceServiceImplTest {

    private final HsqlDBStore store = Mockito.mock(HsqlDBStore.class);
    private final TsdrHsqlDBPersistenceServiceImpl impl = new TsdrHsqlDBPersistenceServiceImpl(store);

    @Before
    public void before(){
    }

    @Test
    public void testStoreMetric() throws SQLException {
        TSDRMetricRecord metricRecord = HsqlDBStoreTest.createMetricRecord();
        impl.storeMetric(metricRecord);
        Mockito.verify(store, Mockito.atLeast(1)).store(metricRecord);
    }

    @Test
    public void testStoreLog() throws SQLException {
        TSDRLogRecord logRecord = HsqlDBStoreTest.createLogRecord();
        impl.storeLog(logRecord);
        Mockito.verify(store, Mockito.atLeast(1)).store(logRecord);
    }

    @Test
    public void testStoreMetricList() throws SQLException {
        List<TSDRMetricRecord> list = new ArrayList<>(1);
        TSDRMetricRecord metricRecord = HsqlDBStoreTest.createMetricRecord();
        list.add(metricRecord);
        impl.storeMetric(list);
        Mockito.verify(store, Mockito.atLeast(1)).store(metricRecord);
    }

    @Test
    public void testStoreLogList() throws SQLException {
        List<TSDRLogRecord> list = new ArrayList<>(1);
        TSDRLogRecord metricRecord = HsqlDBStoreTest.createLogRecord();
        list.add(metricRecord);
        impl.storeLog(list);
        Mockito.verify(store, Mockito.atLeast(1)).store(metricRecord);
    }

    @Test
    public void testPurgeTSDRRecords() throws SQLException {
        impl.purge(DataCategory.QUEUESTATS,0L);
        Mockito.verify(store,Mockito.atLeast(1)).purge(DataCategory.QUEUESTATS,0L);
    }

    @Test
    public void testPurgeAllTSDRRecords() throws SQLException {
        impl.purge(0L);
        for (DataCategory dc : DataCategory.values()) {
            Mockito.verify(store, Mockito.atLeast(1)).purge(dc, 0L);
        }
    }

    @Test
    public void testGetTSDRMetricRecords() throws SQLException {
        impl.getTSDRMetricRecords("Test",0L,0L);
        Mockito.verify(store,Mockito.atLeastOnce()).getTSDRMetricRecords("Test",0L,0L,1000);
    }

    @Test
    public void testGetTSDRLogRecords() throws SQLException {
        impl.getTSDRLogRecords("Test",0L,0L);
        Mockito.verify(store,Mockito.atLeastOnce()).getTSDRLogRecords("Test",0L,0L,1000);
    }
}
