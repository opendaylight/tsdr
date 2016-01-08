/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hsqldb;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import org.opendaylight.tsdr.spi.model.TSDRConstants;
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
public class TSDRHSQLDBPersistenceServiceImpl implements TsdrPersistenceService{
    private static final Logger LOGGER = LoggerFactory.getLogger(TSDRHSQLDBPersistenceServiceImpl.class);
    private HSQLDBStore store = null;

    public TSDRHSQLDBPersistenceServiceImpl(){
        TsdrPersistenceServiceUtil.setTsdrPersistenceService(this);
        System.out.println("HSQLDB Store was initialized...");
    }

    @Override
    public void store(TSDRMetricRecord metricRecord) {
        try {
            store.store(metricRecord);
        }catch(SQLException e){
            LOGGER.error("Failed to store record to database",e);
        }
    }

    @Override
    public void store(TSDRLogRecord logRecord) {
        try{
            store.store(logRecord);
        }catch(SQLException e){
            LOGGER.error("Failed to store record to database",e);
        }
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
        store = new HSQLDBStore();
    }

    public void start(HSQLDBStore s) {
        this.store = s;
    }

    @Override
    public void stop(int timeout) {
        store.shutdown();
    }

    @Override
    public void purgeTSDRRecords(DataCategory category, Long retentionTime){
        LOGGER.info("Execute Purge with Category {} and earlier than {}.",category.name(),new Date(retentionTime));
        try{
            store.purge(category,retentionTime);
        }catch(SQLException e){
            LOGGER.error("Failed to purge records from the database",e);
        }
    }

    @Override
    public void purgeAllTSDRRecords(Long retentionTime){
        for(DataCategory dataCategory:DataCategory.values()){
            try{
                store.purge(dataCategory,retentionTime);
            }catch(SQLException e){
                LOGGER.error("Failed to purge records from the database",e);
            }
        }
    }

    @Override
    public List<TSDRMetricRecord> getTSDRMetricRecords(String tsdrMetricKey, long startDateTime, long endDateTime) {
        try {
            return store.getTSDRMetricRecords(tsdrMetricKey, startDateTime, endDateTime, TSDRConstants.MAX_RESULTS_FROM_LIST_METRICS_COMMAND);
        }catch(SQLException e){
            LOGGER.error("Failed to get Metric Records",e);
            return null;
        }
    }

    @Override
    public List<TSDRLogRecord> getTSDRLogRecords(String tsdrMetricKey, long startTime, long endTime) {
        try{
            return store.getTSDRLogRecords(tsdrMetricKey,startTime,endTime,TSDRConstants.MAX_RESULTS_FROM_LIST_METRICS_COMMAND);
        }catch(SQLException e){
            LOGGER.error("Failed to get log Records",e);
            return null;
        }
    }
}
