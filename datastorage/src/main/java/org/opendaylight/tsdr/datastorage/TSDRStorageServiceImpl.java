/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.datastorage;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.Future;

import org.opendaylight.tsdr.datastorage.aggregate.IntervalGenerator;
import org.opendaylight.tsdr.datastorage.aggregate.AggregationFunction;
import org.opendaylight.tsdr.datastorage.persistence.TSDRPersistenceServiceFactory;
import org.opendaylight.tsdr.spi.model.TSDRConstants;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetTSDRLogRecordsInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetTSDRLogRecordsOutput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetTSDRLogRecordsOutputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetTSDRAggregatedMetricsInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetTSDRAggregatedMetricsOutput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetTSDRAggregatedMetricsOutputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetTSDRMetricsInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetTSDRMetricsInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetTSDRMetricsOutput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetTSDRMetricsOutputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.PurgeAllTSDRRecordInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.PurgeTSDRRecordInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.StoreTSDRLogRecordInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.StoreTSDRMetricRecordInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.gettsdraggregatedmetrics.output.AggregatedMetrics;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.gettsdraggregatedmetrics.output.AggregatedMetricsBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.gettsdrlogrecords.output.Logs;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.gettsdrlogrecords.output.LogsBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.gettsdrmetrics.output.Metrics;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.gettsdrmetrics.output.MetricsBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;
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

     private static ServiceLoader<AggregationFunction> aggregationFunctions = ServiceLoader
             .load(AggregationFunction.class);

     /**
     * stores TSDRMetricRecord.
     *
     */
    @Override
    public Future<RpcResult<java.lang.Void>> storeTSDRMetricRecord(StoreTSDRMetricRecordInput input) {
        log.debug("Entering TSDRStorageService.storeTSDRMetrics()");
        if ( input == null || input.getTSDRMetricRecord() == null){
            log.error("Input of storeTSDRMetrics is null");
            return Futures.immediateFuture(RpcResultBuilder.<Void> success()
                    .build());
        }
        List<TSDRRecord> tsdrMetricRecordList = new ArrayList<TSDRRecord>(input.getTSDRMetricRecord().size());
        tsdrMetricRecordList.addAll(input.getTSDRMetricRecord());
        if(TSDRPersistenceServiceFactory.getTSDRPersistenceDataStore() != null) {
            TSDRPersistenceServiceFactory.getTSDRPersistenceDataStore().store(tsdrMetricRecordList);
        }else{
            log.warn("storeTSDRMetricRecord: cannot store the metric -- persistence service is found to be null");
        }
        log.debug("Exiting TSDRStorageService.storeTSDRMetrics()");
        return Futures.immediateFuture(RpcResultBuilder.<Void> success().build());
    }

    /**
     * purges TSDRMetricRecord.
     *
     */
    @Override
    public Future<RpcResult<java.lang.Void>> purgeTSDRRecord(PurgeTSDRRecordInput input){
         log.info("Entering TSDRStorageService.purgeTSDRRecord()");
         if ( input == null || input.getRetentionTime() == null
             || input.getRetentionTime() == 0
             || input.getTSDRDataCategory() == null){
             /*
              * The data category and retention_time of this API cannot be null.
              *
              */
             log.error("Input of  purgeTSDRRecord invalid");
             return Futures.immediateFuture(RpcResultBuilder.<Void> success()
                     .build());
         }
         DataCategory category = input.getTSDRDataCategory();
         Long timestamp = input.getRetentionTime();

         if(TSDRPersistenceServiceFactory.getTSDRPersistenceDataStore() != null) {
             TSDRPersistenceServiceFactory.getTSDRPersistenceDataStore().purgeTSDRRecords(category, timestamp);
         }else{
             log.warn("purgeTSDRRecord -- persistence service is found to be null");
         }
         log.info("Exiting TSDRStorageService.purgeTSDRRecord()");
         return Futures.immediateFuture(RpcResultBuilder.<Void> success()
             .build());
    }
    /**
     * purges TSDRMetricRecord.
     *
     */
    @Override
    public Future<RpcResult<java.lang.Void>> purgeAllTSDRRecord(PurgeAllTSDRRecordInput input){
         log.info("Entering TSDRStorageService.purgeAllTSDRRecord()");
         if ( input == null || input.getRetentionTime() == null || input.getRetentionTime() == 0){
             /*
              * The retention time cannot be null.
              *
              */
             log.error("Input of purgeAllTSDRRecord is invalid");
             return Futures.immediateFuture(RpcResultBuilder.<Void> success()
                     .build());
         }

         Long timestamp = input.getRetentionTime();

         if(TSDRPersistenceServiceFactory.getTSDRPersistenceDataStore() != null) {
             TSDRPersistenceServiceFactory.getTSDRPersistenceDataStore().purgeAllTSDRRecords(timestamp);
         }else{
             log.warn("purgeAllTSDRRecord -- persistence service is found to be null");
         }
         log.info("Exiting TSDRStorageService.purgeAllTSDRRecord()");
         return Futures.immediateFuture(RpcResultBuilder.<Void> success()
             .build());
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
    public Future<RpcResult<GetTSDRMetricsOutput>> getTSDRMetrics(GetTSDRMetricsInput input) {
        List<TSDRMetricRecord> result = TSDRPersistenceServiceFactory.getTSDRPersistenceDataStore().getTSDRMetricRecords(input.getTSDRDataCategory(), input.getStartTime(), input.getEndTime());
        GetTSDRMetricsOutputBuilder output = new GetTSDRMetricsOutputBuilder();
        List<Metrics> metrics = new LinkedList<Metrics>();
        for(TSDRMetricRecord m:result){
            MetricsBuilder b = new MetricsBuilder();
            b.setMetricName(m.getMetricName());
            b.setMetricValue(m.getMetricValue());
            b.setNodeID(m.getNodeID());
            b.setRecordKeys(m.getRecordKeys());
            b.setTimeStamp(m.getTimeStamp());
            b.setTSDRDataCategory(m.getTSDRDataCategory());
            metrics.add(b.build());
        }
        output.setMetrics(metrics);
        RpcResultBuilder<GetTSDRMetricsOutput> builder = RpcResultBuilder.success(output);
        return builder.buildFuture();
    }

    @Override
    public Future<RpcResult<GetTSDRAggregatedMetricsOutput>> getTSDRAggregatedMetrics(
            final GetTSDRAggregatedMetricsInput input) {

        // Locate the appropriate aggregation function implementation
        final AggregationFunction aggregationFunction = Iterators.find(
                aggregationFunctions.iterator(), new Predicate<AggregationFunction>() {
            @Override
            public boolean apply(AggregationFunction candidate) {
                return candidate.getType().equals(input.getAggregation());
            }
        }, null);
        if (aggregationFunction == null) {
            return RpcResultBuilder.<GetTSDRAggregatedMetricsOutput>failed()
                    .withError(ErrorType.APPLICATION,
                            String.format("No aggregation function implementation was found for '%s'.",
                                    input.getAggregation()))
                    .buildFuture();
        }

        // Gather the metrics for the given time span
        final GetTSDRMetricsInput metricsInput = new GetTSDRMetricsInputBuilder()
                .setTSDRDataCategory(input.getTSDRDataCategory())
                .setStartTime(input.getStartTime())
                .setEndTime(input.getEndTime()).build();
        final Future<RpcResult<GetTSDRMetricsOutput>> result = getTSDRMetrics(metricsInput);

        // Aggregate the results
        return Futures.lazyTransform(result, new Function<RpcResult<GetTSDRMetricsOutput>,RpcResult<GetTSDRAggregatedMetricsOutput>>() {
            @Override
            public RpcResult<GetTSDRAggregatedMetricsOutput> apply(RpcResult<GetTSDRMetricsOutput> metricsOutput) {
                final List<AggregatedMetrics> aggregatedMetrics = Lists.newLinkedList();
                final PeekingIterator<Metrics> metricIterator = Iterators.peekingIterator(metricsOutput.getResult().getMetrics().iterator());
                // Generate and iterate over all the intervals in the given range
                for (Long intervalStartInclusive : new IntervalGenerator(input.getStartTime(), input.getEndTime(), input.getInterval())) {
                    final Long intervalEndExclusive = intervalStartInclusive + input.getInterval();

                    // Gather the list of metrics that fall within the current interval
                    // We make the assumption that the list of metrics is already sorted by time-stamp
                    final List<Metrics> metricsInInterval = Lists.newLinkedList();
                    while (metricIterator.hasNext()) {
                        if (metricIterator.peek().getTimeStamp() >= intervalEndExclusive) {
                            break;
                        }
                        metricsInInterval.add(metricIterator.next());
                    }

                    // Aggregate the metrics in the interval
                    aggregatedMetrics.add(new AggregatedMetricsBuilder()
                            .setTimeStamp(intervalStartInclusive)
                            .setMetricValue(aggregationFunction.aggregate(metricsInInterval)).build());
                }

                // We're done
                final GetTSDRAggregatedMetricsOutput output = new GetTSDRAggregatedMetricsOutputBuilder()
                        .setAggregatedMetrics(aggregatedMetrics).build();
                return RpcResultBuilder.success(output).build();
            }
        });
    }

    @Override
    public Future<RpcResult<GetTSDRLogRecordsOutput>> getTSDRLogRecords(GetTSDRLogRecordsInput input) {
        List<TSDRLogRecord> result = TSDRPersistenceServiceFactory.getTSDRPersistenceDataStore().getTSDRLogRecords(input.getTSDRDataCategory(), input.getStartTime(), input.getEndTime());
        GetTSDRLogRecordsOutputBuilder output = new GetTSDRLogRecordsOutputBuilder();
        List<Logs> logs = new LinkedList<Logs>();
        for(TSDRLogRecord l:result){
            LogsBuilder b = new LogsBuilder();
            b.setTSDRDataCategory(l.getTSDRDataCategory());
            b.setTimeStamp(l.getTimeStamp());
            b.setRecordKeys(l.getRecordKeys());
            b.setNodeID(l.getNodeID());
            b.setIndex(l.getIndex());
            b.setRecordAttributes(l.getRecordAttributes());
            b.setRecordFullText(l.getRecordFullText());
            logs.add(b.build());
        }
        output.setLogs(logs);
        RpcResultBuilder<GetTSDRLogRecordsOutput> builder = RpcResultBuilder.success(output);
        return builder.buildFuture();
    }

    public Future<RpcResult<Void>> storeTSDRLogRecord(StoreTSDRLogRecordInput input) {
        log.debug("Entering TSDRStorageService.storeTSDRLog()");
        if ( input == null || input.getTSDRLogRecord() == null){
            log.error("Input of storeTSDRLog is null");
            return Futures.immediateFuture(RpcResultBuilder.<Void> success().build());
        }
        List<TSDRRecord> tsdrLogRecordList = new ArrayList<TSDRRecord>(input.getTSDRLogRecord().size());
        tsdrLogRecordList.addAll(input.getTSDRLogRecord());
        if(TSDRPersistenceServiceFactory.getTSDRPersistenceDataStore() != null) {
            TSDRPersistenceServiceFactory.getTSDRPersistenceDataStore().store(tsdrLogRecordList);
        }else{
            log.warn("storeTSDRMetricRecord: cannot store the metric -- persistence service is found to be null");
        }
        log.debug("Exiting TSDRStorageService.storeTSDRMetrics()");
        return Futures.immediateFuture(RpcResultBuilder.<Void> success().build());
    }
}
