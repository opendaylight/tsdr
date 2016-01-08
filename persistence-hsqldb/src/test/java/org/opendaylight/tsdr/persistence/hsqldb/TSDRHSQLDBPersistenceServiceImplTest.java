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
import org.opendaylight.tsdr.persistence.hsqldb.HSQLDBStore;
import org.opendaylight.tsdr.persistence.hsqldb.TSDRHSQLDBPersistenceServiceImpl;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;

/**
 * @author <a href="mailto:saichler@gmail.com">Sharon Aicler</a>
 * Created by saichler on 12/16/15.
 */
public class TSDRHSQLDBPersistenceServiceImplTest {

    private HSQLDBStore store = Mockito.mock(HSQLDBStore.class);
    private TSDRHSQLDBPersistenceServiceImpl impl = new TSDRHSQLDBPersistenceServiceImpl();

    @Before
    public void before(){
        impl.start(store);
    }

    @Test
    public void testStoreMetric() throws SQLException {
        TSDRMetricRecord metricRecord = HSQLDBStoreTest.createMetricRecord();
        impl.store(metricRecord);
        Mockito.verify(store, Mockito.atLeast(1)).store(metricRecord);
    }

    @Test
    public void testStoreLog() throws SQLException {
        TSDRLogRecord logRecord = HSQLDBStoreTest.createLogRecord();
        impl.store(logRecord);
        Mockito.verify(store, Mockito.atLeast(1)).store(logRecord);
    }

    @Test
    public void testStoreList() throws SQLException {
        List<TSDRRecord> list = new ArrayList<>(2);
        TSDRLogRecord logRecord = HSQLDBStoreTest.createLogRecord();
        TSDRMetricRecord metricRecord = HSQLDBStoreTest.createMetricRecord();
        list.add(metricRecord);
        list.add(logRecord);
        impl.store(list);
        Mockito.verify(store, Mockito.atLeast(1)).store(logRecord);
        Mockito.verify(store, Mockito.atLeast(1)).store(metricRecord);
    }

    @Test
    public void testStart(){
        impl.start(100);
    }

    @Test
    public void testStart2(){
        impl.start(store);
    }

    @Test
    public void testPurgeTSDRRecords() throws SQLException {
        impl.purgeTSDRRecords(DataCategory.QUEUESTATS,0L);
        Mockito.verify(store,Mockito.atLeast(1)).purge(DataCategory.QUEUESTATS,0L);
    }

    @Test
    public void testPurgeAllTSDRRecords() throws SQLException {
        impl.purgeAllTSDRRecords(0L);
        for(DataCategory dc:DataCategory.values()) {
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
