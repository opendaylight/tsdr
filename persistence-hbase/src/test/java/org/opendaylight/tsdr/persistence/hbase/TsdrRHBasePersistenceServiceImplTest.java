/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 * Copyright (c) 2015 xFlow Research Inc. and others.  All rights reserved
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hbase;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.apache.hadoop.hbase.TableNotFoundException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.tsdr.spi.model.TSDRConstants;
import org.opendaylight.tsdr.spi.scheduler.impl.SchedulerServiceImpl;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;

/**
 * Unit Test for HBase data store under TSDR.
 *
 * @author <a href="mailto:hariharan_sethuraman@dell.com">Hariharan Sethuraman</a>
 * @author <a href="mailto:chaudhry.usama@xflowresearch.com">Chaudhry Muhammad Usama </a>
 * @author Thomas Pantelis
 */
public class TsdrRHBasePersistenceServiceImplTest {
    private TsdrHBasePersistenceServiceImpl storageService;
    private HBaseDataStore mockDataStore;
    private final SchedulerServiceImpl schedulerService = new SchedulerServiceImpl();

    @Before
    public void setup() throws Exception {
        mockDataStore = mock(HBaseDataStore.class);

        Properties props = new Properties();
        props.setProperty(HBaseDataStoreContext.CREATE_TABLE_RETRY_INTERVAL_PROP, "1");
        HBaseDataStoreContext context = new HBaseDataStoreContext(props);

        HBaseDataStoreFactory mockDataStoreFactory = mock(HBaseDataStoreFactory.class);
        doReturn(mockDataStore).when(mockDataStoreFactory).getHBaseDataStore();
        doReturn(context).when(mockDataStoreFactory).getDataStoreContext();

        storageService = new TsdrHBasePersistenceServiceImpl(mockDataStoreFactory, schedulerService);
    }

    @After
    public void teardown() {
        schedulerService.close();
    }

    @Test
    public void testStoreLog() throws TableNotFoundException {
        final TSDRLogRecordBuilder builder = new TSDRLogRecordBuilder();
        TSDRLogRecord tsdrLog = builder.setIndex(1)
            .setRecordFullText("su root failed for lonvick")
            .setNodeID("node1.example.com")
            .setRecordKeys(Arrays.asList(new RecordKeysBuilder().setKeyName(DataCategory.SYSLOG.name())
                    .setKeyValue("log1").build()))
            .setTSDRDataCategory(DataCategory.SYSLOG)
            .setTimeStamp(Long.valueOf(System.currentTimeMillis())).build();
        storageService.storeLog(tsdrLog);

        ArgumentCaptor<HBaseEntity> entity = ArgumentCaptor.forClass(HBaseEntity.class);
        verify(mockDataStore).create(entity.capture());
        assertEquals(DataCategory.SYSLOG.name(), entity.getValue().getTableName());
        assertEquals(tsdrLog.getTimeStamp().longValue(), entity.getValue().getColumns().get(0).getTimeStamp());
        assertEquals(tsdrLog.getRecordFullText(), entity.getValue().getColumns().get(0).getValue());

        reset(mockDataStore);
        storageService.storeLog(builder.setRecordFullText(null).build());
        storageService.storeLog(builder.setNodeID(null).build());
        verifyNoMoreInteractions(mockDataStore);
    }

    @Test
    public void testStoreMetric() throws TableNotFoundException {
        final TSDRMetricRecordBuilder builder = new TSDRMetricRecordBuilder();
        TSDRMetricRecord tsdrMetric = builder.setMetricName("PacketsMatched")
            .setMetricValue(BigDecimal.valueOf(20000000))
            .setNodeID("node1")
            .setRecordKeys(Arrays.asList(new RecordKeysBuilder().setKeyName(TSDRConstants.FLOW_TABLE_KEY_NAME)
                .setKeyValue("table1").build()))
            .setTSDRDataCategory(DataCategory.FLOWTABLESTATS)
            .setTimeStamp(Long.valueOf(System.currentTimeMillis())).build();
        storageService.storeMetric(tsdrMetric);

        ArgumentCaptor<HBaseEntity> entity = ArgumentCaptor.forClass(HBaseEntity.class);
        verify(mockDataStore).create(entity.capture());
        assertEquals(DataCategory.FLOWTABLESTATS.name(), entity.getValue().getTableName());
        assertEquals(tsdrMetric.getTimeStamp().longValue(), entity.getValue().getColumns().get(0).getTimeStamp());
        assertEquals(tsdrMetric.getMetricValue().toString(), entity.getValue().getColumns().get(0).getValue());

        reset(mockDataStore);
        storageService.storeMetric(builder.setMetricName(null).build());
        storageService.storeMetric(builder.setNodeID(null).build());
        storageService.storeMetric(builder.setMetricValue(null).build());
        verifyNoMoreInteractions(mockDataStore);
    }

