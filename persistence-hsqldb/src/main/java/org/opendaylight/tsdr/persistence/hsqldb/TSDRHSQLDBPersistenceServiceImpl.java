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
import org.opendaylight.tsdr.model.TSDRConstants;
import org.opendaylight.tsdr.persistence.spi.TsdrPersistenceService;
import org.opendaylight.tsdr.util.TsdrPersistenceServiceUtil;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRRecord;
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
        System.out.println("TSDR HSQLDB Data Store was initialized.");
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
    public void store(List<TSDRMetricRecord> metricRecordList) {
       for(TSDRRecord record:metricRecordList){
           if(record instanceof TSDRMetricRecord){
               store((TSDRMetricRecord)record);
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
    public List<?> getMetrics(String tsdrMetricKey, Date startDateTime, Date endDateTime) {
        try {
            long startTime = 0;
            long endTime = 0;
            if (startDateTime != null) {
                startTime = startDateTime.getTime();
            } else{
                startTime = 0;
            }
            if ( endDateTime != null) {
                endTime = endDateTime.getTime();
            } else {
                endTime = Long.MAX_VALUE;
            }
            return store.getTSDRMetricRecords(tsdrMetricKey, startTime, endTime, TSDRConstants.MAX_RESULTS_FROM_LIST_METRICS_COMMAND);

        }catch(SQLException e){
            LOGGER.error("Failed to get Metric Records",e);
            return null;
        }
    }
}
