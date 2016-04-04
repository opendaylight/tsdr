/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.datastorage;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;
import com.google.common.util.concurrent.Futures;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.opendaylight.tsdr.datastorage.aggregate.AggregationFunction;
import org.opendaylight.tsdr.datastorage.aggregate.IntervalGenerator;
import org.opendaylight.tsdr.spi.persistence.TSDRBinaryPersistenceService;
import org.opendaylight.tsdr.spi.persistence.TSDRLogPersistenceService;
import org.opendaylight.tsdr.spi.persistence.TSDRMetricPersistenceService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.GetTSDRLogRecordsInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.GetTSDRLogRecordsOutput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.GetTSDRLogRecordsOutputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.StoreTSDRLogRecordInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.TsdrLogDataService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.gettsdrlogrecords.output.Logs;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.gettsdrlogrecords.output.LogsBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.AggregationType;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.GetTSDRAggregatedMetricsInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.GetTSDRAggregatedMetricsOutput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.GetTSDRAggregatedMetricsOutputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.GetTSDRMetricsInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.GetTSDRMetricsInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.GetTSDRMetricsOutput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.GetTSDRMetricsOutputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.StoreTSDRMetricRecordInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.TsdrMetricDataService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.gettsdraggregatedmetrics.output.AggregatedMetrics;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.gettsdraggregatedmetrics.output.AggregatedMetricsBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.gettsdrmetrics.output.Metrics;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.gettsdrmetrics.output.MetricsBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.PurgeAllTSDRRecordInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.PurgeTSDRRecordInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRService;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class TSDRStorageServiceImpl implements TSDRService,TsdrMetricDataService,TsdrLogDataService, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(TSDRStorageServiceImpl.class);

    private final ServiceLoader<AggregationFunction> aggregationFunctions;

    private TSDRMetricPersistenceService metricPersistenceService;

    private TSDRLogPersistenceService logPersistenceService;

    private TSDRBinaryPersistenceService binaryPersistenceService;

    public TSDRStorageServiceImpl(TSDRMetricPersistenceService metricService,TSDRLogPersistenceService logService){
     this.metricPersistenceService = metricService;
     this.logPersistenceService = logService;
     aggregationFunctions = ServiceLoader.load(AggregationFunction.class,this.getClass().getClassLoader());
    }

    public void setMetricPersistenceService(TSDRMetricPersistenceService metricService){
        this.metricPersistenceService = metricService;
    }

    public void setLogPersistenceService(TSDRLogPersistenceService logService) {
        this.logPersistenceService = logService;
    }

    public void setBinaryPersistenceService(TSDRBinaryPersistenceService binaryService){
        this.binaryPersistenceService = binaryService;
    }

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
        List<TSDRMetricRecord> tsdrMetricRecordList = new ArrayList<TSDRMetricRecord>(input.getTSDRMetricRecord().size());
        tsdrMetricRecordList.addAll(input.getTSDRMetricRecord());
        if(this.metricPersistenceService != null) {
            metricPersistenceService.storeMetric(tsdrMetricRecordList);
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

         if(this.metricPersistenceService != null) {
             this.metricPersistenceService.purge(category, timestamp);
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

         if(this.metricPersistenceService != null) {
             this.metricPersistenceService.purge(timestamp);
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
        if(this.metricPersistenceService!=null){
            //add close here
        }
    }

    @Override
    public Future<RpcResult<GetTSDRMetricsOutput>> getTSDRMetrics(GetTSDRMetricsInput input) {
        List<TSDRMetricRecord> result = this.metricPersistenceService.getTSDRMetricRecords(input.getTSDRDataCategory(), input.getStartTime(), input.getEndTime());
        return buildResult(result);
    }

    private Future<RpcResult<GetTSDRMetricsOutput>> buildResult(List<TSDRMetricRecord> result){

        GetTSDRMetricsOutputBuilder output = new GetTSDRMetricsOutputBuilder();

        if(this.metricPersistenceService==null){
            RpcResultBuilder<GetTSDRMetricsOutput> builder = RpcResultBuilder.failed();
            return builder.buildFuture();
        }

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

        if(this.metricPersistenceService==null){
            RpcResultBuilder<GetTSDRAggregatedMetricsOutput> builder = RpcResultBuilder.failed();
            return builder.buildFuture();
        }

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

        //Fix for bug 5655 - Do not aggregate when # of points is less than requested
        long numberOfPoints = (input.getEndTime()-input.getStartTime())/input.getInterval();
        try {
            //In case of a MEAN aggregation and the number of requested points is larger than what is, just return the original
            //result.
            if(input.getAggregation() == AggregationType.MEAN &&
                    result.get().getResult().getMetrics().size()<=numberOfPoints){
                final List<AggregatedMetrics> aggregatedMetrics = Lists.newLinkedList();
                for(Metrics m:result.get().getResult().getMetrics()){
                    // Aggregate the metrics in the interval
                    aggregatedMetrics.add(new AggregatedMetricsBuilder()
                            .setTimeStamp(m.getTimeStamp())
                            .setMetricValue(m.getMetricValue()).build());
                }
                // We're done
                final GetTSDRAggregatedMetricsOutputBuilder outputBuilder = new GetTSDRAggregatedMetricsOutputBuilder()
                        .setAggregatedMetrics(aggregatedMetrics);
                return RpcResultBuilder.success(outputBuilder).buildFuture();

            }
        } catch (InterruptedException | ExecutionException e) {
            RpcResultBuilder builder = RpcResultBuilder.failed();
            builder.withError(ErrorType.APPLICATION,"Failed to extract data for aggregation");
            return builder.buildFuture();
        }

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

        if(this.logPersistenceService==null){
            RpcResultBuilder<GetTSDRLogRecordsOutput> builder = RpcResultBuilder.failed();
            return builder.buildFuture();
        }

        List<TSDRLogRecord> result = this.logPersistenceService.getTSDRLogRecords(input.getTSDRDataCategory(), input.getStartTime(), input.getEndTime());
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
        List<TSDRLogRecord> tsdrLogRecordList = new ArrayList<TSDRLogRecord>(input.getTSDRLogRecord().size());
        tsdrLogRecordList.addAll(input.getTSDRLogRecord());
        if(this.logPersistenceService != null) {
            this.logPersistenceService.storeLog(tsdrLogRecordList);
        }else{
            log.warn("storeTSDRMetricRecord: cannot store the metric -- persistence service is found to be null");
        }
        log.debug("Exiting TSDRStorageService.storeTSDRMetrics()");
        return Futures.immediateFuture(RpcResultBuilder.<Void> success().build());
    }
}