    @Test
    public void testRetryStoreFailure() throws IOException {
        TSDRMetricRecord tsdrMetric = new TSDRMetricRecordBuilder().setMetricName("PacketsMatched")
            .setMetricValue(BigDecimal.valueOf(20000000))
            .setNodeID("node1")
            .setRecordKeys(Arrays.asList(new RecordKeysBuilder().setKeyName(TSDRConstants.FLOW_TABLE_KEY_NAME)
                .setKeyValue("table1").build()))
            .setTSDRDataCategory(DataCategory.FLOWTABLESTATS)
            .setTimeStamp(Long.valueOf(System.currentTimeMillis())).build();

        storageService.storeMetric(tsdrMetric);

        for (String tableName : HBasePersistenceUtil.getTsdrHBaseTables()) {
            verify(mockDataStore).createTable(tableName);
        }

        reset(mockDataStore);
        doThrow(new TableNotFoundException()).doReturn(null).when(mockDataStore).create(any(HBaseEntity.class));

        storageService.storeMetric(tsdrMetric);

        for (String tableName : HBasePersistenceUtil.getTsdrHBaseTables()) {
            verify(mockDataStore).createTable(tableName);
        }

        verify(mockDataStore, times(2)).create(any(HBaseEntity.class));
    }

    @Test
    public void testGetTSDRLogRecords() throws TableNotFoundException {
        final long timeStamp = System.currentTimeMillis();
        TSDRLogRecord tsdrLog = new TSDRLogRecordBuilder().setIndex(-1)
            .setRecordFullText("su root failed for lonvick")
            .setNodeID("node1.example.com")
            .setRecordKeys(Arrays.asList(new RecordKeysBuilder().setKeyName(DataCategory.SYSLOG.name())
                    .setKeyValue("log1").build()))
            .setTSDRDataCategory(DataCategory.SYSLOG)
            .setTimeStamp(Long.valueOf(timeStamp)).build();
        storageService.storeLog(tsdrLog);

        ArgumentCaptor<HBaseEntity> entity = ArgumentCaptor.forClass(HBaseEntity.class);
        verify(mockDataStore).create(entity.capture());

        doReturn(Arrays.asList(entity.getValue())).when(mockDataStore).getDataByTimeRange(
                DataCategory.FLOWTABLESTATS.name(), 0L, timeStamp);

        final List<TSDRLogRecord> records = storageService.getTSDRLogRecords(
                DataCategory.FLOWTABLESTATS.name(), 0L, timeStamp);
        assertEquals(1, records.size());
        assertEquals(tsdrLog, records.get(0));

        assertEquals(0, storageService.getTSDRLogRecords(null, 0L, timeStamp).size());
        assertEquals(0, storageService.getTSDRLogRecords("nottsdrkey", 0L, timeStamp).size());
        assertEquals(0, storageService.getTSDRLogRecords(
                "[NID=node1.example.com][DC=SYSLOG][MN=PacketsMatched][RK=SYSLOG:log1]", 0L,timeStamp).size());
        assertEquals(0, storageService.getTSDRLogRecords("[NID=node1.example.com][DC=Error][MN=][RK=SYSLOG:log1]", 0L,
                timeStamp).size());
    }

    @Test
    public void testGetTSDRMetricRecords() throws TableNotFoundException {
        final long timeStamp = System.currentTimeMillis();
        TSDRMetricRecord tsdrMetric = new TSDRMetricRecordBuilder().setMetricName("PacketsMatched")
            .setMetricValue(new BigDecimal(Double.parseDouble("20000000")))
            .setNodeID("node1")
            .setRecordKeys(Arrays.asList(new RecordKeysBuilder().setKeyName(TSDRConstants.FLOW_TABLE_KEY_NAME)
                .setKeyValue("table1").build()))
            .setTSDRDataCategory(DataCategory.FLOWTABLESTATS)
            .setTimeStamp(Long.valueOf(timeStamp)).build();
        storageService.storeMetric(tsdrMetric);

        ArgumentCaptor<HBaseEntity> entity = ArgumentCaptor.forClass(HBaseEntity.class);
        verify(mockDataStore).create(entity.capture());

        doReturn(Arrays.asList(entity.getValue())).when(mockDataStore).getDataByTimeRange(
                DataCategory.FLOWTABLESTATS.name(), 0L, timeStamp);

        final List<TSDRMetricRecord> records = storageService.getTSDRMetricRecords(
                DataCategory.FLOWTABLESTATS.name(), 0L, timeStamp);
        assertEquals(1, records.size());
        assertEquals(tsdrMetric, records.get(0));

        assertEquals(0, storageService.getTSDRMetricRecords(null, 0L, timeStamp).size());
        assertEquals(0, storageService.getTSDRMetricRecords("nottsdrkey", 0L, timeStamp).size());
        assertEquals(0, storageService.getTSDRMetricRecords(
                "[NID=node1][DC=FLOWTABLESTATS][MN=PacketsMatched][RK=TableID:table1]", 0L, timeStamp).size());
        assertEquals(0, storageService.getTSDRMetricRecords(
                "[NID=node1][DC=ErrorTABLE][MN=PacketsMatched][RK=TableID:table1]", 0L, timeStamp).size());
    }

