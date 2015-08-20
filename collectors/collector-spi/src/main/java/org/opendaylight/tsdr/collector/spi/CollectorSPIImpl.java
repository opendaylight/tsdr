/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.collector.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.StoreTSDRMetricRecordInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRMetricRecordInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

import com.google.common.util.concurrent.Futures;
/**
 * @author Sharon Aicler(saichler@gmail.com)
 * This class is being used as a stub to the persistence layer SPI. It purpose is to give a layer
 * where TSDR project can do statistics and throttle the inserted metrics.
 **/
public class CollectorSPIImpl implements TsdrCollectorSpiService{

    private TSDRService tsdrService = null;

    public CollectorSPIImpl(TSDRService _service){
        this.tsdrService = _service;
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
        tsdrService.storeTSDRMetricRecord(tsdrServiceInput.build());
        return Futures.immediateFuture(RpcResultBuilder.<Void> success().build());
    }
}
