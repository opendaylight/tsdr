/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 * Copyright (c) 2015 xFlow Research Inc. and others.  All rights reserved
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hbase;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.util.concurrent.ListenableScheduledFuture;
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.tsdr.spi.model.TSDRConstants;
import org.opendaylight.tsdr.spi.scheduler.SchedulerService;
import org.opendaylight.tsdr.spi.scheduler.Task;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.TSDRLog;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.TSDRMetric;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;

/**
 * Unit Test for HBase data store under TSDR.
 *
 * @author <a href="mailto:hariharan_sethuraman@dell.com">Hariharan Sethuraman</a>
 * @author <a href="mailto:chaudhry.usama@xflowresearch.com">Chaudhry Muhammad Usama </a>
 */
public class TsdrRHBasePersistenceServiceImplTest {
    private TsdrHBasePersistenceServiceImpl storageService;
    private HBaseDataStore hbaseDataStore;
    private SchedulerService schedulerService;
    private static Map<String, Map<String,List<HBaseEntity>>> tableEntityMap;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Before
    public void setup() throws Exception {
        hbaseDataStore = mock(HBaseDataStore.class);
        HBaseDataStoreFactory.setHBaseDataStoreIfAbsent(hbaseDataStore);

        ListenableScheduledFuture scheduledFuture = mock(ListenableScheduledFuture.class);
        doReturn(true).when(scheduledFuture).isDone();

        schedulerService = mock(SchedulerService.class);
        doReturn(scheduledFuture).when(schedulerService).scheduleTask(any(Task.class));

        storageService = new TsdrHBasePersistenceServiceImpl(schedulerService);
        tableEntityMap = new HashMap<>();
        doAnswer(invocation -> {
            Object[] arguments = invocation.getArguments();
            if (arguments != null && arguments.length > 0 && arguments[0] != null) {
                HBaseEntity entity = (HBaseEntity) arguments[0];
                if (!tableEntityMap.containsKey(entity.getTableName())) {
                    tableEntityMap.put(entity.getTableName(), new TreeMap<String, List<HBaseEntity>>());
                }
                Map<String, List<HBaseEntity>> entityMap = tableEntityMap.get(entity.getTableName());
                if (!entityMap.containsKey(entity.getRowKey())) {
                    entityMap.put(entity.getRowKey(), new ArrayList<HBaseEntity>());
                }
                List<HBaseEntity> entitiesCol = entityMap.get(entity.getRowKey());
                entitiesCol.add(entity);
                return entity;
            }
            return null;
        }).when(hbaseDataStore).create(any(HBaseEntity.class));

        /*
         * Mocking up the create a list of rows in HTable
         * List<HBaseEntity> create(List<HBaseEntity> entityList)
         * @param entityList - a list of objects of HBaseEntity.
         */
        doAnswer(invocation -> {
            Object[] arguments = invocation.getArguments();
            if (arguments != null && arguments.length > 0 && arguments[0] != null) {
                List<HBaseEntity> entityList = (List<HBaseEntity>) arguments[0];
                for (HBaseEntity entity : entityList) {
                    if (!tableEntityMap.containsKey(entity.getTableName())) {
                        tableEntityMap.put(entity.getTableName(), new TreeMap<String, List<HBaseEntity>>());
                    }
                    Map<String, List<HBaseEntity>> entityMap = tableEntityMap.get(entity.getTableName());
                    if (!entityMap.containsKey(entity.getRowKey())) {
                        entityMap.put(entity.getRowKey(), new ArrayList<HBaseEntity>());
                    }
                    List<HBaseEntity> entitiesCol = entityMap.get(entity.getRowKey());
                    entitiesCol.add(entity);
                }
                return null;
            }
            return null;
        }).when(hbaseDataStore).create(any(ArrayList.class));

        doAnswer(invocation -> {
            Object[] arguments = invocation.getArguments();
            List<HBaseEntity> entityCol = new ArrayList<>();
            if (arguments != null && arguments.length > 0 && arguments[0] != null) {
                String tableName = (String) arguments[0];
                Long startTime = (Long) arguments[1];
                Long endTime = (Long) arguments[2];
                Map<String, List<HBaseEntity>> entityMap = tableEntityMap.get(tableName);
                if (entityMap == null) {
                    return entityCol;
                }
                for (List<HBaseEntity> entityValues : entityMap.values()) {
                    for (HBaseEntity entity : entityValues) {
                        for (HBaseColumn currentColumn : entity.getColumns()) {
                            if (currentColumn.getTimeStamp() >= startTime && currentColumn.getTimeStamp() <= endTime) {
                                entityCol.add(entity);
                                break;
                            }
                        }
                    }
                }
            }
            return entityCol;
        }).when(hbaseDataStore).getDataByTimeRange(any(String.class), any(Long.class), any(Long.class));

        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            List<HBaseEntity> deletelist = new ArrayList<>();
            if (args != null && args.length > 0 && args[0] != null) {
                String tableName = (String) args[0];
                Long retentionTime = (Long) args[1];
                Map<String, List<HBaseEntity>> entityMap = tableEntityMap.get(tableName);
                if (entityMap == null) {
                    return null;
                }
                for (List<HBaseEntity> entityValues : entityMap.values()) {
                    for (HBaseEntity entity1 : entityValues) {
                        for (HBaseColumn currentColumn : entity1.getColumns()) {
                            if (currentColumn.getTimeStamp() <= retentionTime) {
                                deletelist.add(entity1);
                            }
                        }
                    }
                }
                for (HBaseEntity entity2 : deletelist) {
                    entityMap.remove(entity2.getRowKey());
                }
            }
            return null;
        }).when(hbaseDataStore).deleteByTimestamp(any(String.class), any(Long.class));

        Mockito.doNothing().when(hbaseDataStore).createTable(any(String.class));//.thenReturn(true);

        Mockito.doNothing().when(hbaseDataStore).closeConnection(any(String.class));//.thenReturn(true);

        storageService.createTables();
    }

    @Test
    public void testFlushCommitTables() {
        String[] words = {"table1", "table2"};
        Set<String> tableNames = new HashSet<>(Arrays.asList(words));
        storageService.flushCommit(tableNames);
    }

    @Test
    public void testStoreLog() {
        String timeStamp = new Long(new Date().getTime()).toString();
        List<RecordKeys> recordKeys = new ArrayList<>();
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
        storageService.storeLog((TSDRLogRecord) tsdrLog1);
        storageService.storeLog(builder1.setRecordFullText(null).build());
        storageService.storeLog(builder1.setNodeID(null).build());
    }

    @Test
    public void testStoreMetric() {
        String timeStamp = new Long(new Date().getTime()).toString();
        List<RecordKeys> recordKeys = new ArrayList<>();
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
        storageService.storeMetric((TSDRMetricRecord) tsdrMetric1);
        storageService.storeMetric(builder1.setMetricName(null).build());
        storageService.storeMetric(builder1.setNodeID(null).build());
        storageService.storeMetric(builder1.setMetricValue(null).build());
    }

    @Test
    public void testGetTSDRLogRecords() {
        Boolean result = false;
        String timeStamp = new Long(new Date().getTime()).toString();
        List<RecordKeys> recordKeys = new ArrayList<>();
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
        storageService.storeLog((TSDRLogRecord)tsdrLog1);
        result = storageService.getTSDRLogRecords(DataCategory.SYSLOG.name(), 0L, Long.parseLong(timeStamp))
                .size() == 1;
        result = storageService.getTSDRLogRecords(null, 0L, Long.parseLong(timeStamp)).size() == 0;
        result = storageService.getTSDRLogRecords("nottsdrkey", 0L, Long.parseLong(timeStamp)).size() == 0;
        result = storageService
                .getTSDRLogRecords("[NID=node1.example.com][DC=SYSLOG][MN=PacketsMatched][RK=SYSLOG:log1]", 0L,
                        Long.parseLong(timeStamp))
                .size() == 0;
        result = storageService.getTSDRLogRecords("[NID=node1.example.com][DC=Error][MN=][RK=SYSLOG:log1]", 0L,
                Long.parseLong(timeStamp)).size() == 0;
        assertTrue(result);
    }

    @Test
    public void testGetTSDRMetricRecords() {
        Boolean result = false;
        String timeStamp = new Long(new Date().getTime()).toString();
        List<RecordKeys> recordKeys = new ArrayList<>();
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
        storageService.storeMetric((TSDRMetricRecord)tsdrMetric1);
        result = storageService.getTSDRMetricRecords(DataCategory.FLOWTABLESTATS.name(), 0L, Long.parseLong(timeStamp))
                .size() == 1;
        result = storageService.getTSDRMetricRecords(null, 0L, Long.parseLong(timeStamp)).size() == 0;
        result = storageService.getTSDRMetricRecords("nottsdrkey", 0L, Long.parseLong(timeStamp)).size() == 0;
        result = storageService
                .getTSDRMetricRecords("[NID=node1][DC=FLOWTABLESTATS][MN=PacketsMatched][RK=TableID:table1]", 0L,
                        Long.parseLong(timeStamp))
                .size() == 0;
        result = storageService.getTSDRMetricRecords("[NID=node1][DC=ErrorTABLE][MN=PacketsMatched][RK=TableID:table1]",
                0L, Long.parseLong(timeStamp)).size() == 0;
        assertTrue(result);
    }

    @Test
    public void testpurgeTSDRRecords() {
        Boolean result = false;
        String timeStamp = new Long(new Date().getTime()).toString();
        List<RecordKeys> recordKeys = new ArrayList<>();
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
        storageService.storeMetric((TSDRMetricRecord)tsdrMetric1);
        result = storageService.getTSDRMetricRecords(DataCategory.FLOWTABLESTATS.name(), 0L, Long.parseLong(timeStamp))
                .size() == 1;
        storageService.purge(DataCategory.FLOWTABLESTATS, Long.parseLong(timeStamp));
        result = storageService.getTSDRMetricRecords(DataCategory.FLOWTABLESTATS.name(), 0L, Long.parseLong(timeStamp))
                .size() == 0;
        assertTrue(result);
    }

    @Test
    public void testpurgeAllTSDRRecords() {
        Boolean result = false;
        String timeStamp = new Long(new Date().getTime()).toString();
        List<RecordKeys> recordKeys = new ArrayList<>();
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
        storageService.storeMetric((TSDRMetricRecord) tsdrMetric1);
        result = storageService.getTSDRMetricRecords(DataCategory.FLOWTABLESTATS.name(), 0L, Long.parseLong(timeStamp))
                .size() == 1;
        storageService.purge(Long.parseLong(timeStamp));
        result = storageService.getTSDRMetricRecords(DataCategory.FLOWTABLESTATS.name(), 0L, Long.parseLong(timeStamp))
                .size() == 0;
        assertTrue(result);
    }

    @Test
    public void testStoreList() {
        String timeStamp = new Long(new Date().getTime()).toString();
        List<RecordKeys> recordKeys = new ArrayList<>();
        RecordKeys recordKey = new RecordKeysBuilder()
            .setKeyName(TSDRConstants.FLOW_TABLE_KEY_NAME)
            .setKeyValue("table1").build();
        recordKeys.add(recordKey);
        TSDRMetricRecordBuilder builder1 = new TSDRMetricRecordBuilder();
        TSDRMetricRecord tsdrMetric1 =   builder1.setMetricName("PacketsMatched")
            .setMetricValue(new BigDecimal(Double.parseDouble("20000000")))
            .setNodeID("node1")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.FLOWTABLESTATS)
            .setTimeStamp(new Long(timeStamp)).build();
        List<TSDRMetricRecord> recordList = new ArrayList<>();
        List<TSDRLogRecord> recordListLog = new ArrayList<>();
        recordList.add(tsdrMetric1);
        TSDRLogRecordBuilder builder2 = new TSDRLogRecordBuilder();
        TSDRLogRecord tsdrLog1 =   builder2.setIndex(1)
                .setRecordFullText("su root failed for lonvick")
                .setNodeID("node1.example.com")
                .setRecordKeys(recordKeys)
                .setTSDRDataCategory(DataCategory.SYSLOG)
                .setTimeStamp(new Long(timeStamp)).build();

        recordListLog.add(tsdrLog1);
        storageService.storeMetric(recordList);
        storageService.storeLog(recordListLog);
        boolean result = storageService.getTSDRMetricRecords(DataCategory.FLOWTABLESTATS.name(), 0L,
                Long.parseLong(timeStamp)).size() == 1;
        assertTrue(result);
        storageService.storeMetric((List<TSDRMetricRecord>) null);
        List<TSDRLogRecord> recordList1 = new ArrayList<>();
        storageService.storeLog(recordList1);
        TsdrHBasePersistenceServiceImpl storageService1 = new TsdrHBasePersistenceServiceImpl(schedulerService) {
            @Override
            public HBaseEntity convertToHBaseEntity(TSDRLogRecord logRecord) {
                return null;
            }
        };
        List<TSDRLogRecord> recordList2 = new ArrayList<>();
        recordList.add(tsdrMetric1);
        TSDRLogRecordBuilder builder3 = new TSDRLogRecordBuilder();
        TSDRLogRecord tsdrLog2 =   builder3.setIndex(1)
                .setRecordFullText("su root failed for lonvick")
                .setNodeID("node1.example.com")
                .setRecordKeys(recordKeys)
                .setTSDRDataCategory(DataCategory.SYSLOG)
                .setTimeStamp(new Long(timeStamp)).build();
        recordList2.add(tsdrLog2);
        storageService1.storeLog(recordList2);
    }

    @After
    public void teardown() {
        storageService.stop(0);
        tableEntityMap.clear();
        tableEntityMap = null;
    }
}
