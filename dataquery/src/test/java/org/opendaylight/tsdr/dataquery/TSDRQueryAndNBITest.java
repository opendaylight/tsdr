/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.dataquery;

import com.google.common.base.Throwables;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.test.framework.AppDescriptor;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.LowLevelAppDescriptor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.config.yang.config.TSDR_dataquery.impl.TSDRDataqueryModule;
import org.opendaylight.tsdr.dataquery.rest.nbi.TSDRNBIRestAPI;
import org.opendaylight.tsdr.dataquery.rest.query.TSDRLogQueryAPI;
import org.opendaylight.tsdr.dataquery.rest.query.TSDRMetricsQueryAPI;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetTSDRAggregatedMetricsInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetTSDRAggregatedMetricsOutput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetTSDRAggregatedMetricsOutputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetTSDRLogRecordsInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetTSDRLogRecordsOutput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetTSDRLogRecordsOutputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetTSDRMetricsInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetTSDRMetricsOutput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetTSDRMetricsOutputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.gettsdraggregatedmetrics.output.AggregatedMetrics;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.gettsdraggregatedmetrics.output.AggregatedMetricsBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.gettsdrlogrecords.output.Logs;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.gettsdrlogrecords.output.LogsBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.gettsdrmetrics.output.Metrics;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.gettsdrmetrics.output.MetricsBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrlog.RecordAttributes;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrlog.RecordAttributesBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;