    @Test
    public void testPurgeCategory() throws IOException {
        final long timeStamp = System.currentTimeMillis();
        storageService.purge(DataCategory.FLOWTABLESTATS, timeStamp);

        verify(mockDataStore).deleteByTimestamp(DataCategory.FLOWTABLESTATS.name(), timeStamp);
    }

    @Test
    public void testPurgeAllCategories() throws IOException {
        final long timeStamp = System.currentTimeMillis();
        storageService.purge(timeStamp);

        for (DataCategory category : DataCategory.values()) {
            verify(mockDataStore).deleteByTimestamp(category.name(), timeStamp);
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testStoreMetricList() throws TableNotFoundException {
        TSDRMetricRecord tsdrMetric1 = new TSDRMetricRecordBuilder().setMetricName("Metric1")
            .setMetricValue(BigDecimal.valueOf(1)).setNodeID("node1")
            .setRecordKeys(Arrays.asList(new RecordKeysBuilder().setKeyName(TSDRConstants.FLOW_TABLE_KEY_NAME)
                .setKeyValue("table1").build()))
            .setTSDRDataCategory(DataCategory.FLOWTABLESTATS).setTimeStamp(Long.valueOf(1)).build();

        TSDRMetricRecord tsdrMetric2 = new TSDRMetricRecordBuilder().setMetricName("Metric2")
                .setMetricValue(BigDecimal.valueOf(2)).setNodeID("node1")
                .setRecordKeys(Arrays.asList(new RecordKeysBuilder().setKeyName(TSDRConstants.FLOW_TABLE_KEY_NAME)
                    .setKeyValue("table1").build()))
                .setTSDRDataCategory(DataCategory.FLOWTABLESTATS).setTimeStamp(Long.valueOf(2)).build();

        storageService.storeMetric(Arrays.asList(tsdrMetric1, tsdrMetric2));

        ArgumentCaptor<List> entitiesCaptor = ArgumentCaptor.forClass(List.class);
        verify(mockDataStore).create(entitiesCaptor.capture());
        final List<HBaseEntity> entities = entitiesCaptor.getValue();
        assertEquals(2, entities.size());
        assertEquals(DataCategory.FLOWTABLESTATS.name(), entities.get(0).getTableName());
        assertEquals(tsdrMetric1.getTimeStamp().longValue(), entities.get(0).getColumns().get(0).getTimeStamp());
        assertEquals(tsdrMetric1.getMetricValue().toString(), entities.get(0).getColumns().get(0).getValue());

        assertEquals(DataCategory.FLOWTABLESTATS.name(), entities.get(1).getTableName());
        assertEquals(tsdrMetric2.getTimeStamp().longValue(), entities.get(1).getColumns().get(0).getTimeStamp());
        assertEquals(tsdrMetric2.getMetricValue().toString(), entities.get(1).getColumns().get(0).getValue());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testStoreLogList() throws TableNotFoundException {
        TSDRLogRecord tsdrLog1 = new TSDRLogRecordBuilder().setIndex(1)
            .setRecordFullText("log1").setNodeID("node1")
            .setRecordKeys(Arrays.asList(new RecordKeysBuilder().setKeyName(DataCategory.SYSLOG.name())
                    .setKeyValue("log1").build()))
            .setTSDRDataCategory(DataCategory.SYSLOG).setTimeStamp(Long.valueOf(1)).build();

        TSDRLogRecord tsdrLog2 = new TSDRLogRecordBuilder().setIndex(1)
                .setRecordFullText("log1").setNodeID("node1")
                .setRecordKeys(Arrays.asList(new RecordKeysBuilder().setKeyName(DataCategory.SYSLOG.name())
                        .setKeyValue("log1").build()))
                .setTSDRDataCategory(DataCategory.SYSLOG).setTimeStamp(Long.valueOf(2)).build();

        storageService.storeLog(Arrays.asList(tsdrLog1, tsdrLog2));

        ArgumentCaptor<List> entitiesCaptor = ArgumentCaptor.forClass(List.class);
        verify(mockDataStore).create(entitiesCaptor.capture());
        final List<HBaseEntity> entities = entitiesCaptor.getValue();
        assertEquals(2, entities.size());
        assertEquals(DataCategory.SYSLOG.name(), entities.get(0).getTableName());
        assertEquals(tsdrLog1.getTimeStamp().longValue(), entities.get(0).getColumns().get(0).getTimeStamp());
        assertEquals(tsdrLog1.getRecordFullText(), entities.get(0).getColumns().get(0).getValue());

        assertEquals(DataCategory.SYSLOG.name(), entities.get(1).getTableName());
        assertEquals(tsdrLog2.getTimeStamp().longValue(), entities.get(1).getColumns().get(0).getTimeStamp());
        assertEquals(tsdrLog2.getRecordFullText(), entities.get(1).getColumns().get(0).getValue());
    }

    @Test
    public void testClose() {
        storageService.close();

        for (String tableName : HBasePersistenceUtil.getTsdrHBaseTables()) {
            verify(mockDataStore).closeConnection(tableName);
        }
    }
}
