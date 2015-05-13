/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.datastorage.test;

import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.opendaylight.tsdr.datastorage.TSDRStorageServiceImpl;
import org.opendaylight.tsdr.model.TSDRConstants;
import org.opendaylight.tsdr.util.TsdrPersistenceServiceUtil;
import org.opendaylight.tsdr.persistence.spi.TsdrPersistenceService;

import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.StoreOFStatsInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.openflowstats.ObjectKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.openflowstats.ObjectKeysBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storeofstats.input.TSDROFStats;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storeofstats.input.TSDROFStatsBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.StoreTSDRMetricRecordInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRMetric;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Counter32;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Counter64;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.statistics.FlowStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.statistics.FlowStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.flow.table.statistics.FlowTableStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.flow.table.statistics.FlowTableStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.group.statistics.GroupStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.group.statistics.GroupStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.BucketId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.Bucket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.BucketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.BucketKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.statistics.BucketsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.statistics.buckets.BucketCounter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.statistics.buckets.BucketCounterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.statistics.buckets.BucketCounterKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.duration.DurationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.node.connector.statistics.PacketsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.flow.capable.node.connector.statistics.FlowCapableNodeConnectorStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.flow.capable.node.connector.statistics.FlowCapableNodeConnectorStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.flow.capable.node.connector.queue.statistics.FlowCapableNodeConnectorQueueStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.flow.capable.node.connector.queue.statistics.FlowCapableNodeConnectorQueueStatisticsBuilder;

import org.mockito.Mockito;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Matchers.any;
import org.mockito.invocation.InvocationOnMock;

import org.mockito.stubbing.Answer;

/**
 * Unit Test for TSDR Data Storage Service.
 * * @author <a href="mailto:hariharan_sethuraman@dell.com">Hariharan Sethuraman</a>
 *
 * Created: Apr 27, 2015
 */
import org.opendaylight.yangtools.yang.common.RpcResult;

public class TSDRStorageImplTest {

    public TSDRStorageServiceImpl storageService;
    public TsdrPersistenceService persistenceService;
    private static Map<String, List<TSDRMetricRecord>> tableRecordMap;

