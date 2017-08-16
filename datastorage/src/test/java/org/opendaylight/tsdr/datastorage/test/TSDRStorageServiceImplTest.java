/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.datastorage.test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableMap;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.opendaylight.tsdr.datastorage.TSDRMetricsMap;
import org.opendaylight.tsdr.datastorage.TSDRStorageServiceImpl;
import org.opendaylight.tsdr.spi.model.TSDRConstants;
import org.opendaylight.tsdr.spi.persistence.TSDRLogPersistenceService;
import org.opendaylight.tsdr.spi.persistence.TSDRMetricPersistenceService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.GetTSDRLogRecordsInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.StoreTSDRLogRecordInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.AggregationType;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.GetTSDRAggregatedMetricsInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.GetTSDRAggregatedMetricsOutput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.GetTSDRMetricsInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.StoreTSDRMetricRecordInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.gettsdraggregatedmetrics.output.AggregatedMetrics;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.PurgeAllTSDRRecordInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.PurgeTSDRRecordInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;

/**
 * Unit Test for TSDR Data Storage Service.
 * * @author <a href="mailto:hariharan_sethuraman@dell.com">Hariharan Sethuraman</a>
 *
 * Created: Apr 27, 2015
 */

public class TSDRStorageServiceImplTest {

    public TSDRStorageServiceImpl storageService;
    public TSDRMetricPersistenceService metricPersistenceService;
    public TSDRLogPersistenceService logPersistenceService;
    public TSDRMetricsMap MetricsMap;
    private static Map<String, List<TSDRRecord>> tableRecordMap;

    @Before
    public void setup() {
        metricPersistenceService = mock(TSDRMetricPersistenceService.class);
        logPersistenceService = mock(TSDRLogPersistenceService.class);
        storageService = new TSDRStorageServiceImpl(metricPersistenceService,logPersistenceService, null);
        MetricsMap = new TSDRMetricsMap();
        tableRecordMap = new HashMap<>();
        Answer answerStore = invocation -> {
            Object[] arguments = invocation.getArguments();
            String tableName = null;
            if (arguments != null && arguments.length > 0 && arguments[0] != null) {
                List<TSDRRecord> recordCol = (List<TSDRRecord>)arguments[0];
                for(TSDRRecord record: recordCol){
                    tableName = getTableNameFrom(record.getTSDRDataCategory());
                    if(!tableRecordMap.containsKey(tableName)){
                        tableRecordMap.put(tableName, new ArrayList<TSDRRecord>());
                    }
                    List<TSDRRecord> existingRecordCol = tableRecordMap.get(tableName);
                    System.out.println("Creating record " + record + " under table " + tableName);
                    existingRecordCol.add(record);
                }
            }
            return null;
        };
        doAnswer(answerStore).when(metricPersistenceService).storeMetric(any(List.class));
        doAnswer(answerStore).when(logPersistenceService).storeLog(any(List.class));

        Mockito.doNothing().when(metricPersistenceService).purge(any(DataCategory.class),any(long.class));
        Mockito.doNothing().when(logPersistenceService).purge(any(DataCategory.class),any(long.class));
        Mockito.doNothing().when(metricPersistenceService).purge(any(long.class));
        Mockito.doNothing().when(logPersistenceService).purge(any(long.class));
       Answer answerGetMetrics = invocation -> {
        Object[] arguments = invocation.getArguments();
        List<TSDRRecord> recordCol = new ArrayList<>();
        if (arguments != null && arguments.length > 0 && arguments[0] != null) {
            String tableName = (String)arguments[0];
            System.out.println("Retrieving metrics from table name " + tableName + " records:"+tableRecordMap);
            List<TSDRRecord> allRecordCol = tableRecordMap.get(tableName);
            System.out.println(allRecordCol);
            for(TSDRRecord record: allRecordCol){
                    recordCol.add(record);
            }
        }
        System.out.println("Get metrics of size " + recordCol.size() + ", records:" + recordCol);
        return recordCol;
    };

        doAnswer(answerGetMetrics).when(metricPersistenceService).getTSDRMetricRecords(any(String.class),any(long.class),any(long.class));
        doAnswer(answerGetMetrics).when(logPersistenceService).getTSDRLogRecords(any(String.class),any(long.class),any(long.class));
}

