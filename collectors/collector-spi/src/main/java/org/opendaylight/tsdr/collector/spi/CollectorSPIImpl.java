/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.collector.spi;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.List;
import java.util.stream.Collectors;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.StoreTSDRLogRecordInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.StoreTSDRLogRecordInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.TsdrLogDataService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.StoreTSDRMetricRecordInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.StoreTSDRMetricRecordInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.TsdrMetricDataService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRLogRecordInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRLogRecordOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRLogRecordOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRMetricRecordInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRMetricRecordOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRMetricRecordOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is being used as a stub to the persistence layer SPI. It purpose is to give a layer
 * where TSDR project can do statistics and throttle the inserted metrics.
 *
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class CollectorSPIImpl implements TsdrCollectorSpiService {
    private static final Logger LOG = LoggerFactory.getLogger(CollectorSPIImpl.class);

    private final TsdrMetricDataService metricDataService;
    private final TsdrLogDataService logDataService;


    public CollectorSPIImpl(TsdrMetricDataService metricService,TsdrLogDataService logService) {
        this.metricDataService = metricService;
        this.logDataService = logService;
    }

    @Override
    public ListenableFuture<RpcResult<InsertTSDRMetricRecordOutput>> insertTSDRMetricRecord(
            InsertTSDRMetricRecordInput input) {
        List<TSDRMetricRecord> records = input.getTSDRMetricRecord().stream().map(inputRec ->
            new TSDRMetricRecordBuilder().setMetricName(inputRec.getMetricName())
                .setMetricValue(inputRec.getMetricValue())
                .setNodeID(inputRec.getNodeID())
                .setRecordKeys(inputRec.getRecordKeys())
                .setTimeStamp(inputRec.getTimeStamp())
                .setTSDRDataCategory(inputRec.getTSDRDataCategory()).build()).collect(Collectors.toList());

        StoreTSDRMetricRecordInput tsdrServiceInput =
                new StoreTSDRMetricRecordInputBuilder().setTSDRMetricRecord(records).build();

        return Futures.transform(metricDataService.storeTSDRMetricRecord(tsdrServiceInput), result -> {
            return result.isSuccessful() ? RpcResultBuilder.success(new InsertTSDRMetricRecordOutputBuilder().build())
                    .build() : RpcResultBuilder.<InsertTSDRMetricRecordOutput>failed()
                    .withRpcErrors(result.getErrors()).build();
        }, MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<RpcResult<InsertTSDRLogRecordOutput>> insertTSDRLogRecord(InsertTSDRLogRecordInput input) {
        List<TSDRLogRecord> records = input.getTSDRLogRecord().stream().map(inputRec ->
            new TSDRLogRecordBuilder().setNodeID(inputRec.getNodeID())
                .setRecordAttributes(inputRec.getRecordAttributes())
                .setRecordFullText(inputRec.getRecordFullText())
                .setRecordKeys(inputRec.getRecordKeys())
                .setTimeStamp(inputRec.getTimeStamp())
                .setIndex(inputRec.getIndex())
                .setTSDRDataCategory(inputRec.getTSDRDataCategory()).build()).collect(Collectors.toList());

        StoreTSDRLogRecordInput tsdrServiceInput =
                new StoreTSDRLogRecordInputBuilder().setTSDRLogRecord(records).build();

        return Futures.transform(logDataService.storeTSDRLogRecord(tsdrServiceInput), result -> {
            return result.isSuccessful() ? RpcResultBuilder.success(new InsertTSDRLogRecordOutputBuilder().build())
                        .build() : RpcResultBuilder.<InsertTSDRLogRecordOutput>failed()
                        .withRpcErrors(result.getErrors()).build();
        }, MoreExecutors.directExecutor());
    }
}
