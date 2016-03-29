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
import org.opendaylight.tsdr.spi.persistence.TSDRBinaryPersistenceService;
import org.opendaylight.tsdr.spi.persistence.TSDRLogPersistenceService;
import org.opendaylight.tsdr.spi.persistence.TSDRMetricPersistenceService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.binary.data.rev160325.storetsdrbinaryrecord.input.TSDRBinaryRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class TSDRHSQLDBPersistenceServiceImpl implements TSDRMetricPersistenceService,TSDRLogPersistenceService, TSDRBinaryPersistenceService{
    private static final Logger LOGGER = LoggerFactory.getLogger(TSDRHSQLDBPersistenceServiceImpl.class);
    private HSQLDBStore store = null;

    public TSDRHSQLDBPersistenceServiceImpl(){
        store = new HSQLDBStore();
        LOGGER.info("TSDR HSQLDB Data Store was initialized.");
    }

    public TSDRHSQLDBPersistenceServiceImpl(HSQLDBStore store){
        this.store = store;
        LOGGER.info("TSDR HSQLDB Data Store was initialized.");
    }

    @Override
    public void storeMetric(TSDRMetricRecord metricRecord) {
        try {
            store.store(metricRecord);
        }catch(SQLException e){
            LOGGER.error("Failed to store record to database",e);
        }
    }

    @Override
    public void storeLog(TSDRLogRecord logRecord) {
        try{
            store.store(logRecord);
        }catch(SQLException e){
            LOGGER.error("Failed to store record to database",e);
        }
    }

    @Override
    public void storeMetric(List<TSDRMetricRecord> metricRecordList) {
       for(TSDRMetricRecord record:metricRecordList) {
           storeMetric(record);
       }
    }

    @Override
    public void storeLog(List<TSDRLogRecord> logRecordList) {
        for(TSDRLogRecord record:logRecordList) {
            storeLog(record);
        }
    }

    @Override
    public void purge(DataCategory category, long retentionTime){
        LOGGER.info("Execute Purge with Category {} and earlier than {}.",category.name(),new Date(retentionTime));
        try{
            store.purge(category,retentionTime);
        }catch(SQLException e){
            LOGGER.error("Failed to purge records from the database",e);
        }
    }

    @Override
    public void purge(long retentionTime){
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

    @Override
    public List<TSDRBinaryRecord> getTSDRBinaryRecords(String tsdrMetricKey, long startDateTime, long endDateTime) {
        //@TODO - Add code to retrieve binary data
        return null;
    }

    @Override
    public void storeBinary(TSDRBinaryRecord binaryRecord) {
        //@TODO - Add code to store binary data
    }

    @Override
    public void storeBinary(List<TSDRBinaryRecord> recordList) {
       //@TODO - Add code to store binary data
    }
}