    @Test
    public void testpurgeAllTSDRRecord(){
        String timeStamp = new Long(new Date().getTime()).toString();
          storageService.purgeAllTSDRRecord(new PurgeAllTSDRRecordInputBuilder().setRetentionTime(new Long(timeStamp)).build());
          storageService.purgeAllTSDRRecord(new PurgeAllTSDRRecordInputBuilder().setRetentionTime(null).build());
    }

    @Test
    public void teststoreTSDRLogRecord() {
           List<TSDRLogRecord> metricCol = new ArrayList<>();
           String timeStamp = new Long(new Date().getTime()).toString();
           List<RecordKeys> recordKeys = new ArrayList<>();
           RecordKeys recordKey1 = new RecordKeysBuilder()
                   .setKeyName(DataCategory.SYSLOG.name())
                   .setKeyValue("log1").build();
               recordKeys.add(recordKey1);
           TSDRLogRecordBuilder builder1 = new TSDRLogRecordBuilder();
            metricCol.add(builder1.setIndex(1)
                   .setRecordFullText("su root failed for lonvick")
                   .setNodeID("node1.example.com")
                   .setRecordKeys(recordKeys)
                   .setTSDRDataCategory(DataCategory.SYSLOG)
                   .setTimeStamp(new Long(timeStamp)).build());
        storageService.storeTSDRLogRecord(new StoreTSDRLogRecordInputBuilder().setTSDRLogRecord(metricCol).build());
        storageService.storeTSDRLogRecord(new StoreTSDRLogRecordInputBuilder().setTSDRLogRecord(null).build());
    }

    @Test
    public void testgetTSDRLogRecord() {
           Date startDate = new Date();
           List<TSDRLogRecord> metricCol = new ArrayList<>();
           String timeStamp = new Long(new Date().getTime()).toString();
           List<RecordKeys> recordKeys = new ArrayList<>();
           RecordKeys recordKey1 = new RecordKeysBuilder()
                   .setKeyName(DataCategory.SYSLOG.name())
                   .setKeyValue("log1").build();
               recordKeys.add(recordKey1);
           TSDRLogRecordBuilder builder1 = new TSDRLogRecordBuilder();
            metricCol.add(builder1.setIndex(1)
                   .setRecordFullText("su root failed for lonvick")
                   .setNodeID("node1.example.com")
                   .setRecordKeys(recordKeys)
                   .setTSDRDataCategory(DataCategory.SYSLOG)
                   .setTimeStamp(new Long(timeStamp)).build());
            Date endDate = new Date();
        storageService.storeTSDRLogRecord(new StoreTSDRLogRecordInputBuilder().setTSDRLogRecord(metricCol).build());
        storageService.storeTSDRLogRecord(new StoreTSDRLogRecordInputBuilder().setTSDRLogRecord(null).build());
        storageService.getTSDRLogRecords(new GetTSDRLogRecordsInputBuilder()
                .setStartTime(startDate.getTime())
                .setEndTime(endDate.getTime())
                .setTSDRDataCategory(SYS_LOG_TABLE_NAME).build());
    }


    @Test
    public void testpurgeTSDRRecord(){
        String timeStamp = new Long(new Date().getTime()).toString();
            storageService.purgeTSDRRecord(new PurgeTSDRRecordInputBuilder()
             .setTSDRDataCategory(DataCategory.SYSLOG)
             .setRetentionTime(new Long(timeStamp)).build());
            storageService.purgeTSDRRecord(new PurgeTSDRRecordInputBuilder()
                    .setRetentionTime(null).build());

    }

