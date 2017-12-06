/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.dataquery;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import org.opendaylight.tsdr.dataquery.rest.nbi.TSDRNbiRestAPI;
import org.opendaylight.tsdr.dataquery.rest.query.TSDRLogQueryAPI;
import org.opendaylight.tsdr.dataquery.rest.query.TSDRMetricsQueryAPI;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.dataquery.impl.rev150219.AddLogInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.dataquery.impl.rev150219.AddMetricInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.dataquery.impl.rev150219.TSDRDataqueryImplService;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the TSDRDataqueryImplService interface.
 *
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class TSDRNbiServiceImpl implements TSDRDataqueryImplService {
    private static Logger LOG = LoggerFactory.getLogger(TSDRNbiServiceImpl.class);

    private final TsdrMetricDataService metricDataService;
    private final TsdrLogDataService logDataService;

    // The reference to the the RPC registry to store the data

    public TSDRNbiServiceImpl(TsdrMetricDataService metricService, TsdrLogDataService logService) {
        logDataService = logService;
        metricDataService = metricService;

        TSDRNbiRestAPI.setMetricDataService(metricService);
        TSDRLogQueryAPI.setLogDataService(logService);
        TSDRMetricsQueryAPI.setMetricDataService(metricService);
    }

    @Override
    public Future<RpcResult<Void>> addMetric(AddMetricInput input) {
        TSDRMetricRecordBuilder builder = new TSDRMetricRecordBuilder();
        builder.setMetricName(input.getMetricName());
        builder.setMetricValue(input.getMetricValue());
        builder.setNodeID(input.getNodeID());
        builder.setTimeStamp(input.getTimeStamp());
        builder.setTSDRDataCategory(input.getTSDRDataCategory());
        builder.setRecordKeys(input.getRecordKeys());
        StoreTSDRMetricRecordInputBuilder in = new StoreTSDRMetricRecordInputBuilder();
        List<TSDRMetricRecord> list = new LinkedList<>();
        list.add(builder.build());
        in.setTSDRMetricRecord(list);
        store(in.build());
        RpcResultBuilder<Void> rpc = RpcResultBuilder.success();
        return rpc.buildFuture();
    }

    @Override
    public Future<RpcResult<Void>> addLog(AddLogInput input) {
        TSDRLogRecordBuilder builder = new TSDRLogRecordBuilder();
        builder.setRecordFullText(input.getRecordFullText());
        builder.setNodeID(input.getNodeID());
        builder.setTimeStamp(input.getTimeStamp());
        builder.setTSDRDataCategory(input.getTSDRDataCategory());
        builder.setRecordKeys(input.getRecordKeys());
        builder.setRecordAttributes(input.getRecordAttributes());
        StoreTSDRLogRecordInputBuilder in = new StoreTSDRLogRecordInputBuilder();
        List<TSDRLogRecord> list = new LinkedList<>();
        list.add(builder.build());
        in.setTSDRLogRecord(list);
        store(in.build());
        RpcResultBuilder<Void> rpc = RpcResultBuilder.success();
        return rpc.buildFuture();

    }

    // Invoke the storage rpc method
    private void store(StoreTSDRMetricRecordInput input) {
        logResult(metricDataService.storeTSDRMetricRecord(input), "storeTSDRMetricRecord");
    }

    // Invoke the storage rpc method
    private void store(StoreTSDRLogRecordInput input) {
        logResult(logDataService.storeTSDRLogRecord(input), "storeTSDRLogRecord");
    }

    private void logResult(Future<RpcResult<Void>> future, String rpc) {
        Futures.addCallback(JdkFutureAdapters.listenInPoolThread(future), new FutureCallback<RpcResult<Void>>() {
            @Override
            public void onSuccess(RpcResult<Void> result) {
                LOG.debug("RPC {} returned result {]", rpc, result);
            }

            @Override
            public void onFailure(Throwable ex) {
                LOG.error("RPC {} failed", rpc, ex);
            }
        }, MoreExecutors.directExecutor());
    }
}