/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class TSDRQueryAndNBITest extends JerseyTest{

    private QueryResourceConfig config;
    private static final String NBI_RESPONSE = "[{\"datapoints\":[[10.0,";

    public static GetTSDRMetricsOutput createMetricRecords(boolean emptyResult){
        MetricsBuilder rb = new MetricsBuilder();
        rb.setMetricValue(new BigDecimal(10D));
        rb.setTSDRDataCategory(DataCategory.EXTERNAL);
        rb.setMetricName("Test");
        rb.setNodeID("NodeTest");
        rb.setTimeStamp(System.currentTimeMillis());
        List<RecordKeys> recordKeys = new ArrayList<>();
        RecordKeysBuilder rkb = new RecordKeysBuilder();
        rkb.setKeyValue("TestV");
        rkb.setKeyName("TestK");
        recordKeys.add(rkb.build());
        rb.setRecordKeys(recordKeys);
        List<Metrics> result = new ArrayList<>();
        if(!emptyResult) {
            result.add(rb.build());
        }
        GetTSDRMetricsOutputBuilder b = new GetTSDRMetricsOutputBuilder();
        b.setMetrics(result);
        return b.build();
    }

    public static GetTSDRAggregatedMetricsOutput createAggregatedMetricRecords(boolean emptyResult){
        AggregatedMetricsBuilder rb = new AggregatedMetricsBuilder();
        rb.setMetricValue(new BigDecimal(10D));
        rb.setTimeStamp(System.currentTimeMillis());
        List<AggregatedMetrics> result = new ArrayList<>();
        if(!emptyResult) {
            result.add(rb.build());
        }
        return new GetTSDRAggregatedMetricsOutputBuilder()
                .setAggregatedMetrics(result).build();
    }

    public static GetTSDRLogRecordsOutput createLogRecords(){
        LogsBuilder rb = new LogsBuilder();
        rb.setTSDRDataCategory(DataCategory.EXTERNAL);
        rb.setRecordFullText("Record full text test");
        rb.setNodeID("NodeTest");
        rb.setTimeStamp(System.currentTimeMillis());
        List<RecordKeys> recordKeys = new ArrayList<>();
        RecordKeysBuilder rkb = new RecordKeysBuilder();
        rkb.setKeyValue("TestV");
        rkb.setKeyName("TestK");
        recordKeys.add(rkb.build());
        rb.setRecordKeys(recordKeys);
        RecordAttributesBuilder rab = new RecordAttributesBuilder();
        rab.setValue("RATestV");
        rab.setName("RATestN");
        List<RecordAttributes> raList = new ArrayList<>();
        raList.add(rab.build());
        rb.setRecordAttributes(raList);
        List<Logs> result = new ArrayList<>();
        result.add(rb.build());
        GetTSDRLogRecordsOutputBuilder b = new GetTSDRLogRecordsOutputBuilder();
        b.setLogs(result);
        return b.build();
    }

    @Override
    protected AppDescriptor configure() {
        config = new QueryResourceConfig();
        TSDRDataqueryModule.tsdrService = Mockito.mock(TSDRService.class);
        Future<RpcResult<GetTSDRMetricsOutput>> metric = Mockito.mock(Future.class);
        Future<RpcResult<GetTSDRAggregatedMetricsOutput>> metricAggregated = Mockito.mock(Future.class);
        Future<RpcResult<GetTSDRLogRecordsOutput>> metric2 = Mockito.mock(Future.class);
        RpcResult<GetTSDRMetricsOutput> rpcResult = Mockito.mock(RpcResult.class);
        RpcResult<GetTSDRAggregatedMetricsOutput> rpcResultAggregated = Mockito.mock(RpcResult.class);
        RpcResult<GetTSDRLogRecordsOutput> rpcResult2 = Mockito.mock(RpcResult.class);
        Mockito.when(TSDRDataqueryModule.tsdrService.getTSDRMetrics(Mockito.any(GetTSDRMetricsInput.class))).thenReturn(metric);
        Mockito.when(TSDRDataqueryModule.tsdrService.getTSDRAggregatedMetrics(Mockito.any(GetTSDRAggregatedMetricsInput.class))).thenReturn(metricAggregated);
        Mockito.when(TSDRDataqueryModule.tsdrService.getTSDRLogRecords(Mockito.any(GetTSDRLogRecordsInput.class))).thenReturn(metric2);
        try {
            Mockito.when(metric.get()).thenReturn(rpcResult);
            Mockito.when(metricAggregated.get()).thenReturn(rpcResultAggregated);
            Mockito.when(metric2.get()).thenReturn(rpcResult2);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        Mockito.when(rpcResult.getResult()).thenReturn(createMetricRecords(false));
        Mockito.when(rpcResultAggregated.getResult()).thenReturn(createAggregatedMetricRecords(false));
        Mockito.when(rpcResult2.getResult()).thenReturn(createLogRecords());

        return new LowLevelAppDescriptor.Builder(config).build();
    }

    @Test
    public void testQueryForMetrics(){
        WebResource webResource = resource();
        String result = webResource.path("/metrics/query").queryParam("tsdrkey", "[NID=127.0.0.1]")
                .queryParam("from","0")
                .queryParam("until",""+Long.MAX_VALUE).get(String.class);
        Assert.assertTrue(result.indexOf("NodeTest")!=-1);

    }

    @Test
    public void testQueryForLogs(){
        WebResource webResource = resource();
        String result = webResource.path("/logs/query").queryParam("tsdrkey", "[NID=127.0.0.1]")
                .queryParam("from","0")
                .queryParam("until",""+Long.MAX_VALUE).get(String.class);
        Assert.assertTrue(result.indexOf("NodeTest")!=-1);

    }

    @Test
    public void testNBIForMetrics(){
        WebResource webResource = resource();
        String result = webResource.path("/nbi/render").queryParam("tsdrkey", "[NID=127.0.0.1]")
                .queryParam("from","0")
                .queryParam("until",""+Long.MAX_VALUE)
                .queryParam("format","json")
                .queryParam("maxDataPoints","960")
                .get(String.class);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.startsWith(NBI_RESPONSE));
    }

    @Test
    public void testNBIEmptyResponseForMetrics() throws InterruptedException, ExecutionException{
        TSDRDataqueryModule.tsdrService = Mockito.mock(TSDRService.class);
        Future<RpcResult<GetTSDRAggregatedMetricsOutput>> metric = Mockito.mock(Future.class);
        RpcResult<GetTSDRAggregatedMetricsOutput> rpcResult = Mockito.mock(RpcResult.class);
        Mockito.when(TSDRDataqueryModule.tsdrService.getTSDRAggregatedMetrics(Mockito.any(GetTSDRAggregatedMetricsInput.class))).thenReturn(metric);
        Mockito.when(metric.get()).thenReturn(rpcResult);
        Mockito.when(rpcResult.getResult()).thenReturn(createAggregatedMetricRecords(true));

        WebResource webResource = resource();
        String result = webResource.path("/nbi/render").queryParam("tsdrkey", "[NID=128.0.0.1]")
                .queryParam("from","0")
                .queryParam("until",""+Long.MAX_VALUE)
                .queryParam("format","json")
                .queryParam("maxDataPoints","960")
                .get(String.class);
        Assert.assertNotNull(result);
        Assert.assertEquals("{}",result);
        configure();
    }

    @Test
    public void testTimeConvertion(){
        Long time = TSDRNBIRestAPI.getTimeFromString(null);
        Assert.assertNotNull(time);
        time = TSDRNBIRestAPI.getTimeFromString("now");
        Assert.assertTrue(time>System.currentTimeMillis()-2000);
        time = TSDRNBIRestAPI.getTimeFromString("-5min");
        Assert.assertTrue(time>System.currentTimeMillis()-302000);
        time = TSDRNBIRestAPI.getTimeFromString("-1h");
        Assert.assertTrue(time>System.currentTimeMillis()-(60000*60+2000));
        time = TSDRNBIRestAPI.getTimeFromString("-1d");
        Assert.assertTrue(time>System.currentTimeMillis()-(60000*60*24+2000));
        time = TSDRNBIRestAPI.getTimeFromString("1");
        Assert.assertTrue(time==1000);
    }

    private class QueryResourceConfig extends DefaultResourceConfig {
        public Set<Object> getSingletons() {
            HashSet<Object> set = new HashSet<>(1);
            TSDRMetricsQueryAPI qapi = new TSDRMetricsQueryAPI();
            TSDRLogQueryAPI lapi = new TSDRLogQueryAPI();
            TSDRNBIRestAPI nbi = new TSDRNBIRestAPI();
            set.add(qapi);
            set.add(lapi);
            set.add(nbi);
            return set;
        }
    }
}
