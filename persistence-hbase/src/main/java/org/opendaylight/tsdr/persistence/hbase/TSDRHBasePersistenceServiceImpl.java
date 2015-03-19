/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hbase;

import org.opendaylight.tsdr.persistence.spi.TsdrPersistenceService;
import org.opendaylight.tsdr.util.TsdrPersistenceServiceUtil;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRMetric;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

/**
 * This class provides HBase implementation of TSDRPersistenceService.
 *
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 *
 * Created: Feb 24, 2015
 *
 *
 */
public class TSDRHBasePersistenceServiceImpl  implements
    TsdrPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(TSDRHBasePersistenceServiceImpl.class);


    public TSDRHBasePersistenceServiceImpl(){
        TsdrPersistenceServiceUtil.setTsdrPersistenceService(this);
        log.info("TSDRHBasePersistenceServiceImpl is initialized " + new Date());
    }
    /**
     * Store TSDRMetrics.
     */
    @Override
    public void store(TSDRMetricRecord metrics){
        log.debug("Entering store(TSDRMetrics)");
        //convert TSDRRecord to HBaseEntities
        HBaseEntity entity = convertToHBaseEntity(metrics);
        HBaseDataStoreFactory.getHBaseDataStore().create(entity);
         log.debug("Exiting store(TSDRMetrics)");
     }

    /**
     * Store a list of TSDRMetrics.
    */
    @Override
    public void store(List<TSDRMetricRecord> metricList){
        log.debug("Entering store(List<TSDRMetrics>)");
        if ( metricList != null && metricList.size() != 0){
            for(TSDRMetricRecord metrics: metricList){
               store(metrics);
            }
        }
        log.debug("Entering store(List<TSDRMetrics>)");
    }

    @Override public void start(int timeout) {

    }

    @Override public void stop(int timeout) {
        closeConnections();
        TsdrPersistenceServiceUtil.setTsdrPersistenceService(null);
    }

    /**
     * convert TSDRMetrics to HBaseEntity.
     * @param metrics
     * @return
    */
    private HBaseEntity convertToHBaseEntity(TSDRMetricRecord metrics){
        log.debug("Entering convertToHBaseEntity(TSDRMetrics)");
        HBaseEntity entity = new HBaseEntity();

        TSDRMetric metricData = metrics;

        if ( metricData != null){
             DataCategory dataCategory = metricData.getTSDRDataCategory();
             if (dataCategory != null){
                 entity = HBasePersistenceUtil.getEntityFromMetricStats(metricData, dataCategory);
             }
        }
        log.debug("Exiting convertToHBaseEntity(TSDRMetrics)");
        return entity;
     }


    /**
     * Close DB connections.
     */
    public void closeConnections(){
        List<String> tableNames = HBasePersistenceUtil.getTSDRHBaseTables();
        for ( String tableName: tableNames){
            HBaseDataStoreFactory.getHBaseDataStore().closeConnection(tableName);
        }
        return;
    }


}
