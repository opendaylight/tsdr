/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.cassandra;

import java.util.Date;
import java.util.List;

import org.opendaylight.tsdr.spi.persistence.TsdrPersistenceService;
import org.opendaylight.tsdr.spi.util.TsdrPersistenceServiceUtil;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRLog;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRMetric;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrlogrecord.input.TSDRLogRecord;
/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class TSDRCassandraPersistenceServiceImpl implements TsdrPersistenceService{

    private CassandraStore store = null;

    public TSDRCassandraPersistenceServiceImpl(){
        TsdrPersistenceServiceUtil.setTsdrPersistenceService(this);
        System.out.println("Cassandra Store was initialized...");
    }

    @Override
    public void store(TSDRMetricRecord metricRecord) {
        store.store(metricRecord);
    }

    @Override
    public void store(TSDRLogRecord logRecord) {
        store.store(logRecord);
    }

    @Override
    public void store(List<TSDRRecord> metricRecordList) {
       for(TSDRRecord record:metricRecordList){
           if(record instanceof TSDRMetricRecord){
               store((TSDRMetricRecord)record);
           }else
           if(record instanceof TSDRLogRecord){
               store((TSDRLogRecord)record);
           }
       }
    }

    @Override
    public void start(int timeout) {
        store = new CassandraStore();
    }

    @Override
    public void stop(int timeout) {
        store.shutdown();
    }

    @Override
    public void purgeTSDRRecords(DataCategory category, Long retention_time){
        throw new UnsupportedOperationException("purgeTSDRRecords not yet supported by Cassandra");
    }

    @Override
    public void purgeAllTSDRRecords(Long retention_time){
        throw new UnsupportedOperationException("purgeAllTSDRRecords not yet supported by Cassandra");
    }

    @Override
    public List<TSDRMetricRecord> getTSDRMetricRecords(String tsdrMetricKey, long startDateTime, long endDateTime) {
        return store.getMetrics(tsdrMetricKey,startDateTime,endDateTime);
    }

    @Override
    public List<TSDRLogRecord> getTSDRLogRecords(String tsdrMetricKey, long startTime, long endTime) {
        return store.getLogs(tsdrMetricKey,startTime,endTime);
    }
}
