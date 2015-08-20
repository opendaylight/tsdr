/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.nbi;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.Future;

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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.northbound.api.rev150820.AddMetricInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.northbound.api.rev150820.TsdrNorthboundApiService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class TSDRNBIServiceImpl implements TsdrNorthboundApiService{
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
        b.setMetricName(input.getName());
        b.setMetricValue(new Counter64(new BigInteger(input.getValue())));
        b.setNodeID(input.getNodeID());
        b.setTimeStamp(System.currentTimeMillis());
        b.setTSDRDataCategory(DataCategory.forValue(input.getCategory()));
        b.setRecordKeys(parseRecordKeys(input.getPath()));
        StoreTSDRMetricRecordInputBuilder in = new StoreTSDRMetricRecordInputBuilder();
        List<TSDRMetricRecord> list = new LinkedList<>();
        list.add(b.build());
        in.setTSDRMetricRecord(list);
        store(in.build());
        RpcResultBuilder<Void> rpc = RpcResultBuilder.success();
        return rpc.buildFuture();
    }

    public List<RecordKeys> parseRecordKeys(String str) {
        List<RecordKeys> result = new LinkedList<>();
        StringTokenizer tokens = new StringTokenizer(str, ".");
        RecordKeysBuilder bld = new RecordKeysBuilder();
        bld.setKeyName("rpc");
        bld.setKeyValue("rpc");
        result.add(bld.build());
        while (tokens.hasMoreTokens()) {
            RecordKeysBuilder b = new RecordKeysBuilder();
            String key = tokens.nextToken();
            b.setKeyName(key);
            b.setKeyValue(key);
            result.add(b.build());
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
