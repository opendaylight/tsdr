/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.datastorage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import org.opendaylight.tsdr.datastorage.persistence.TSDRPersistenceServiceFactory;
import org.opendaylight.tsdr.persistence.DataStoreType;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.StoreOFStatsInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.StoreTSDRMetricRecordInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.StoreTSDRMetricRecordInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storeofstats.input.TSDROFStats;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;

/**
 * TSDR storage service implementation class.
 *
 * <p>
 * It takes the data collected from data collection service as input, convert it
 * into TSDR data model, and then send a request to TSDR persistence service to
 * store into the persistence data store.
 * </p>
 *
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 *
 *    Created: March 1, 2015
 */
public class TSDRStorageServiceImpl implements TSDRService, AutoCloseable {

     private static final Logger log = LoggerFactory
        .getLogger(TSDRStorageServiceImpl.class);
     /**
     * stores TSDRMetricRecord.
     *
     */
    @Override
    public Future<RpcResult<java.lang.Void>> storeTSDRMetricRecord(
        StoreTSDRMetricRecordInput input) {
        log.debug("Entering TSDRStorageService.storeTSDRMetrics()");
        if ( input == null || input.getTSDRMetricRecord() == null){
            log.error("Input of storeTSDRMetrics is null");
            return null;
        }
        List<TSDRMetricRecord> tsdrMetricRecordList = input.getTSDRMetricRecord();
        TSDRPersistenceServiceFactory.getTSDRPersistenceDataStore().store(
            tsdrMetricRecordList);
        log.debug("Exiting TSDRStorageService.storeTSDRMetrics()");
        return Futures.immediateFuture(RpcResultBuilder.<Void> success()
            .build());
    }

    /**
     * The API to store a list of TSDROFStats.
     *
     */
    @Override
    public Future<RpcResult<java.lang.Void>> storeOFStats(StoreOFStatsInput input) {
        log.debug("Entering TSDRStorageService.store()");
        if ( input == null || input.getTSDROFStats() == null){
            log.error("Input of store(StoreInput input) is null");
            return null;
        }
        Future<RpcResult<java.lang.Void>> result = Futures.immediateFuture(RpcResultBuilder.<Void> success()
                .build());
        List<TSDROFStats> stats = input.getTSDROFStats();
        List<TSDRMetricRecord> metricList = new ArrayList<TSDRMetricRecord>();
        for (TSDROFStats statEntry : stats) {
            if (statEntry.getStatsType() == DataCategory.FLOWSTATS) {
                log.debug("Obtained FlowStats");
                metricList = TSDRStorageServiceUtil.getFlowMetricsFrom(statEntry);
            } else if (statEntry.getStatsType() == DataCategory.FLOWTABLESTATS) {
                log.debug("Obtained FlowTableStats");
                metricList = TSDRStorageServiceUtil.getFlowTableMetricsFrom(statEntry);
            }else if (statEntry.getStatsType() == DataCategory.FLOWGROUPSTATS){
                log.debug("Obtained FlowGroupStats");
                metricList = TSDRStorageServiceUtil.getGroupMetricsFrom(statEntry);
            }else if (statEntry.getStatsType() == DataCategory.PORTSTATS){
                log.debug("Obtained FlowGroupStats");
                metricList = TSDRStorageServiceUtil.getPortMetricsFrom(statEntry);
            }else if (statEntry.getStatsType() == DataCategory.QUEUESTATS){
                log.debug("Obtained QueueStats");
                metricList = TSDRStorageServiceUtil.getQueueMetricsFrom(statEntry);
            }
            if (metricList == null || metricList.size() == 0){
                continue;
            }
            StoreTSDRMetricRecordInput storeTSDRMetricsInput = new StoreTSDRMetricRecordInputBuilder()
               .setTSDRMetricRecord(metricList).build();
            result = storeTSDRMetricRecord(storeTSDRMetricsInput);
            log.debug("Exiting TSDRDataStorage.store()");
        }
        log.debug("Exiting TSDRStorageService.store()");
        return result;
    }


    @Override
    /**
     * Close DB connections in the persistence data store.
     */
    public void close() throws Exception {
        if ( TSDRPersistenceServiceFactory.getData_store_type() == DataStoreType.HBASE){
            TSDRPersistenceServiceFactory.getTSDRPersistenceDataStore().closeConnections();

        }
    }
}
