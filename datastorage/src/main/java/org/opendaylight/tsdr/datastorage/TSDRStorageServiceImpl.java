/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.datastorage;

import com.google.common.base.Optional;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.ExecutionException;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.tsdr.datastorage.aggregate.AggregationFunction;
import org.opendaylight.tsdr.datastorage.aggregate.IntervalGenerator;
import org.opendaylight.tsdr.spi.persistence.TSDRBinaryPersistenceService;
import org.opendaylight.tsdr.spi.persistence.TSDRLogPersistenceService;
import org.opendaylight.tsdr.spi.persistence.TSDRMetricPersistenceService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.GetTSDRLogRecordsInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.GetTSDRLogRecordsOutput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.GetTSDRLogRecordsOutputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.StoreTSDRLogRecordInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.StoreTSDRLogRecordOutput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.StoreTSDRLogRecordOutputBuilder;
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
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.StoreTSDRMetricRecordOutput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.StoreTSDRMetricRecordOutputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.TsdrMetricDataService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.gettsdraggregatedmetrics.output.AggregatedMetrics;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.gettsdraggregatedmetrics.output.AggregatedMetricsBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.gettsdrmetrics.output.Metrics;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.gettsdrmetrics.output.MetricsBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.PurgeAllTSDRRecordInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.PurgeAllTSDRRecordOutput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.PurgeAllTSDRRecordOutputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.PurgeTSDRRecordInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.PurgeTSDRRecordOutput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.PurgeTSDRRecordOutputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRService;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.osgi.service.blueprint.container.ServiceUnavailableException;
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
 */