    @Test
    public void teststoreTSDRMetricRecord(){
        List<TSDRMetricRecord> metricCol = new ArrayList<>();
        String timeStamp = new Long(new Date().getTime()).toString();
        List<RecordKeys> recordKeys = new ArrayList<>();
        RecordKeys recordKey1 = new RecordKeysBuilder()
            .setKeyName(TSDRConstants.GROUP_KEY_NAME)
            .setKeyValue("group1").build();
        recordKeys.add(recordKey1);
        TSDRMetricRecordBuilder builder1 = new TSDRMetricRecordBuilder();
        metricCol.add(builder1.setMetricName("PacketCount")
            .setMetricValue(new BigDecimal(Double.parseDouble(new Long(40).toString())))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.FLOWGROUPSTATS)
            .setTimeStamp(new Long(timeStamp)).build());
            storageService.storeTSDRMetricRecord(new StoreTSDRMetricRecordInputBuilder().setTSDRMetricRecord(metricCol).build());
            storageService.storeTSDRMetricRecord(new StoreTSDRMetricRecordInputBuilder().setTSDRMetricRecord(null).build());
    }

    @Test
    public void testgetTSDRMetricRecord(){
        Date startDate = new Date();
        List<TSDRMetricRecord> metricCol = new ArrayList<>();
        String timeStamp = new Long(new Date().getTime()).toString();
        List<RecordKeys> recordKeys = new ArrayList<>();
        RecordKeys recordKey1 = new RecordKeysBuilder()
            .setKeyName(TSDRConstants.GROUP_KEY_NAME)
            .setKeyValue("group1").build();
        recordKeys.add(recordKey1);
        TSDRMetricRecordBuilder builder1 = new TSDRMetricRecordBuilder();
        metricCol.add(builder1.setMetricName("PacketCount")
            .setMetricValue(new BigDecimal(Double.parseDouble(new Long(40).toString())))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.FLOWGROUPSTATS)
            .setTimeStamp(new Long(timeStamp)).build());
        Date endDate = new Date();
            storageService.storeTSDRMetricRecord(new StoreTSDRMetricRecordInputBuilder().setTSDRMetricRecord(metricCol).build());
            storageService.storeTSDRMetricRecord(new StoreTSDRMetricRecordInputBuilder().setTSDRMetricRecord(null).build());
            storageService.getTSDRMetrics(new GetTSDRMetricsInputBuilder()
                    .setStartTime(startDate.getTime())
                    .setEndTime(endDate.getTime())
                    .setTSDRDataCategory(GROUP_METRICS_TABLE_NAME).build());
    }

    @Test
    public void testGetTSDRAggregatedMetrics() throws InterruptedException, ExecutionException{
        // Generate and store metrics
        final ImmutableMap<Long, Double> valuesByTimestamps =
                new ImmutableMap.Builder<Long, Double>()
                    .put(0L, 100d)
                    .put(1L, 130d)
                    .put(2L, 100d)
                    .put(3L, 99d)
                    .put(4L, 101d)
                    .put(5L, 108d)
                    .build();
        for (Entry<Long, Double> valueAtTimestamp : valuesByTimestamps.entrySet()) {
            List<TSDRMetricRecord> metricCol = new ArrayList<>();
            List<RecordKeys> recordKeys = new ArrayList<>();
            RecordKeys recordKey1 = new RecordKeysBuilder()
                .setKeyName(TSDRConstants.GROUP_KEY_NAME)
                .setKeyValue("group1").build();
            recordKeys.add(recordKey1);
            TSDRMetricRecordBuilder builder1 = new TSDRMetricRecordBuilder();
            metricCol.add(builder1.setMetricName("PacketCount")
                .setMetricValue(BigDecimal.valueOf(valueAtTimestamp.getValue()))
                .setNodeID("node1")
                .setRecordKeys(recordKeys)
                .setTSDRDataCategory(DataCategory.FLOWGROUPSTATS)
                .setTimeStamp(valueAtTimestamp.getKey()).build());
            storageService.storeTSDRMetricRecord(new StoreTSDRMetricRecordInputBuilder().setTSDRMetricRecord(metricCol).build());
        }

        // Issue the RPC call to gather the aggregated results
        Future<RpcResult<GetTSDRAggregatedMetricsOutput>> future = storageService.getTSDRAggregatedMetrics(new GetTSDRAggregatedMetricsInputBuilder()
                .setTSDRDataCategory(GROUP_METRICS_TABLE_NAME)
                .setStartTime(0L)
                .setEndTime(6L)
                .setAggregation(AggregationType.MEAN)
                .setInterval(2L)
                .build());
        List<AggregatedMetrics> metrics = future.get().getResult().getAggregatedMetrics();

        // Verify
        double delta = 0.00001;
        assertEquals(4, metrics.size());
        assertEquals(115, metrics.get(0).getMetricValue().doubleValue(), delta);
        assertEquals(99.5, metrics.get(1).getMetricValue().doubleValue(),delta);
        assertEquals(104.5, metrics.get(2).getMetricValue().doubleValue(), delta);
        assertEquals(null, metrics.get(3).getMetricValue());
    }

    @Test
    public void testGetTSDRAggregatedMetricsNoMean() throws InterruptedException, ExecutionException{
        // Generate and store metrics
        final ImmutableMap<Long, Double> valuesByTimestamps =
                new ImmutableMap.Builder<Long, Double>()
                        .put(0L, 100d)
                        .put(1L, 130d)
                        .put(2L, 100d)
                        .put(3L, 99d)
                        .put(4L, 101d)
                        .put(5L, 108d)
                        .build();
        for (Entry<Long, Double> valueAtTimestamp : valuesByTimestamps.entrySet()) {
            List<TSDRMetricRecord> metricCol = new ArrayList<>();
            List<RecordKeys> recordKeys = new ArrayList<>();
            RecordKeys recordKey1 = new RecordKeysBuilder()
                    .setKeyName(TSDRConstants.GROUP_KEY_NAME)
                    .setKeyValue("group1").build();
            recordKeys.add(recordKey1);
            TSDRMetricRecordBuilder builder1 = new TSDRMetricRecordBuilder();
            metricCol.add(builder1.setMetricName("PacketCount")
                    .setMetricValue(BigDecimal.valueOf(valueAtTimestamp.getValue()))
                    .setNodeID("node1")
                    .setRecordKeys(recordKeys)
                    .setTSDRDataCategory(DataCategory.FLOWGROUPSTATS)
                    .setTimeStamp(valueAtTimestamp.getKey()).build());
            storageService.storeTSDRMetricRecord(new StoreTSDRMetricRecordInputBuilder().setTSDRMetricRecord(metricCol).build());
        }

        // Issue the RPC call to gather the aggregated results
        Future<RpcResult<GetTSDRAggregatedMetricsOutput>> future = storageService.getTSDRAggregatedMetrics(new GetTSDRAggregatedMetricsInputBuilder()
                .setTSDRDataCategory(GROUP_METRICS_TABLE_NAME)
                .setStartTime(0L)
                .setEndTime(20L)
                .setAggregation(AggregationType.MEAN)
                .setInterval(1L)
                .build());
        List<AggregatedMetrics> metrics = future.get().getResult().getAggregatedMetrics();

        // Verify
        //output should be exactly as the input above as the Mean was not applied
        double delta = 0.00001;
        assertEquals(6, metrics.size());
        assertEquals(100, metrics.get(0).getMetricValue().doubleValue(), delta);
        assertEquals(130, metrics.get(1).getMetricValue().doubleValue(),delta);
        assertEquals(100, metrics.get(2).getMetricValue().doubleValue(),delta);
        assertEquals(99,  metrics.get(3).getMetricValue().doubleValue(), delta);
        assertEquals(101, metrics.get(4).getMetricValue().doubleValue(), delta);
        assertEquals(108, metrics.get(5).getMetricValue().doubleValue(), delta);
    }

    private static final String GROUP_METRICS_TABLE_NAME = "GroupMetrics";
    private static final String SYS_LOG_TABLE_NAME = "SYSLOG";

    private static String getTableNameFrom(DataCategory datacategory){
        if ( datacategory == DataCategory.FLOWGROUPSTATS){
            return GROUP_METRICS_TABLE_NAME;
        }else if (datacategory == DataCategory.SYSLOG){
            return SYS_LOG_TABLE_NAME;
        }
        return "";
    }

    @After
    public void teardown() {
        try {
            storageService.close();
        }catch(Exception ee){}
        storageService = null;
        metricPersistenceService = null;
        logPersistenceService = null;
        tableRecordMap.clear();
    }
}
