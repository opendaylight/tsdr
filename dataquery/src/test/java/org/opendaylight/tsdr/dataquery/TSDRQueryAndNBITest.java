/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.dataquery;

import com.google.common.util.concurrent.Futures;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.ws.rs.core.Application;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.tsdr.dataquery.rest.nbi.TSDRNbiRestAPI;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.GetTSDRLogRecordsInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.GetTSDRLogRecordsOutput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.GetTSDRLogRecordsOutputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.TsdrLogDataService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.gettsdrlogrecords.output.Logs;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.gettsdrlogrecords.output.LogsBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.tsdrlog.RecordAttributes;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.tsdrlog.RecordAttributesBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.GetTSDRAggregatedMetricsInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.GetTSDRAggregatedMetricsOutput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.GetTSDRAggregatedMetricsOutputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.GetTSDRMetricsInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.GetTSDRMetricsOutput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.GetTSDRMetricsOutputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.TsdrMetricDataService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.gettsdraggregatedmetrics.output.AggregatedMetrics;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.gettsdraggregatedmetrics.output.AggregatedMetricsBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.gettsdrmetrics.output.Metrics;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.gettsdrmetrics.output.MetricsBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

/**
 * Northbound tests.
 *
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class TSDRQueryAndNBITest extends JerseyTest {
    private static final String NBI_RESPONSE = "[{\"datapoints\":[[10.0,";

    private static TsdrMetricDataService metricDataService = Mockito.mock(TsdrMetricDataService.class);
    private static TsdrLogDataService logDataService = Mockito.mock(TsdrLogDataService.class);

    public static GetTSDRMetricsOutput createMetricRecords(boolean emptyResult) {
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
        if (!emptyResult) {
            result.add(rb.build());
        }
        GetTSDRMetricsOutputBuilder builder = new GetTSDRMetricsOutputBuilder();
        builder.setMetrics(result);
        return builder.build();
    }

    public static GetTSDRAggregatedMetricsOutput createAggregatedMetricRecords(boolean emptyResult) {
        AggregatedMetricsBuilder rb = new AggregatedMetricsBuilder();
        rb.setMetricValue(new BigDecimal(10D));
        rb.setTimeStamp(System.currentTimeMillis());
        List<AggregatedMetrics> result = new ArrayList<>();
        if (!emptyResult) {
            result.add(rb.build());
        }
        return new GetTSDRAggregatedMetricsOutputBuilder().setAggregatedMetrics(result).build();
    }

    public static GetTSDRLogRecordsOutput createLogRecords() {
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
        GetTSDRLogRecordsOutputBuilder builder = new GetTSDRLogRecordsOutputBuilder();
        builder.setLogs(result);
        return builder.build();
    }

    @Override
    protected Application configure() {
        Mockito.when(metricDataService.getTSDRMetrics(Mockito.any(GetTSDRMetricsInput.class)))
            .thenReturn(Futures.immediateFuture(RpcResultBuilder.success(createMetricRecords(false)).build()));
        Mockito.when(metricDataService.getTSDRAggregatedMetrics(Mockito.any(GetTSDRAggregatedMetricsInput.class)))
            .thenReturn(Futures.immediateFuture(
                    RpcResultBuilder.success(createAggregatedMetricRecords(false)).build()));
        Mockito.when(logDataService.getTSDRLogRecords(Mockito.any(GetTSDRLogRecordsInput.class)))
            .thenReturn(Futures.immediateFuture(RpcResultBuilder.success(createLogRecords()).build()));

        return new TSDRQueryServiceApplication(metricDataService, logDataService);
    }

    @Test
    public void testQueryForMetrics() {
        String result = target("/metrics/query").queryParam("tsdrkey", "[NID=127.0.0.1]")
                .queryParam("from","0")
                .queryParam("until","" + Long.MAX_VALUE).request().get(String.class);
        Assert.assertTrue(result, result.indexOf("NodeTest") != -1);
    }

    @Test
    public void testQueryForLogs() {
        String result = target("/logs/query").queryParam("tsdrkey", "[NID=127.0.0.1]")
                .queryParam("from","0")
                .queryParam("until","" + Long.MAX_VALUE).request().get(String.class);
        Assert.assertTrue(result.indexOf("NodeTest") != -1);

    }

    @Test
    public void testNBIForMetrics() {
        String result = target("/nbi/render").queryParam("tsdrkey", "[NID=127.0.0.1]")
                .queryParam("from","0")
                .queryParam("until","" + Long.MAX_VALUE)
                .queryParam("format","json")
                .queryParam("maxDataPoints","960")
                .request().get(String.class);
        Assert.assertNotNull(result);
        Assert.assertTrue(result.startsWith(NBI_RESPONSE));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testNBIEmptyResponseForMetrics() throws InterruptedException, ExecutionException {
        //Future<RpcResult<GetTSDRAggregatedMetricsOutput>> metric = Mockito.mock(Future.class);
        RpcResult<GetTSDRAggregatedMetricsOutput> rpcResult =
                RpcResultBuilder.success(createAggregatedMetricRecords(true)).build();
        Mockito.when(metricDataService.getTSDRAggregatedMetrics(Mockito.any(GetTSDRAggregatedMetricsInput.class)))
            .thenReturn(Futures.immediateFuture(rpcResult));

        String result = target("/nbi/render").queryParam("tsdrkey", "[NID=128.0.0.1]")
                .queryParam("from","0")
                .queryParam("until","" + Long.MAX_VALUE)
                .queryParam("format","json")
                .queryParam("maxDataPoints","960")
                .request().get(String.class);
        Assert.assertNotNull(result);
        Assert.assertEquals("{}",result);
        configure();
    }

    @Test
    public void testTimeConvertion() {
        Long time = TSDRNbiRestAPI.getTimeFromString(null);
        Assert.assertNotNull(time);
        time = TSDRNbiRestAPI.getTimeFromString("now");
        Assert.assertTrue(time > System.currentTimeMillis() - 2000);
        time = TSDRNbiRestAPI.getTimeFromString("-5min");
        Assert.assertTrue(time > System.currentTimeMillis() - 302000);
        time = TSDRNbiRestAPI.getTimeFromString("-1h");
        Assert.assertTrue(time > System.currentTimeMillis() - (60000 * 60 + 2000));
        time = TSDRNbiRestAPI.getTimeFromString("-1d");
        Assert.assertTrue(time > System.currentTimeMillis() - (60000 * 60 * 24 + 2000));
        time = TSDRNbiRestAPI.getTimeFromString("1");
        Assert.assertTrue(time == 1000);
    }
}
