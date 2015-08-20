/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.datastorage;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

import org.opendaylight.tsdr.datastorage.persistence.TSDRPersistenceServiceFactory;
import org.opendaylight.tsdr.spi.model.TSDRConstants;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetMetricInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetMetricOutput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetMetricOutputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.PurgeTSDRMetricRecordInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.StoreOFStatsInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.StoreTSDRMetricRecordInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.StoreTSDRMetricRecordInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.getmetric.output.Metrics;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.getmetric.output.MetricsBuilder;
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
        if(TSDRPersistenceServiceFactory.getTSDRPersistenceDataStore() != null) {
            TSDRPersistenceServiceFactory.getTSDRPersistenceDataStore().store(
                tsdrMetricRecordList);
        }else{
            log.warn("storeTSDRMetricRecord: cannot store the metric -- persistence service is found to be null");
        }
        log.debug("Exiting TSDRStorageService.storeTSDRMetrics()");
        return Futures.immediateFuture(RpcResultBuilder.<Void> success()
            .build());
    }

    /**
     * purges TSDRMetricRecord.
     *
     */
    @Override
    public Future<RpcResult<java.lang.Void>> purgeTSDRMetricRecord(PurgeTSDRMetricRecordInput input){
        return null;
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

            TSDRPersistenceServiceFactory.getTSDRPersistenceDataStore().stop(
                TSDRConstants.STOP_PERSISTENCE_SERVICE_TIMEOUT);


    }

    @Override
    public Future<RpcResult<GetMetricOutput>> getMetric(GetMetricInput input) {
        List<?> result = TSDRPersistenceServiceFactory.getTSDRPersistenceDataStore().getMetrics(input.getName(), new Date(input.getFrom()), new Date(input.getUntil()));
        GetMetricOutputBuilder output = new GetMetricOutputBuilder();
        List<Metrics> metrics = new LinkedList<Metrics>();
        Method mColumns = null;
        List<?> columns = null;
        Method mTime = null;
        Method mValue = null;
        boolean isCassandra = false;
        boolean isH2 = false;
        boolean isHBase = false;
        for(Object o:result){
            if(mTime==null){
                if(isCassandra || o.getClass().getName().indexOf("Cassandra")!=-1){
                    isCassandra = true;
                    try {
                        mTime = o.getClass().getMethod("getTime",(Class<?>[]) null);
                        mValue = o.getClass().getMethod("getValue", (Class<?>[]) null);
                    } catch (NoSuchMethodException | SecurityException e) {
                        log.error("Can't find time method",e);
                    }
                }else
                if(isH2 || o.getClass().getName().equals("org.opendaylight.tsdr.entity.Metric")){
                    isH2 = true;
                    try {
                        mTime = o.getClass().getMethod("getMetricTimeStamp",(Class<?>[]) null);
                        mValue = o.getClass().getMethod("getMetricValue", (Class<?>[]) null);
                    } catch (NoSuchMethodException | SecurityException e) {
                        log.error("Can't find time method",e);
                    }
                }else
                if(isHBase || o.getClass().getName().equals("org.opendaylight.tsdr.persistence.hbase.HBaseEntity")){
                    isHBase = true;
                    try {
                        mColumns = o.getClass().getMethod("getColumns",(Class<?>[]) null);
                        columns = (List<?>)mColumns.invoke(o, (Object[])null);
                        mTime = columns.get(0).getClass().getMethod("getTimeStamp",(Class<?>[]) null);
                        mValue = columns.get(0).getClass().getMethod("getValue",(Class<?>[]) null);
                    } catch (NoSuchMethodException | SecurityException | IllegalAccessException
                            | IllegalArgumentException
                            | InvocationTargetException e) {
                        log.error("Can't find time method",e);
                    }
                }
            }
            try {
                MetricsBuilder mb = new MetricsBuilder();
                if(columns==null){
                    Date time = (Date) mTime.invoke(o, (Object[]) null);
                    Double value = (Double)mValue.invoke(o, (Object[])null);
                    mb.setTime(time.getTime());
                    mb.setValue(new BigDecimal(value));
                    metrics.add(mb.build());
                }else{
                    for(Object col:columns){
                        Date time = (Date) mTime.invoke(col, (Object[]) null);
                        Double value = (Double)mValue.invoke(col, (Object[]) null);
                        mb.setTime(time.getTime());
                        mb.setValue(new BigDecimal(value));
                        metrics.add(mb.build());
                    }
                }
            } catch (IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e) {
                log.error("Failed to collect data from metric",e);
            }
        }
        output.setMetrics(metrics);
        RpcResultBuilder<GetMetricOutput> builder = RpcResultBuilder.success(output);
        return builder.buildFuture();
    }
}