@Singleton
public class TSDRStorageServiceImpl implements TSDRService,TsdrMetricDataService,TsdrLogDataService, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(TSDRStorageServiceImpl.class);

    private final ServiceLoader<AggregationFunction> aggregationFunctions;

    private final TSDRMetricPersistenceService metricPersistenceService;

    private final TSDRLogPersistenceService logPersistenceService;

    private final TSDRBinaryPersistenceService binaryPersistenceService;

    @Inject
    public TSDRStorageServiceImpl(TSDRMetricPersistenceService metricService, TSDRLogPersistenceService logService,
            TSDRBinaryPersistenceService binaryPersistenceService) {
        this.metricPersistenceService = metricService;
        this.logPersistenceService = logService;
        this.binaryPersistenceService = binaryPersistenceService;
        aggregationFunctions = ServiceLoader.load(AggregationFunction.class, this.getClass().getClassLoader());
    }

    /**
     * stores TSDRMetricRecord.
     *
     */
    @Override
    public ListenableFuture<RpcResult<StoreTSDRMetricRecordOutput>> storeTSDRMetricRecord(
            StoreTSDRMetricRecordInput input) {
        LOG.debug("Entering TSDRStorageService.storeTSDRMetrics()");
        if (input == null || input.getTSDRMetricRecord() == null) {
            LOG.debug("Input of storeTSDRMetrics is null");
            return RpcResultBuilder.<StoreTSDRMetricRecordOutput>failed().withError(ErrorType.PROTOCOL,
                    "Input of storeTSDRMetrics is null").buildFuture();
        }
        List<TSDRMetricRecord> tsdrMetricRecordList = new ArrayList<>(input.getTSDRMetricRecord().size());
        tsdrMetricRecordList.addAll(input.getTSDRMetricRecord());

        try {
            metricPersistenceService.storeMetric(tsdrMetricRecordList);
        } catch (ServiceUnavailableException e) {
            LOG.debug("storeTSDRMetricRecord: cannot store the metric -- persistence service is not available");
            return RpcResultBuilder.<StoreTSDRMetricRecordOutput>failed().withError(ErrorType.APPLICATION,
                    "The persistence service is not available", e).buildFuture();
        }

        LOG.debug("Exiting TSDRStorageService.storeTSDRMetrics()");
        return RpcResultBuilder.success(new StoreTSDRMetricRecordOutputBuilder().build()).buildFuture();
    }

    /**
     * purges TSDRMetricRecord.
     *
     */
    @Override
    public ListenableFuture<RpcResult<PurgeTSDRRecordOutput>> purgeTSDRRecord(PurgeTSDRRecordInput input) {
        LOG.debug("Entering TSDRStorageService.purgeTSDRRecord()");
        if (input == null || input.getRetentionTime() == null || input.getRetentionTime() == 0
                || input.getTSDRDataCategory() == null) {
             // The data category and retention_time of this API cannot be null.
            LOG.debug("Input of  purgeTSDRRecord invalid");
            return RpcResultBuilder.<PurgeTSDRRecordOutput>failed().withError(ErrorType.PROTOCOL,
                    "Input for purgeTSDRRecord invalid").buildFuture();
        }
        DataCategory category = input.getTSDRDataCategory();
        Long timestamp = input.getRetentionTime();

        try {
            this.metricPersistenceService.purge(category, timestamp);
        } catch (ServiceUnavailableException e) {
            LOG.debug("purgeTSDRRecord -- persistence service is not available");
            return RpcResultBuilder.<PurgeTSDRRecordOutput>failed().withError(ErrorType.APPLICATION,
                    "The persistence service is not available", e).buildFuture();
        }

        LOG.debug("Exiting TSDRStorageService.purgeTSDRRecord()");
        return RpcResultBuilder.success(new PurgeTSDRRecordOutputBuilder().build()).buildFuture();
    }

    /**
     * purges TSDRMetricRecord.
     *
     */
    @Override
    public ListenableFuture<RpcResult<PurgeAllTSDRRecordOutput>> purgeAllTSDRRecord(PurgeAllTSDRRecordInput input) {
        LOG.debug("Entering TSDRStorageService.purgeAllTSDRRecord()");
        if (input == null || input.getRetentionTime() == null || input.getRetentionTime() == 0) {
             // The retention time cannot be null.
            LOG.debug("Input of purgeAllTSDRRecord is invalid");
            return RpcResultBuilder.<PurgeAllTSDRRecordOutput>failed().withError(ErrorType.PROTOCOL,
                    "Input for purgeAllTSDRRecord is invalid").buildFuture();
        }

        Long timestamp = input.getRetentionTime();

        try {
            this.metricPersistenceService.purge(timestamp);
        } catch (ServiceUnavailableException e) {
            LOG.debug("purgeAllTSDRRecord -- persistence service is not available");
            return RpcResultBuilder.<PurgeAllTSDRRecordOutput>failed().withError(ErrorType.APPLICATION,
                    "The persistence service is not available", e).buildFuture();
        }

        LOG.debug("Exiting TSDRStorageService.purgeAllTSDRRecord()");
        return RpcResultBuilder.success(new PurgeAllTSDRRecordOutputBuilder().build()).buildFuture();
    }

    @Override
    @PreDestroy
    public void close() {
    }

    @Override
    public ListenableFuture<RpcResult<GetTSDRMetricsOutput>> getTSDRMetrics(GetTSDRMetricsInput input) {
        try {
            List<TSDRMetricRecord> result = this.metricPersistenceService.getTSDRMetricRecords(
                    input.getTSDRDataCategory(), input.getStartTime(), input.getEndTime());
            return buildResult(result);
        } catch (ServiceUnavailableException e) {
            return RpcResultBuilder.<GetTSDRMetricsOutput>failed().withError(ErrorType.APPLICATION,
                    "The persistence service is not available", e).buildFuture();
        }
    }

    private ListenableFuture<RpcResult<GetTSDRMetricsOutput>> buildResult(List<TSDRMetricRecord> result) {

        GetTSDRMetricsOutputBuilder output = new GetTSDRMetricsOutputBuilder();

        List<Metrics> metrics = new LinkedList<>();
        for (TSDRMetricRecord m:result) {
            MetricsBuilder builder = new MetricsBuilder();
            builder.setMetricName(m.getMetricName());
            builder.setMetricValue(m.getMetricValue());
            builder.setNodeID(m.getNodeID());
            builder.setRecordKeys(m.getRecordKeys());
            builder.setTimeStamp(m.getTimeStamp());
            builder.setTSDRDataCategory(m.getTSDRDataCategory());
            metrics.add(builder.build());
        }
        output.setMetrics(metrics);
        RpcResultBuilder<GetTSDRMetricsOutput> builder = RpcResultBuilder.success(output);
        return builder.buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<GetTSDRAggregatedMetricsOutput>> getTSDRAggregatedMetrics(
            final GetTSDRAggregatedMetricsInput input) {

        // Locate the appropriate aggregation function implementation
        Optional<AggregationFunction> aggregationFunction = Iterators.tryFind(
                aggregationFunctions.iterator(), candidate -> candidate.getType().equals(input.getAggregation()));
        if (!aggregationFunction.isPresent()) {
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
        final ListenableFuture<RpcResult<GetTSDRMetricsOutput>> result = getTSDRMetrics(metricsInput);

        //Fix for bug 5655 - Do not aggregate when # of points is less than requested
        long numberOfPoints = (input.getEndTime() - input.getStartTime()) / input.getInterval();
        try {
            if (result.isDone() && !result.get().isSuccessful()) {
                return RpcResultBuilder.<GetTSDRAggregatedMetricsOutput>failed().withRpcErrors(result.get()
                        .getErrors()).buildFuture();
            }

            // In case of a MEAN aggregation and the number of requested points is larger than what is, just return
            // the original result.
            if (input.getAggregation() == AggregationType.MEAN
                    && result.get().getResult().getMetrics().size() <= numberOfPoints) {
                final List<AggregatedMetrics> aggregatedMetrics = Lists.newLinkedList();
                for (Metrics m : result.get().getResult().getMetrics()) {
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
            return RpcResultBuilder.<GetTSDRAggregatedMetricsOutput>failed().withError(
                    ErrorType.APPLICATION,"Failed to extract data for aggregation").buildFuture();
        }

        // Aggregate the results
        return Futures.transform(result, metricsOutput -> {
            final List<AggregatedMetrics> aggregatedMetrics = Lists.newArrayList();
            final PeekingIterator<Metrics> metricIterator = Iterators
                    .peekingIterator(metricsOutput.getResult().getMetrics().iterator());
            // Generate and iterate over all the intervals in the given range
            for (Long intervalStartInclusive : new IntervalGenerator(input.getStartTime(), input.getEndTime(),
                    input.getInterval())) {
                final Long intervalEndExclusive = intervalStartInclusive + input.getInterval();

                // Gather the list of metrics that fall within the current interval
                // We make the assumption that the list of metrics is already sorted by time-stamp
                final List<Metrics> metricsInInterval = Lists.newArrayList();
                while (metricIterator.hasNext()) {
                    if (metricIterator.peek().getTimeStamp() >= intervalEndExclusive) {
                        break;
                    }
                    metricsInInterval.add(metricIterator.next());
                }

                // Aggregate the metrics in the interval
                aggregatedMetrics.add(new AggregatedMetricsBuilder()
                        .setTimeStamp(intervalStartInclusive)
                        .setMetricValue(aggregationFunction.get().aggregate(metricsInInterval)).build());
            }

            // We're done
            final GetTSDRAggregatedMetricsOutput output = new GetTSDRAggregatedMetricsOutputBuilder()
                    .setAggregatedMetrics(aggregatedMetrics).build();
            return RpcResultBuilder.success(output).build();
        }, MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<RpcResult<GetTSDRLogRecordsOutput>> getTSDRLogRecords(GetTSDRLogRecordsInput input) {
        try {
            List<TSDRLogRecord> result = this.logPersistenceService.getTSDRLogRecords(input.getTSDRDataCategory(),
                    input.getStartTime(), input.getEndTime());
            GetTSDRLogRecordsOutputBuilder output = new GetTSDRLogRecordsOutputBuilder();
            List<Logs> logs = new LinkedList<>();
            for (TSDRLogRecord log : result) {
                LogsBuilder builder = new LogsBuilder();
                builder.setTSDRDataCategory(log.getTSDRDataCategory());
                builder.setTimeStamp(log.getTimeStamp());
                builder.setRecordKeys(log.getRecordKeys());
                builder.setNodeID(log.getNodeID());
                builder.setIndex(log.getIndex());
                builder.setRecordAttributes(log.getRecordAttributes());
                builder.setRecordFullText(log.getRecordFullText());
                logs.add(builder.build());
            }
            output.setLogs(logs);
            RpcResultBuilder<GetTSDRLogRecordsOutput> builder = RpcResultBuilder.success(output);
            return builder.buildFuture();
        } catch (ServiceUnavailableException e) {
            return RpcResultBuilder.<GetTSDRLogRecordsOutput>failed().withError(ErrorType.APPLICATION,
                    "TSDRLogPersistenceService is not available").buildFuture();
        }
    }

    @Override
    public ListenableFuture<RpcResult<StoreTSDRLogRecordOutput>> storeTSDRLogRecord(StoreTSDRLogRecordInput input) {
        LOG.debug("Entering TSDRStorageService.storeTSDRLog()");
        if (input == null || input.getTSDRLogRecord() == null) {
            LOG.debug("Input of storeTSDRLog is null");
            return RpcResultBuilder.<StoreTSDRLogRecordOutput>failed().withError(ErrorType.PROTOCOL,
                    "Input for storeTSDRLog is null").buildFuture();
        }
        List<TSDRLogRecord> tsdrLogRecordList = new ArrayList<>(input.getTSDRLogRecord().size());
        tsdrLogRecordList.addAll(input.getTSDRLogRecord());

        try {
            this.logPersistenceService.storeLog(tsdrLogRecordList);
        } catch (ServiceUnavailableException e) {
            LOG.debug("storeTSDRLogRecord: cannot store the record -- persistence service is not available");
        }

        LOG.debug("Exiting TSDRStorageService.storeTSDRMetrics()");
        return RpcResultBuilder.success(new StoreTSDRLogRecordOutputBuilder().build()).buildFuture();
    }
}
