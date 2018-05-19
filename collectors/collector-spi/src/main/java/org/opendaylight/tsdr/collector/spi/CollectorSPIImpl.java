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
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.StoreTSDRLogRecordInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.TsdrLogDataService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecordBuilder;
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
        StoreTSDRMetricRecordInputBuilder tsdrServiceInput = new StoreTSDRMetricRecordInputBuilder();
        List<TSDRMetricRecord> records = new ArrayList<>();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi
                .rev150915.inserttsdrmetricrecord.input.TSDRMetricRecord inputRec : input.getTSDRMetricRecord()) {
            TSDRMetricRecordBuilder rec = new TSDRMetricRecordBuilder();
            rec.setMetricName(inputRec.getMetricName());
            rec.setMetricValue(inputRec.getMetricValue());
            rec.setNodeID(inputRec.getNodeID());
            rec.setRecordKeys(inputRec.getRecordKeys());
            rec.setTimeStamp(inputRec.getTimeStamp());
            rec.setTSDRDataCategory(inputRec.getTSDRDataCategory());
            records.add(rec.build());
        }
        tsdrServiceInput.setTSDRMetricRecord(records);

        return Futures.transform(metricDataService.storeTSDRMetricRecord(tsdrServiceInput.build()), result -> {
            return result.isSuccessful() ? RpcResultBuilder.success(new InsertTSDRMetricRecordOutputBuilder().build())
                    .build() : RpcResultBuilder.<InsertTSDRMetricRecordOutput>failed()
                    .withRpcErrors(result.getErrors()).build();
        }, MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<RpcResult<InsertTSDRLogRecordOutput>> insertTSDRLogRecord(InsertTSDRLogRecordInput input) {
        StoreTSDRLogRecordInputBuilder tsdrServiceInput = new StoreTSDRLogRecordInputBuilder();
        List<TSDRLogRecord> records = new ArrayList<>();
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi
                .rev150915.inserttsdrlogrecord.input.TSDRLogRecord inputRec:input.getTSDRLogRecord()) {
            TSDRLogRecordBuilder rec = new TSDRLogRecordBuilder();
            rec.setNodeID(inputRec.getNodeID());
            rec.setRecordAttributes(inputRec.getRecordAttributes());
            rec.setRecordFullText(inputRec.getRecordFullText());
            rec.setRecordKeys(inputRec.getRecordKeys());
            rec.setTimeStamp(inputRec.getTimeStamp());
            rec.setIndex(inputRec.getIndex());
            rec.setTSDRDataCategory(inputRec.getTSDRDataCategory());
            records.add(rec.build());
        }
        tsdrServiceInput.setTSDRLogRecord(records);

        return Futures.transform(logDataService.storeTSDRLogRecord(tsdrServiceInput.build()), result -> {
            return result.isSuccessful() ? RpcResultBuilder.success(new InsertTSDRLogRecordOutputBuilder().build())
                        .build() : RpcResultBuilder.<InsertTSDRLogRecordOutput>failed()
                        .withRpcErrors(result.getErrors()).build();
        }, MoreExecutors.directExecutor());
    }
}
