/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 * Copyright (c) 2015 xFlow Research Inc. and others.  All rights reserved
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.datastorage.persistence.hbase;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ScheduledFuture;

import org.apache.hadoop.hbase.TableNotFoundException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.tsdr.persistence.hbase.HBaseColumn;
import org.opendaylight.tsdr.persistence.hbase.HBaseDataStore;
import org.opendaylight.tsdr.persistence.hbase.HBaseEntity;
import org.opendaylight.tsdr.persistence.hbase.TSDRHBasePersistenceServiceImpl;
import org.opendaylight.tsdr.spi.model.TSDRConstants;
import org.opendaylight.tsdr.spi.scheduler.SchedulerService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRLog;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRMetric;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrlogrecord.input.TSDRLogRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;

import jline.internal.ShutdownHooks.Task;
import junit.framework.Assert;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * Unit Test for HBase data store under TSDR.
 * * @author <a href="mailto:hariharan_sethuraman@dell.com">Hariharan Sethuraman</a>
 * <p/>
 * Created: Apr 27, 2015
 *
 * Revision: Dec 5, 2015
 * @author <a href="mailto:chaudhry.usama@xflowresearch.com">Chaudhry Muhammad Usama </a>
 */

public class TSDRHBasePersistenceServiceImplTest {
    public TSDRHBasePersistenceServiceImpl storageService = null;
    private HBaseDataStore hbaseDataStore;
    private SchedulerService schedulerService;
    private ScheduledFuture future;
    private static Map<String, Map<String,List<HBaseEntity>>> tableEntityMap;
    @Before
    public void setup() {
        hbaseDataStore = mock(HBaseDataStore.class);
        future = mock(ScheduledFuture.class);
        schedulerService = mock(SchedulerService.class);
        storageService = new TSDRHBasePersistenceServiceImpl(hbaseDataStore,future);
        tableEntityMap = new HashMap<String, Map<String, List<HBaseEntity>>>();
        try{
            doAnswer(new Answer<HBaseEntity>() {
                @Override
                public HBaseEntity answer(InvocationOnMock invocation) throws Throwable {
                    Object[] arguments = invocation.getArguments();
                    if (arguments != null && arguments.length > 0 && arguments[0] != null) {
                        HBaseEntity entity = (HBaseEntity) arguments[0];
                        if(!tableEntityMap.containsKey(entity.getTableName())){
                            tableEntityMap.put(entity.getTableName(),new TreeMap<String,List<HBaseEntity>>());
                        }
                        Map<String,List<HBaseEntity>> entityMap = tableEntityMap.get(entity.getTableName());
                        if(!entityMap.containsKey(entity.getRowKey())){
                            entityMap.put(entity.getRowKey(), new ArrayList<HBaseEntity>());
                        }
                        List<HBaseEntity> entitiesCol = entityMap.get(entity.getRowKey());
                        entitiesCol.add(entity);
                        System.out.println("Creating entity:"+entity.getRowKey() + ",table:" + entity.getTableName());
                        return entity;
                    }
                    return null;
                }
            }).when(hbaseDataStore).create(any(HBaseEntity.class));
        }catch(Exception ee){
            System.out.println("Error while creating TSDR record in HBase data store {}");
            ee.printStackTrace();
        }
        /**
         * Mocking up the create a list of rows in HTable
         * List<HBaseEntity> create(List<HBaseEntity> entityList)
         * @param entityList - a list of objects of HBaseEntity.
         */
        try{
            doAnswer(new Answer() {
                @Override
                public Object answer(InvocationOnMock invocation) throws Throwable {
                    Object[] arguments = invocation.getArguments();
                    if (arguments != null && arguments.length > 0 && arguments[0] != null) {
                        List<HBaseEntity> entityList = (List<HBaseEntity>) arguments[0];
                         for(HBaseEntity entity: entityList){
                          if(!tableEntityMap.containsKey(entity.getTableName())){
                              tableEntityMap.put(entity.getTableName(),new TreeMap<String,List<HBaseEntity>>());
                          }
                          Map<String,List<HBaseEntity>> entityMap = tableEntityMap.get(entity.getTableName());
                          if(!entityMap.containsKey(entity.getRowKey())){
                              entityMap.put(entity.getRowKey(), new ArrayList<HBaseEntity>());
                          }
                          List<HBaseEntity> entitiesCol = entityMap.get(entity.getRowKey());
                          entitiesCol.add(entity);
                          System.out.println("Creating entity List:"+entity.getRowKey() + ",table:" + entity.getTableName());
                          }
                         return null;
                    }
                    return null;
                }
            }).when(hbaseDataStore).create(any(ArrayList.class));
        }catch(Exception ee){
            System.out.println("Error while creating TSDR records with list in HBase data store {}");
            ee.printStackTrace();
        }
    doAnswer(new Answer() {
            @Override
            public List<HBaseEntity> answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                List<HBaseEntity> entityCol = new ArrayList<HBaseEntity>();
                if (arguments != null && arguments.length > 0 && arguments[0] != null) {
                    String tableName = (String) arguments[0];
                    Long startTime = (Long) arguments[1];
                    Long endTime = (Long) arguments[2];
                    Map<String, List<HBaseEntity>> entityMap = tableEntityMap.get(tableName);
                    if(entityMap == null){
                        return entityCol;
                    }
                    for(List<HBaseEntity> entityValues: entityMap.values()){
                        for(HBaseEntity entity: entityValues){
                            for (HBaseColumn currentColumn : entity.getColumns()) {
                                if(currentColumn.getTimeStamp() >= startTime && currentColumn.getTimeStamp() <= endTime){
                                     entityCol.add(entity);
                                     break;
                                }
                            }
                        }
                    }
                    System.out.println("Retrieving entities:"+entityCol + ",table:" + tableName + " from date:"+startTime + " end date" + endTime);
                }
                return entityCol;
            }
       }).when(hbaseDataStore).getDataByTimeRange(any(String.class), any(Long.class), any(Long.class));
        try{
        doAnswer(new Answer() {
        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        List<HBaseEntity> Deletelist = new ArrayList<HBaseEntity>();
        if (args != null && args.length > 0 && args[0] != null ) {
                String tableName = (String) args[0];
                Long retentionTime = (Long) args[1];
                Map<String, List<HBaseEntity>> entityMap = tableEntityMap.get(tableName);
                if (entityMap == null) {
                    return null;
                }
                for(List<HBaseEntity> entityValues: entityMap.values()){
                        for(HBaseEntity entity: entityValues){
                                for (HBaseColumn currentColumn : entity.getColumns()) {
                                        if(currentColumn.getTimeStamp() <= retentionTime){
                                                Deletelist.add(entity);
                                                }
                                        }
                                }
                        }
                for (HBaseEntity entity: Deletelist){
                        entityMap.remove(entity.getRowKey());
                }
                System.out.println("Purging entries:"+ Deletelist + " from table:" + tableName + " till time:"+retentionTime);
        }
        return null;
        }}).when(hbaseDataStore).deleteByTimestamp(any(String.class), any(Long.class));
        } catch(Exception ee){
            System.out.println("Error while deleting by Timestamp {}");
            ee.printStackTrace();
        }
       try{
           Mockito.doNothing().when(hbaseDataStore).createTable(any(String.class));//.thenReturn(true);
       } catch(Exception ee){
           System.out.println("Error while creating Tables");
           ee.printStackTrace();}
       Mockito.doNothing().when(hbaseDataStore).closeConnection(any(String.class));//.thenReturn(true);
       Mockito.when(schedulerService.scheduleTask((org.opendaylight.tsdr.spi.scheduler.Task) any(Task.class))).thenReturn(null);//.thenReturn(true);
       Mockito.when(future.isDone()).thenReturn(true);
       try{
           storageService.createTables();
       }catch (Exception ee) {
           System.out.println("Error while creating Tables.");
           ee.printStackTrace();
       }
    }
    @Test
    public void testStart() {
        storageService.start(10);
    }

    @Test
    public void testTriggerTableCreatingTask() {
        storageService.TriggerTableCreatingTask();
        Assert.assertNotNull(storageService.future);
    }
    @Test
    public void testFlushCommitTables() {
        String[] words = {"table1", "table2"};
        Set<String> tableNames = new HashSet<String>(Arrays.asList(words));
        storageService.flushCommit(tableNames);
    }

    @Test
    public void testStoreLog() {
        String timeStamp = (new Long((new Date()).getTime())).toString();
        List<RecordKeys> recordKeys = new ArrayList<RecordKeys>();
        RecordKeys recordKey1 = new RecordKeysBuilder()
            .setKeyName(DataCategory.SYSLOG.name())
            .setKeyValue("log1").build();
        recordKeys.add(recordKey1);
        TSDRLogRecordBuilder builder1 = new TSDRLogRecordBuilder();
        TSDRLog tsdrLog1 =   builder1.setIndex(1)
            .setRecordFullText("su root failed for lonvick")
            .setNodeID("node1.example.com")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.SYSLOG)
            .setTimeStamp(new Long(timeStamp)).build();
        storageService.store((TSDRLogRecord)tsdrLog1);
        storageService.store((TSDRLogRecord) builder1.setRecordFullText(null).build());
        storageService.store((TSDRLogRecord) builder1.setNodeID(null).build());
    }

    @Test
    public void testStoreMetric() {
        String timeStamp = (new Long((new Date()).getTime())).toString();
        List<RecordKeys> recordKeys = new ArrayList<RecordKeys>();
        RecordKeys recordKey = new RecordKeysBuilder()
            .setKeyName(TSDRConstants.FLOW_TABLE_KEY_NAME)
            .setKeyValue("table1").build();
        recordKeys.add(recordKey);
        TSDRMetricRecordBuilder builder1 = new TSDRMetricRecordBuilder();
        TSDRMetric tsdrMetric1 =   builder1.setMetricName("PacketsMatched")
            .setMetricValue(new BigDecimal(Double.parseDouble("20000000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.FLOWTABLESTATS)
            .setTimeStamp(new Long(timeStamp)).build();
        storageService.store((TSDRMetricRecord)tsdrMetric1);
        storageService.store((TSDRMetricRecord)builder1.setMetricName(null).build());
        storageService.store((TSDRMetricRecord)builder1.setNodeID(null).build());
        storageService.store((TSDRMetricRecord)builder1.setMetricValue(null).build());
    }

    @Test
    public void testGetTSDRLogRecords() {
        Boolean result = false;
        String timeStamp = (new Long((new Date()).getTime())).toString();
        List<RecordKeys> recordKeys = new ArrayList<RecordKeys>();
        RecordKeys recordKey1 = new RecordKeysBuilder()
            .setKeyName(DataCategory.SYSLOG.name())
            .setKeyValue("log1").build();
        recordKeys.add(recordKey1);
        TSDRLogRecordBuilder builder1 = new TSDRLogRecordBuilder();
        TSDRLog tsdrLog1 =   builder1.setIndex(1)
            .setRecordFullText("su root failed for lonvick")
            .setNodeID("node1.example.com")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.SYSLOG)
            .setTimeStamp(new Long(timeStamp)).build();
        storageService.store((TSDRLogRecord)tsdrLog1);
        result = storageService.getTSDRLogRecords(DataCategory.SYSLOG.name(), 0L, Long.parseLong(timeStamp)).size() == 1;
        result = storageService.getTSDRLogRecords(null, 0L, Long.parseLong(timeStamp)).size() == 0;
        result = storageService.getTSDRLogRecords("nottsdrkey", 0L, Long.parseLong(timeStamp)).size() == 0;
        result = storageService.getTSDRLogRecords("[NID=node1.example.com][DC=SYSLOG][MN=PacketsMatched][RK=SYSLOG:log1]", 0L, Long.parseLong(timeStamp)).size() == 0;
        result = storageService.getTSDRLogRecords("[NID=node1.example.com][DC=Error][MN=][RK=SYSLOG:log1]", 0L, Long.parseLong(timeStamp)).size() == 0;
        assertTrue(result);
    }

    @Test
    public void testGetTSDRMetricRecords() {
        Boolean result = false;
        String timeStamp = (new Long((new Date()).getTime())).toString();
        List<RecordKeys> recordKeys = new ArrayList<RecordKeys>();
        RecordKeys recordKey = new RecordKeysBuilder()
            .setKeyName(TSDRConstants.FLOW_TABLE_KEY_NAME)
            .setKeyValue("table1").build();
        recordKeys.add(recordKey);
        TSDRMetricRecordBuilder builder1 = new TSDRMetricRecordBuilder();
        TSDRMetric tsdrMetric1 =   builder1.setMetricName("PacketsMatched")
            .setMetricValue(new BigDecimal(Double.parseDouble("20000000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.FLOWTABLESTATS)
            .setTimeStamp(new Long(timeStamp)).build();
        storageService.store((TSDRMetricRecord)tsdrMetric1);
        result = ((storageService.getTSDRMetricRecords(DataCategory.FLOWTABLESTATS.name(),0L,Long.parseLong(timeStamp))).size() == 1);
        result = ((storageService.getTSDRMetricRecords(null ,0L,Long.parseLong(timeStamp))).size() == 0);
        result = ((storageService.getTSDRMetricRecords("nottsdrkey",0L,Long.parseLong(timeStamp))).size() == 0);
        result = ((storageService.getTSDRMetricRecords("[NID=node1][DC=FLOWTABLESTATS][MN=PacketsMatched][RK=TableID:table1]",0L,Long.parseLong(timeStamp))).size() == 0);
        result = ((storageService.getTSDRMetricRecords("[NID=node1][DC=ErrorTABLE][MN=PacketsMatched][RK=TableID:table1]",0L,Long.parseLong(timeStamp))).size() == 0);
        assertTrue(result);
    }
    @Test
    public void testpurgeTSDRRecords() {
        Boolean result = false;
        String timeStamp = (new Long((new Date()).getTime())).toString();
        List<RecordKeys> recordKeys = new ArrayList<RecordKeys>();
        RecordKeys recordKey = new RecordKeysBuilder()
            .setKeyName(TSDRConstants.FLOW_TABLE_KEY_NAME)
            .setKeyValue("table1").build();
        recordKeys.add(recordKey);
        TSDRMetricRecordBuilder builder1 = new TSDRMetricRecordBuilder();
        TSDRMetric tsdrMetric1 =   builder1.setMetricName("PacketsMatched")
            .setMetricValue(new BigDecimal(Double.parseDouble("20000000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.FLOWTABLESTATS)
            .setTimeStamp(new Long(timeStamp)).build();
        storageService.store((TSDRMetricRecord)tsdrMetric1);
        result = ((storageService.getTSDRMetricRecords(DataCategory.FLOWTABLESTATS.name(),0L,Long.parseLong(timeStamp))).size() == 1);
        storageService.purgeTSDRRecords(DataCategory.FLOWTABLESTATS,Long.parseLong(timeStamp));
        result = ((storageService.getTSDRMetricRecords(DataCategory.FLOWTABLESTATS.name(),0L,Long.parseLong(timeStamp))).size() == 0);
        assertTrue(result);
    }
    @Test
    public void testpurgeAllTSDRRecords() {
        Boolean result = false;
        String timeStamp = (new Long((new Date()).getTime())).toString();
        List<RecordKeys> recordKeys = new ArrayList<RecordKeys>();
        RecordKeys recordKey = new RecordKeysBuilder()
            .setKeyName(TSDRConstants.FLOW_TABLE_KEY_NAME)
            .setKeyValue("table1").build();
        recordKeys.add(recordKey);
        TSDRMetricRecordBuilder builder1 = new TSDRMetricRecordBuilder();
        TSDRMetric tsdrMetric1 =   builder1.setMetricName("PacketsMatched")
            .setMetricValue(new BigDecimal(Double.parseDouble("20000000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.FLOWTABLESTATS)
            .setTimeStamp(new Long(timeStamp)).build();
        storageService.store((TSDRMetricRecord)tsdrMetric1);
        result = ((storageService.getTSDRMetricRecords(DataCategory.FLOWTABLESTATS.name(),0L,Long.parseLong(timeStamp))).size() == 1);
        storageService.purgeAllTSDRRecords(Long.parseLong(timeStamp));
        result = ((storageService.getTSDRMetricRecords(DataCategory.FLOWTABLESTATS.name(),0L,Long.parseLong(timeStamp))).size() == 0);
        assertTrue(result);
    }

    @Test
    public void testStoreList() {
        Boolean result = false;
        String timeStamp = (new Long((new Date()).getTime())).toString();
        List<RecordKeys> recordKeys = new ArrayList<RecordKeys>();
        RecordKeys recordKey = new RecordKeysBuilder()
            .setKeyName(TSDRConstants.FLOW_TABLE_KEY_NAME)
            .setKeyValue("table1").build();
        recordKeys.add(recordKey);
        TSDRMetricRecordBuilder builder1 = new TSDRMetricRecordBuilder();
        TSDRMetric tsdrMetric1 =   builder1.setMetricName("PacketsMatched")
            .setMetricValue(new BigDecimal(Double.parseDouble("20000000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.FLOWTABLESTATS)
            .setTimeStamp(new Long(timeStamp)).build();
        List<TSDRRecord> recordList = new ArrayList<TSDRRecord>();
        recordList.add(tsdrMetric1);
        TSDRLogRecordBuilder builder2 = new TSDRLogRecordBuilder();
        TSDRLog tsdrLog1 =   builder2.setIndex(1)
                .setRecordFullText("su root failed for lonvick")
                .setNodeID("node1.example.com")
                .setRecordKeys(recordKeys)
                .setTSDRDataCategory(DataCategory.SYSLOG)
                .setTimeStamp(new Long(timeStamp)).build();
        recordList.add(tsdrLog1);
        storageService.store(recordList);
        result = ((storageService.getTSDRMetricRecords(DataCategory.FLOWTABLESTATS.name(),0L,Long.parseLong(timeStamp))).size() == 1);
        assertTrue(result);
        storageService.store((List<TSDRRecord>)null);
        List<TSDRRecord> recordList1 = new ArrayList<TSDRRecord>();
        storageService.store(recordList1);
        TSDRHBasePersistenceServiceImpl storageService1 = new TSDRHBasePersistenceServiceImpl(hbaseDataStore,future){@Override public HBaseEntity convertToHBaseEntity(TSDRLogRecord logRecord){return null;}};
        List<TSDRRecord> recordList2 = new ArrayList<TSDRRecord>();
        recordList.add(tsdrMetric1);
        TSDRLogRecordBuilder builder3 = new TSDRLogRecordBuilder();
        TSDRLog tsdrLog2 =   builder3.setIndex(1)
                .setRecordFullText("su root failed for lonvick")
                .setNodeID("node1.example.com")
                .setRecordKeys(recordKeys)
                .setTSDRDataCategory(DataCategory.SYSLOG)
                .setTimeStamp(new Long(timeStamp)).build();
        recordList2.add(tsdrLog2);
        storageService1.store(recordList2);
    }

    @After
    public void teardown() {
        storageService.stop(0);
        tableEntityMap.clear();
        tableEntityMap = null;
    }
}
