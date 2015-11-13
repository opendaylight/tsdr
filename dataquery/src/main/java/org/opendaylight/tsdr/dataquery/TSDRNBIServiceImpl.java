/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.dataquery;

import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.StoreTSDRMetricRecordInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.StoreTSDRMetricRecordInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Counter64;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.dataquery.impl.rev150219.AddMetricInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.dataquery.impl.rev150219.TSDRDataqueryImplService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.Future;

/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class TSDRNBIServiceImpl implements TSDRDataqueryImplService{
    private static Logger logger = LoggerFactory.getLogger(TSDRNBIServiceImpl.class);
    private TSDRService tsdrService = null;
    // The reference to the the RPC registry to store the data
    private RpcProviderRegistry rpcRegistry = null;

    public TSDRNBIServiceImpl(TSDRService _tsdrService, RpcProviderRegistry _rpcRegistry){
        this.tsdrService = _tsdrService;
        this.rpcRegistry = _rpcRegistry;
    }
    @Override
    public Future<RpcResult<Void>> addMetric(AddMetricInput input) {
        TSDRMetricRecordBuilder b = new TSDRMetricRecordBuilder();
        b.setMetricName(input.getMetricName());
        b.setMetricValue(new BigDecimal(Double.parseDouble(input.getMetricValue())));
        b.setNodeID(input.getNodeID());
        b.setTimeStamp(Long.parseLong(input.getTimestamp()));
        b.setTSDRDataCategory(DataCategory.forValue(input.getCategory()));
        b.setRecordKeys(parseRecordKeys(input.getRecordKeys()));
        StoreTSDRMetricRecordInputBuilder in = new StoreTSDRMetricRecordInputBuilder();
        List<TSDRMetricRecord> list = new LinkedList<>();
        list.add(b.build());
        in.setTSDRMetricRecord(list);
        store(in.build());
        RpcResultBuilder<Void> rpc = RpcResultBuilder.success();
        return rpc.buildFuture();
    }

    public List<RecordKeys> parseRecordKeys(List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.dataquery.impl.rev150219.addmetric.input.RecordKeys> recs) {
        List<RecordKeys> result = new LinkedList<>();
        for(org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.dataquery.impl.rev150219.addmetric.input.RecordKeys rec:recs){
            RecordKeysBuilder rb = new RecordKeysBuilder();
            rb.setKeyName(rec.getKey());
            rb.setKeyValue(rec.getValue());
            result.add(rb.build());
        }
        return result;
    }

    // Invoke the storage rpc method
    private void store(StoreTSDRMetricRecordInput input) {
        if(tsdrService==null){
            tsdrService = this.rpcRegistry
                .getRpcService(TSDRService.class);
        }
        tsdrService.storeTSDRMetricRecord(input);
        logger.debug("Data Storage called");
    }
}