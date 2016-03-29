/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:saichler@gmail.com">Sharon Aicler</a>
 * Created by saichler on 12/16/15.
 */
public class TSDRCassandraPersistenceServiceImplTest {

    private Cluster cluster = Mockito.mock(Cluster.class);
    private Session session = Mockito.mock(Session.class);
    private CassandraStore store = Mockito.mock(CassandraStore.class);
    private TSDRCassandraPersistenceServiceImpl impl = new TSDRCassandraPersistenceServiceImpl(store);

    @Before
    public void before(){

    }

    @Test
    public void testStoreMetric(){
        TSDRMetricRecord metricRecord = CassandraStoreTest.createMetricRecord();
        impl.storeMetric(metricRecord);
        Mockito.verify(store, Mockito.atLeast(1)).store(metricRecord);
    }

    @Test
    public void testStoreLog(){
        TSDRLogRecord logRecord = CassandraStoreTest.createLogRecord();
        impl.storeLog(logRecord);
        Mockito.verify(store, Mockito.atLeast(1)).store(logRecord);
    }

    @Test
    public void testMetricStoreList(){
        List<TSDRMetricRecord> list = new ArrayList<>(2);
        TSDRMetricRecord metricRecord = CassandraStoreTest.createMetricRecord();
        list.add(metricRecord);
        impl.storeMetric(list);
        Mockito.verify(store, Mockito.atLeast(1)).store(metricRecord);
    }

    @Test
    public void testLogStoreList(){
        List<TSDRLogRecord> list = new ArrayList<>(2);
        TSDRLogRecord logRecord = CassandraStoreTest.createLogRecord();
        list.add(logRecord);
        impl.storeLog(list);
        Mockito.verify(store, Mockito.atLeast(1)).store(logRecord);
    }

    @Test
    public void testPurgeTSDRRecords(){
        impl.purge(DataCategory.QUEUESTATS,0L);
        Mockito.verify(store,Mockito.atLeast(1)).purge(DataCategory.QUEUESTATS,0L);
    }

    @Test
    public void testPurgeAllTSDRRecords(){
        impl.purge(0L);
        for(DataCategory dc:DataCategory.values()) {
            Mockito.verify(store, Mockito.atLeast(1)).purge(dc, 0L);
        }
    }

    @Test
    public void testGetTSDRMetricRecords(){
        impl.getTSDRMetricRecords("Test",0L,0L);
        Mockito.verify(store,Mockito.atLeastOnce()).getTSDRMetricRecords("Test",0L,0L,1000);
    }

    @Test
    public void testGetTSDRLogRecords(){
        impl.getTSDRLogRecords("Test",0L,0L);
        Mockito.verify(store,Mockito.atLeastOnce()).getTSDRLogRecords("Test",0L,0L,1000);
    }
}
