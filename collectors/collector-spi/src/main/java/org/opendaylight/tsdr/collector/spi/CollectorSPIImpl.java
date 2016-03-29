/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.collector.spi;

import com.google.common.util.concurrent.Futures;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.StoreTSDRLogRecordInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.TsdrLogDataService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.StoreTSDRMetricRecordInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.TsdrMetricDataService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRLogRecordInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRMetricRecordInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
/**
 * @author Sharon Aicler(saichler@gmail.com)
 * This class is being used as a stub to the persistence layer SPI. It purpose is to give a layer
 * where TSDR project can do statistics and throttle the inserted metrics.
 **/
public class CollectorSPIImpl implements TsdrCollectorSpiService{

    private TsdrMetricDataService metricDataService =null;
    private TsdrLogDataService logDataService =null;


    public CollectorSPIImpl(TsdrMetricDataService metricService,TsdrLogDataService logService){
        this.metricDataService = metricService;
        this.logDataService = logService;
    }

    @Override
    public Future<RpcResult<Void>> insertTSDRMetricRecord(InsertTSDRMetricRecordInput input) {
        StoreTSDRMetricRecordInputBuilder tsdrServiceInput = new StoreTSDRMetricRecordInputBuilder();
        List<TSDRMetricRecord> records = new ArrayList<TSDRMetricRecord>();
        for(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrmetricrecord.input.TSDRMetricRecord inputRec:input.getTSDRMetricRecord()){
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
        metricDataService.storeTSDRMetricRecord(tsdrServiceInput.build());
        return Futures.immediateFuture(RpcResultBuilder.<Void> success().build());
    }

    @Override
    public Future<RpcResult<Void>> insertTSDRLogRecord(InsertTSDRLogRecordInput input) {
        StoreTSDRLogRecordInputBuilder tsdrServiceInput = new StoreTSDRLogRecordInputBuilder();
        List<TSDRLogRecord> records = new ArrayList<TSDRLogRecord>();
        for(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecord inputRec:input.getTSDRLogRecord()){
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
        logDataService.storeTSDRLogRecord(tsdrServiceInput.build());
        return Futures.immediateFuture(RpcResultBuilder.<Void> success().build());
    }
}