    @Before
    public void setup() {
        storageService = new TSDRStorageServiceImpl();
        persistenceService = mock(TsdrPersistenceService.class);
        TsdrPersistenceServiceUtil.setTsdrPersistenceService(persistenceService);
        tableRecordMap = new HashMap<String, List<TSDRMetricRecord>>();

        Answer answerStore = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                BigInteger timeStampBigInteger = new BigInteger((new Long((new Date()).getTime())).toString());
                String tableName = null;
                if (arguments != null && arguments.length > 0 && arguments[0] != null) {
                    if(arguments[0] instanceof TSDRMetricRecord){
                        TSDRMetricRecord record = (TSDRMetricRecord)arguments[0];
                        tableName = getTableNameFrom(record.getTSDRDataCategory());
                        if(!tableRecordMap.containsKey(tableName)){
                            tableRecordMap.put(tableName, new ArrayList<TSDRMetricRecord>());
                        }
                        List<TSDRMetricRecord> recordCol = tableRecordMap.get(tableName);
                        System.out.println("Creating record " + record + " under table " + tableName);
                        recordCol.add(record);
                        return null;
                    }
                    List<TSDRMetricRecord> recordCol = (List<TSDRMetricRecord>)arguments[0];
                    for(TSDRMetricRecord record: recordCol){
                        tableName = getTableNameFrom(record.getTSDRDataCategory());
                        if(!tableRecordMap.containsKey(tableName)){
                            tableRecordMap.put(tableName, new ArrayList<TSDRMetricRecord>());
                        }
                        List<TSDRMetricRecord> existingRecordCol = tableRecordMap.get(tableName);
                        System.out.println("Creating record " + record + " under table " + tableName);
                        existingRecordCol.add(record);
                    }
                }
                return null;
            }
        };

        doAnswer(answerStore).when(persistenceService).store(any(TSDRMetricRecord.class));
        doAnswer(answerStore).when(persistenceService).store(any(List.class));

        Answer answerGetMetrics = new Answer() {
            @Override
            public List<?> answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                List<TSDRMetricRecord> recordCol = new ArrayList<TSDRMetricRecord>();
                if (arguments != null && arguments.length > 0 && arguments[0] != null) {
                    String tableName = (String)arguments[0];
                    Date startDate = (Date)arguments[1];
                    Date endDate = (Date)arguments[2];
                    System.out.println("Retrieving metrics from table name " + tableName + " records:"+tableRecordMap);
                    List<TSDRMetricRecord> allRecordCol = tableRecordMap.get(tableName);
                    System.out.println(allRecordCol);
                    if(allRecordCol == null){
                        return recordCol;
                    }
                    for(TSDRMetricRecord record: allRecordCol){
                        if(record.getTimeStamp().longValue() >= startDate.getTime() && record.getTimeStamp().longValue() <= endDate.getTime()){
                            recordCol.add(record);
                        }
                    }
                }
                System.out.println("Get metrics of size " + recordCol.size() + ", records:" + recordCol);
                return recordCol;
            }
        };

        doAnswer(answerGetMetrics).when(persistenceService).getMetrics(any(String.class),any(Date.class),any(Date.class));

    }

    @Test
    public void testFlowStatistics() {
        Date startDate = new Date();
        List<TSDRMetricRecord> metricCol = new ArrayList<TSDRMetricRecord>();
        String timeStamp = (new Long((new Date()).getTime())).toString();
        List<RecordKeys> recordKeys = new ArrayList<RecordKeys>();
        RecordKeys recordKey1 = new RecordKeysBuilder()
            .setKeyName(TSDRConstants.FLOW_KEY_NAME)
            .setKeyValue("flow1").build();
        RecordKeys recordKey2 = new RecordKeysBuilder()
            .setKeyName(TSDRConstants.FLOW_TABLE_KEY_NAME)
            .setKeyValue("table1").build();
        recordKeys.add(recordKey1);
        recordKeys.add(recordKey2);

        TSDRMetricRecordBuilder builder1 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric1 =   builder1.setMetricName("PacketCount")
            .setMetricValue(new Counter64(new BigInteger("10000000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.FLOWSTATS)
            .setTimeStamp(new Long(timeStamp)).build();

        TSDRMetricRecordBuilder builder2 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric2 =   builder2.setMetricName("ByteCount")
            .setMetricValue(new Counter64(new BigInteger("10000000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.FLOWSTATS)
            .setTimeStamp(new Long(timeStamp)).build();

        metricCol.add((TSDRMetricRecord)tsdrMetric1);
        metricCol.add((TSDRMetricRecord)tsdrMetric2);
        storageService
            .storeTSDRMetricRecord(new StoreTSDRMetricRecordInputBuilder().setTSDRMetricRecord(metricCol).build());

        Date endDate = new Date();
        boolean result = false;
        try{
            result = persistenceService.getMetrics(getTableNameFrom(DataCategory.FLOWSTATS), startDate, endDate).size() == 2;
        }catch(Exception ee){
            System.out.println("Error retrieving metrics from flow stats table with specified time range.");
            result = false;
            ee.printStackTrace();
        }

        assertTrue(result);
    }

    @Test
    public void testFlowTableStatistics() {
        Date startDate = new Date();
        List<TSDRMetricRecord> metricCol = new ArrayList<TSDRMetricRecord>();
        String timeStamp = (new Long((new Date()).getTime())).toString();
        List<RecordKeys> recordKeys = new ArrayList<RecordKeys>();
        RecordKeys recordKey = new RecordKeysBuilder()
            .setKeyName(TSDRConstants.FLOW_TABLE_KEY_NAME)
            .setKeyValue("table1").build();
        recordKeys.add(recordKey);

        TSDRMetricRecordBuilder builder1 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric1 =   builder1.setMetricName("PacketsMatched")
            .setMetricValue(new Counter64(new BigInteger("20000000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.FLOWTABLESTATS)
            .setTimeStamp(new Long(timeStamp)).build();

        TSDRMetricRecordBuilder builder2 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric2 =   builder2.setMetricName("ActiveFlows")
            .setMetricValue(new Counter64(new BigInteger("20000000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.FLOWTABLESTATS)
            .setTimeStamp(new Long(timeStamp)).build();

        TSDRMetricRecordBuilder builder3 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric3 =   builder3.setMetricName("PacketsLookedUp")
            .setMetricValue(new Counter64(new BigInteger("20000000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.FLOWTABLESTATS)
            .setTimeStamp(new Long(timeStamp)).build();

        metricCol.add((TSDRMetricRecord)tsdrMetric1);
        metricCol.add((TSDRMetricRecord)tsdrMetric2);
        metricCol.add((TSDRMetricRecord)tsdrMetric3);
        storageService
            .storeTSDRMetricRecord(new StoreTSDRMetricRecordInputBuilder().setTSDRMetricRecord(metricCol).build());

        boolean result = false;
        Date endDate = new Date();
        try{
            result = persistenceService.getMetrics(getTableNameFrom(DataCategory.FLOWTABLESTATS),startDate, endDate).size() == 3;
        }catch(Exception ee){
            System.out.println("Error retrieving metrics from flowtable stats table with specified time range.");
            result = false;
            ee.printStackTrace();
        }

        assertTrue(result);

    }

    @Test
    public void testPortStatistics() {
        Date startDate = new Date();
        List<TSDRMetricRecord> metricCol = new ArrayList<TSDRMetricRecord>();
        String timeStamp = (new Long((new Date()).getTime())).toString();
        List<RecordKeys> recordKeys = new ArrayList<RecordKeys>();
        RecordKeys recordKey = new RecordKeysBuilder()
            .setKeyName(TSDRConstants.INTERNFACE_KEY_NAME)
            .setKeyValue("port1").build();
        recordKeys.add(recordKey);

        TSDRMetricRecordBuilder builder1 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric1 =   builder1.setMetricName("CollisionCount")
            .setMetricValue(new Counter64(new BigInteger("2000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.PORTSTATS)
            .setTimeStamp(new Long(timeStamp)).build();


        TSDRMetricRecordBuilder builder2 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric2 =   builder2.setMetricName("ReceiveCRCError")
            .setMetricValue(new Counter64(new BigInteger("2000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.PORTSTATS)
            .setTimeStamp(new Long(timeStamp)).build();


        TSDRMetricRecordBuilder builder3 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric3 =   builder3.setMetricName("ReceivedDrops")
            .setMetricValue(new Counter64(new BigInteger("2000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.PORTSTATS)
            .setTimeStamp(new Long(timeStamp)).build();


        TSDRMetricRecordBuilder builder4 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric4 =   builder4.setMetricName("ReceivedErrors")
            .setMetricValue(new Counter64(new BigInteger("2000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.PORTSTATS)
            .setTimeStamp(new Long(timeStamp)).build();


        TSDRMetricRecordBuilder builder5 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric5 =   builder5.setMetricName("ReceiveFrameError")
            .setMetricValue(new Counter64(new BigInteger("2000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.PORTSTATS)
            .setTimeStamp(new Long(timeStamp)).build();


        TSDRMetricRecordBuilder builder6 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric6 =   builder6.setMetricName("ReceiveOverRunError")
            .setMetricValue(new Counter64(new BigInteger("2000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.PORTSTATS)
            .setTimeStamp(new Long(timeStamp)).build();


        TSDRMetricRecordBuilder builder7 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric7 =   builder7.setMetricName("TransmitDrops")
            .setMetricValue(new Counter64(new BigInteger("2000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.PORTSTATS)
            .setTimeStamp(new Long(timeStamp)).build();


        TSDRMetricRecordBuilder builder8 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric8 =   builder8.setMetricName("TransmitErrors")
            .setMetricValue(new Counter64(new BigInteger("2000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.PORTSTATS)
            .setTimeStamp(new Long(timeStamp)).build();


        TSDRMetricRecordBuilder builder9 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric9 =   builder9.setMetricName("ReceivedPackets")
            .setMetricValue(new Counter64(new BigInteger("2000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.PORTSTATS)
            .setTimeStamp(new Long(timeStamp)).build();

        TSDRMetricRecordBuilder builder10 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric10 =   builder10.setMetricName("TransmittedPackets")
            .setMetricValue(new Counter64(new BigInteger("2000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.PORTSTATS)
            .setTimeStamp(new Long(timeStamp)).build();


        TSDRMetricRecordBuilder builder11 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric11 =   builder11.setMetricName("ReceivedBytes")
            .setMetricValue(new Counter64(new BigInteger("20000000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.PORTSTATS)
            .setTimeStamp(new Long(timeStamp)).build();


        TSDRMetricRecordBuilder builder12 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric12 =   builder12.setMetricName("TransmittedBytes")
            .setMetricValue(new Counter64(new BigInteger("20000000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.PORTSTATS)
            .setTimeStamp(new Long(timeStamp)).build();

        TSDRMetricRecordBuilder builder13 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric13 =   builder13.setMetricName("DurationInSeconds")
            .setMetricValue(new Counter64(new BigInteger("20")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.PORTSTATS)
            .setTimeStamp(new Long(timeStamp)).build();


        TSDRMetricRecordBuilder builder14 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric14 =   builder14.setMetricName("DurationInNanoSeconds")
            .setMetricValue(new Counter64(new BigInteger("20")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.PORTSTATS)
            .setTimeStamp(new Long(timeStamp)).build();

        metricCol.add((TSDRMetricRecord)tsdrMetric1);
        metricCol.add((TSDRMetricRecord)tsdrMetric2);
        metricCol.add((TSDRMetricRecord)tsdrMetric3);
        metricCol.add((TSDRMetricRecord)tsdrMetric4);
        metricCol.add((TSDRMetricRecord)tsdrMetric5);
        metricCol.add((TSDRMetricRecord)tsdrMetric6);
        metricCol.add((TSDRMetricRecord)tsdrMetric7);
        metricCol.add((TSDRMetricRecord)tsdrMetric8);
        metricCol.add((TSDRMetricRecord)tsdrMetric9);
        metricCol.add((TSDRMetricRecord)tsdrMetric10);
        metricCol.add((TSDRMetricRecord)tsdrMetric11);
        metricCol.add((TSDRMetricRecord)tsdrMetric12);
        metricCol.add((TSDRMetricRecord)tsdrMetric13);
        metricCol.add((TSDRMetricRecord)tsdrMetric14);
        storageService
            .storeTSDRMetricRecord(new StoreTSDRMetricRecordInputBuilder().setTSDRMetricRecord(metricCol).build());

        boolean result = false;
        Date endDate = new Date();
        try{
            result = persistenceService.getMetrics(getTableNameFrom(DataCategory.PORTSTATS),startDate, endDate).size() == 14;
        }catch(Exception ee){
            System.out.println("Error retrieving metrics from port stats table with specified time range.");
            result = false;
            ee.printStackTrace();
        }

        assertTrue(result);
    }

    @Test
    public void testQueueStatistics() {
        Date startDate = new Date();
        List<TSDRMetricRecord> metricCol = new ArrayList<TSDRMetricRecord>();
        String timeStamp = (new Long((new Date()).getTime())).toString();
        List<RecordKeys> recordKeys = new ArrayList<RecordKeys>();
        RecordKeys recordKey1 = new RecordKeysBuilder()
            .setKeyName(TSDRConstants.QUEUE_KEY_NAME)
            .setKeyValue("queue1").build();
        RecordKeys recordKey2 = new RecordKeysBuilder()
            .setKeyName(TSDRConstants.INTERNFACE_KEY_NAME)
            .setKeyValue("port1").build();
        recordKeys.add(recordKey1);
        recordKeys.add(recordKey2);

        TSDRMetricRecordBuilder builder1 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric1 =   builder1.setMetricName("TransmissionErrors")
            .setMetricValue(new Counter64(new BigInteger("3000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.QUEUESTATS)
            .setTimeStamp(new Long(timeStamp)).build();

       TSDRMetricRecordBuilder builder2 = new TSDRMetricRecordBuilder();

       TSDRMetric tsdrMetric2 =   builder2.setMetricName("TransmittedBytes")
            .setMetricValue(new Counter64(new BigInteger("3000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.QUEUESTATS)
            .setTimeStamp(new Long(timeStamp)).build();

       TSDRMetricRecordBuilder builder3 = new TSDRMetricRecordBuilder();

       TSDRMetric tsdrMetric3 =   builder3.setMetricName("TransmittedPackets")
            .setMetricValue(new Counter64(new BigInteger("3000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.QUEUESTATS)
            .setTimeStamp(new Long(timeStamp)).build();
        metricCol.add((TSDRMetricRecord)tsdrMetric1);
        metricCol.add((TSDRMetricRecord)tsdrMetric2);
        metricCol.add((TSDRMetricRecord)tsdrMetric3);
        storageService
            .storeTSDRMetricRecord(new StoreTSDRMetricRecordInputBuilder().setTSDRMetricRecord(metricCol).build());

        boolean result = false;
        Date endDate = new Date();
        try{
            int colsize = persistenceService.getMetrics(getTableNameFrom(DataCategory.QUEUESTATS),startDate, endDate).size();
            System.out.println("collection size " + colsize);
            result = colsize == 3;
        }catch(Exception ee){
            System.out.println("Error retrieving metrics from queue stats table with specified time range.");
            result = false;
            ee.printStackTrace();
        }

        assertTrue(result);
    }

    @Test
    public void testFlowMeterStatistics() {
        Date startDate = new Date();
        List<TSDRMetricRecord> metricCol = new ArrayList<TSDRMetricRecord>();
        String timeStamp = (new Long((new Date()).getTime())).toString();
        List<RecordKeys> recordKeys = new ArrayList<RecordKeys>();
        RecordKeys recordKey1 = new RecordKeysBuilder()
            .setKeyName(TSDRConstants.METER_KEY_NAME)
            .setKeyValue("meter1").build();
        RecordKeys recordKey2 = new RecordKeysBuilder()
            .setKeyName(TSDRConstants.GROUP_KEY_NAME)
            .setKeyValue("group1").build();
        recordKeys.add(recordKey1);
        recordKeys.add(recordKey2);

        TSDRMetricRecordBuilder builder1 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric1 =   builder1.setMetricName("PacketCount")
            .setMetricValue(new Counter64(new BigInteger(new Long(40).toString())))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.FLOWMETERSTATS)
            .setTimeStamp(new Long(timeStamp)).build();

       TSDRMetricRecordBuilder builder2 = new TSDRMetricRecordBuilder();

       TSDRMetric tsdrMetric2 =   builder2.setMetricName("ByteCount")
            .setMetricValue(new Counter64(new BigInteger(new Long(40).toString())))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.FLOWMETERSTATS)
            .setTimeStamp(new Long(timeStamp)).build();

       TSDRMetricRecordBuilder builder3 = new TSDRMetricRecordBuilder();

       TSDRMetric tsdrMetric3 =   builder3.setMetricName("RefCount")
            .setMetricValue(new Counter64(new BigInteger("4000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.FLOWMETERSTATS)
            .setTimeStamp(new Long(timeStamp)).build();

        metricCol.add((TSDRMetricRecord)tsdrMetric1);
        metricCol.add((TSDRMetricRecord)tsdrMetric2);
        metricCol.add((TSDRMetricRecord)tsdrMetric3);
        storageService
            .storeTSDRMetricRecord(new StoreTSDRMetricRecordInputBuilder().setTSDRMetricRecord(metricCol).build());


        boolean result = false;
        Date endDate = new Date();
        try{
            result = persistenceService.getMetrics(getTableNameFrom(DataCategory.FLOWMETERSTATS),startDate, endDate).size() == 3;
        }catch(Exception ee){
            System.out.println("Error retrieving metrics from flow meter stats table with specified time range.");
            result = false;
            ee.printStackTrace();
        }

        assertTrue(result);
    }


    @Test
    public void testGroupStatistics() {
        Date startDate = new Date();
        List<TSDRMetricRecord> metricCol = new ArrayList<TSDRMetricRecord>();
        String timeStamp = (new Long((new Date()).getTime())).toString();
        List<RecordKeys> recordKeys = new ArrayList<RecordKeys>();
        RecordKeys recordKey1 = new RecordKeysBuilder()
            .setKeyName(TSDRConstants.GROUP_KEY_NAME)
            .setKeyValue("group1").build();
        RecordKeys recordKey2 = new RecordKeysBuilder()
            .setKeyName(TSDRConstants.BUCKET_KEY_NAME)
            .setKeyValue("bucket1").build();
        recordKeys.add(recordKey1);
        recordKeys.add(recordKey2);

        TSDRMetricRecordBuilder builder1 = new TSDRMetricRecordBuilder();

        TSDRMetric tsdrMetric1 =   builder1.setMetricName("PacketCount")
            .setMetricValue(new Counter64(new BigInteger(new Long(40).toString())))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.FLOWGROUPSTATS)
            .setTimeStamp(new Long(timeStamp)).build();

       TSDRMetricRecordBuilder builder2 = new TSDRMetricRecordBuilder();

       TSDRMetric tsdrMetric2 =   builder2.setMetricName("ByteCount")
            .setMetricValue(new Counter64(new BigInteger(new Long(40).toString())))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.FLOWGROUPSTATS)
            .setTimeStamp(new Long(timeStamp)).build();

       TSDRMetricRecordBuilder builder3 = new TSDRMetricRecordBuilder();

       TSDRMetric tsdrMetric3 =   builder3.setMetricName("RefCount")
            .setMetricValue(new Counter64(new BigInteger("4000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.FLOWGROUPSTATS)
            .setTimeStamp(new Long(timeStamp)).build();

        metricCol.add((TSDRMetricRecord)tsdrMetric1);
        metricCol.add((TSDRMetricRecord)tsdrMetric2);
        metricCol.add((TSDRMetricRecord)tsdrMetric3);
        storageService
            .storeTSDRMetricRecord(new StoreTSDRMetricRecordInputBuilder().setTSDRMetricRecord(metricCol).build());


        boolean result = false;
        Date endDate = new Date();
        try{
            result = persistenceService.getMetrics(getTableNameFrom(DataCategory.FLOWGROUPSTATS),startDate, endDate).size() == 3;
        }catch(Exception ee){
            System.out.println("Error retrieving metrics from flow group stats table with specified time range.");
            result = false;
            ee.printStackTrace();
        }

        assertTrue(result);
    }

    private static final String FLOW_STATS_TABLE_NAME = "FlowMetrics";
    private static final String FLOW_TABLE_STATS_TABLE_NAME = "FlowTableMetrics";
    private static final String INTERFACE_METRICS_TABLE_NAME = "InterfaceMetrics";
    private static final String QUEUE_METRICS_TABLE_NAME = "QueueMetrics";
    private static final String GROUP_METRICS_TABLE_NAME = "GroupMetrics";
    private static final String METER_METRICS_TABLE_NAME = "MeterMetrics";

    private static String getTableNameFrom(DataCategory datacategory){
        if ( datacategory == DataCategory.FLOWSTATS){
            return FLOW_STATS_TABLE_NAME;
        }else if ( datacategory == DataCategory.FLOWTABLESTATS){
            return FLOW_TABLE_STATS_TABLE_NAME;
        }else if ( datacategory == DataCategory.FLOWGROUPSTATS){
            return GROUP_METRICS_TABLE_NAME;
        }else if (datacategory == DataCategory.PORTSTATS){
            return  INTERFACE_METRICS_TABLE_NAME;
        }else if (datacategory == DataCategory.QUEUESTATS){
            return QUEUE_METRICS_TABLE_NAME;
        }else if (datacategory == DataCategory.FLOWMETERSTATS){
            return METER_METRICS_TABLE_NAME;
        }

        return "";
    }

    @After
    public void teardown() {
        storageService = null;
        persistenceService = null;
        tableRecordMap.clear();
    }
}
