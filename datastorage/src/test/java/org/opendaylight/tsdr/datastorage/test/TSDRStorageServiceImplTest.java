/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.datastorage.test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.tsdr.datastorage.TSDRMetricsMap;
import org.opendaylight.tsdr.datastorage.TSDRStorageServiceImpl;
import org.opendaylight.tsdr.spi.model.TSDRConstants;
import org.opendaylight.tsdr.spi.persistence.TsdrPersistenceService;
import org.opendaylight.tsdr.spi.util.TsdrPersistenceServiceUtil;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetTSDRLogRecordsInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetTSDRMetricsInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.PurgeAllTSDRRecordInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.PurgeTSDRRecordInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.StoreTSDRLogRecordInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.StoreTSDRMetricRecordInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrlogrecord.input.TSDRLogRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;
/**
 * Unit Test for TSDR Data Storage Service.
 * * @author <a href="mailto:hariharan_sethuraman@dell.com">Hariharan Sethuraman</a>
 *
 * Created: Apr 27, 2015
 */

public class TSDRStorageServiceImplTest {

    public TSDRStorageServiceImpl storageService;
    public TsdrPersistenceService persistenceService;
    public TSDRMetricsMap MetricsMap;
    private static Map<String, List<TSDRRecord>> tableRecordMap;

    @Before
    public void setup() {
        storageService = new TSDRStorageServiceImpl();
        persistenceService = mock(TsdrPersistenceService.class);
        MetricsMap = new TSDRMetricsMap();
        TsdrPersistenceServiceUtil.setTsdrPersistenceService(persistenceService);
        tableRecordMap = new HashMap<String, List<TSDRRecord>>();
        Answer answerStore = new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
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
            }
        };
        doAnswer(answerStore).when(persistenceService).store(any(List.class));

        Mockito.doNothing().when(persistenceService).purgeTSDRRecords(any(DataCategory.class),any(long.class));
        Mockito.doNothing().when(persistenceService).purgeAllTSDRRecords(any(long.class));
       Answer answerGetMetrics = new Answer() {
            @Override
            public List<?> answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                List<TSDRRecord> recordCol = new ArrayList<TSDRRecord>();
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
            }
        };

        doAnswer(answerGetMetrics).when(persistenceService).getTSDRMetricRecords(any(String.class),any(long.class),any(long.class));
        doAnswer(answerGetMetrics).when(persistenceService).getTSDRLogRecords(any(String.class),any(long.class),any(long.class));
}

    @Test
    public void testpurgeAllTSDRRecord(){
        String timeStamp = (new Long((new Date()).getTime())).toString();
          storageService.purgeAllTSDRRecord(new PurgeAllTSDRRecordInputBuilder().setRetentionTime(new Long(timeStamp)).build());
          storageService.purgeAllTSDRRecord(new PurgeAllTSDRRecordInputBuilder().setRetentionTime(null).build());
    }

    @Test
    public void teststoreTSDRLogRecord() {
           List<TSDRLogRecord> metricCol = new ArrayList<TSDRLogRecord>();
           String timeStamp = (new Long((new Date()).getTime())).toString();
           List<RecordKeys> recordKeys = new ArrayList<RecordKeys>();
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
           List<TSDRLogRecord> metricCol = new ArrayList<TSDRLogRecord>();
           String timeStamp = (new Long((new Date()).getTime())).toString();
           List<RecordKeys> recordKeys = new ArrayList<RecordKeys>();
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
        String timeStamp = (new Long((new Date()).getTime())).toString();
            storageService.purgeTSDRRecord(new PurgeTSDRRecordInputBuilder()
             .setTSDRDataCategory(DataCategory.SYSLOG)
             .setRetentionTime(new Long(timeStamp)).build());
            storageService.purgeTSDRRecord(new PurgeTSDRRecordInputBuilder()
                    .setRetentionTime(null).build());

    }

    @Test
    public void teststoreTSDRMetricRecord(){
        List<TSDRMetricRecord> metricCol = new ArrayList<TSDRMetricRecord>();
        String timeStamp = (new Long((new Date()).getTime())).toString();
        List<RecordKeys> recordKeys = new ArrayList<RecordKeys>();
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
        List<TSDRMetricRecord> metricCol = new ArrayList<TSDRMetricRecord>();
        String timeStamp = (new Long((new Date()).getTime())).toString();
        List<RecordKeys> recordKeys = new ArrayList<RecordKeys>();
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
        persistenceService = null;
        tableRecordMap.clear();
    }
}
