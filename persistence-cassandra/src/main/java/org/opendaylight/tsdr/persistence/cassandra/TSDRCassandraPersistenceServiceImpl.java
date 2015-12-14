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
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class TSDRCassandraPersistenceServiceImpl implements TsdrPersistenceService{
    private static final Logger LOGGER = LoggerFactory.getLogger(TSDRCassandraPersistenceServiceImpl.class);
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
    public void purgeTSDRRecords(DataCategory category, Long retentionTime){
        LOGGER.info("Execute Purge with Category {} and earlier than {}.",category.name(),new Date(retentionTime));
        store.purge(category,retentionTime);
    }

    @Override
    public void purgeAllTSDRRecords(Long retentionTime){
        for(DataCategory dataCategory:DataCategory.values()){
            store.purge(dataCategory,retentionTime);
        }
    }

    @Override
    public List<TSDRMetricRecord> getTSDRMetricRecords(String tsdrMetricKey, long startDateTime, long endDateTime) {
        return store.getTSDRMetricRecords(tsdrMetricKey,startDateTime,endDateTime);
    }

    @Override
    public List<TSDRLogRecord> getTSDRLogRecords(String tsdrMetricKey, long startTime, long endTime) {
        return store.getTSDRLogRecords(tsdrMetricKey,startTime,endTime);
    }
}
