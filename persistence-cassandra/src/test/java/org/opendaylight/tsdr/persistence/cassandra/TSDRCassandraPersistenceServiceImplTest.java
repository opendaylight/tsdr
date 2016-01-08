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

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;

/**
 * @author <a href="mailto:saichler@gmail.com">Sharon Aicler</a>
 * Created by saichler on 12/16/15.
 */
public class TSDRCassandraPersistenceServiceImplTest {

    private Cluster cluster = Mockito.mock(Cluster.class);
    private Session session = Mockito.mock(Session.class);
    private CassandraStore store = Mockito.mock(CassandraStore.class);
    private TSDRCassandraPersistenceServiceImpl impl = new TSDRCassandraPersistenceServiceImpl();

    @Before
    public void before(){
        impl.start(store);
    }

    @Test
    public void testStoreMetric(){
        TSDRMetricRecord metricRecord = CassandraStoreTest.createMetricRecord();
        impl.store(metricRecord);
        Mockito.verify(store, Mockito.atLeast(1)).store(metricRecord);
    }

    @Test
    public void testStoreLog(){
        TSDRLogRecord logRecord = CassandraStoreTest.createLogRecord();
        impl.store(logRecord);
        Mockito.verify(store, Mockito.atLeast(1)).store(logRecord);
    }

    @Test
    public void testStoreList(){
        List<TSDRRecord> list = new ArrayList<>(2);
        TSDRLogRecord logRecord = CassandraStoreTest.createLogRecord();
        TSDRMetricRecord metricRecord = CassandraStoreTest.createMetricRecord();
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
    public void testPurgeTSDRRecords(){
        impl.purgeTSDRRecords(DataCategory.QUEUESTATS,0L);
        Mockito.verify(store,Mockito.atLeast(1)).purge(DataCategory.QUEUESTATS,0L);
    }

    @Test
    public void testPurgeAllTSDRRecords(){
        impl.purgeAllTSDRRecords(0L);
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
