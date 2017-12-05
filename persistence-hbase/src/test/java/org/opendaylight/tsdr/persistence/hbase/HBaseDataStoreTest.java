/*
 * Copyright (c) 2016 xFlow Research Inc. and others.  All rights reserved
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hbase;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableNotFoundException;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.eclipse.jdt.internal.core.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Unit tests for HBaseDataStore.
 *
 * @author <a href="mailto:chaudhry.usama@xflowresearch.com">Chaudhry Muhammad Usama</a>
 */
public class HBaseDataStoreTest {
    private HBaseDataStore hbaseDataStore;
    private HBaseDataStoreContext hbaseDataStoreContext;
    private HTablePool htablePool;
    private HTableInterface htableInterface;
    private ResultScanner resultScanner;
    private Configuration conf;
    private Map<String, HTableInterface> htableMap;
    private Result result;
    private HBaseAdmin hbase;
    private NavigableMap nmap;
    private HTable htable;

    @Before
    public void setup() throws IOException {
        nmap = mock(NavigableMap.class);
        hbaseDataStore = new HBaseDataStore();
        conf = mock(Configuration.class);
        htable = mock(HTable.class);
        hbaseDataStoreContext = mock(HBaseDataStoreContext.class);
        htablePool = mock(HTablePool.class);
        htableInterface = mock(HTableInterface.class);
        resultScanner = mock(ResultScanner.class);
        htableMap = mock(Map.class);
        result = mock(Result.class);
        hbase = mock(HBaseAdmin.class);
        hbaseDataStore = new HBaseDataStore(hbaseDataStoreContext) {
            @Override
            public HTablePool getHTablePool() {
                return htablePool;
            }

            @Override
            public HBaseEntity convertResultToEntity(String tableName, Result result) {
                HBaseEntity dentity = new HBaseEntity();
                dentity.setRowKey("rowKey1");
                dentity.setTableName("tableName1");
                HBaseColumn column = new HBaseColumn();
                column.setColumnFamily(TsdrHBaseDataStoreConstants.COLUMN_FAMILY_NAME);
                column.setColumnQualifier(TsdrHBaseDataStoreConstants.COLUMN_QUALIFIER_NAME);
                column.setTimeStamp(1000);
                column.setValue("metricValue");
                List<HBaseColumn> columnList1 = new ArrayList<>();
                columnList1.add(column);
                dentity.setColumns(columnList1);
                return dentity;
            }

            @Override
            public HBaseAdmin getNewHBaseAdmin() {
                return hbase;
            }
        };

        hbaseDataStore.setHBaseDataStoreHtableMap(htableMap);

        doAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            return null;
        }).when(htableInterface).getTableName();

        Mockito.doNothing().when(htableInterface).close();

        Mockito.doNothing().when(htableInterface).delete(any(List.class));

        doReturn("".getBytes()).when(result).getRow();
        doReturn(nmap).when(result).getMap();
        doReturn(htableInterface).when(htableMap).get(any(String.class));
        doReturn(htableInterface).when(htablePool).getTable(any(String.class));
        doReturn(resultScanner).when(htableInterface).getScanner(any(Scan.class));

        Mockito.when(resultScanner.next()).thenReturn(result).thenReturn(null).thenReturn(result)
            .thenReturn(null).thenReturn(result).thenReturn(null).thenReturn(result).thenReturn(null)
            .thenReturn(result).thenReturn(null).thenReturn(result).thenReturn(null).thenReturn(result)
            .thenReturn(null).thenReturn(result).thenReturn(null).thenReturn(result).thenReturn(null)
            .thenReturn(result).thenReturn(null);

        Mockito.doNothing().when(htablePool).putTable(any(HTableInterface.class));

        Mockito.doNothing().when(htableInterface).put(any(Put.class));
    }

    @Test
    public void testGetConnection() throws IOException {
        hbaseDataStore.getConnection("tableName");
    }

    @Test
    public void testCreateTable() throws Exception {
        hbaseDataStore.createTable(null);
        hbaseDataStore.createTable("tableName");
    }

    @Test
    public void testCloseConnection() throws Exception {
        hbaseDataStore.closeConnection((HTable) null);
        hbaseDataStore.closeConnection((String) null);
        hbaseDataStore.closeConnection(htable);
        hbaseDataStore.closeConnection("tableName");
    }

    @Test
    public void testFlushCommit() throws Exception {
        hbaseDataStore.flushCommit("tableName");
        hbaseDataStore.flushCommit(null);
    }

    @Test
    public void testGetDataByRowFamilyQualifier() throws Exception {
        Assert.isNotNull(
                hbaseDataStore.getDataByRowFamilyQualifier("tableName", "startRow", "endRow", "family", "qualifier"));
        Assert.isNotNull(
                hbaseDataStore.getDataByRowFamilyQualifier("tableName", "startRow", "endRow", "family", (String) null));
        Assert.isNotNull(hbaseDataStore.getDataByRowFamilyQualifier("tableName", "startRow", "endRow", "family",
                "qualifier", 10));
        List<String> qualifierList = new ArrayList<>();
        Assert.isNotNull(
                hbaseDataStore.getDataByRowFamilyQualifier("tableName", "startRow", "endRow", "family", qualifierList));
        qualifierList.add("qualifier1");
        qualifierList.add("qualifier2");
        Assert.isNotNull(
                hbaseDataStore.getDataByRowFamilyQualifier("tableName", "startRow", "endRow", "family", qualifierList));
        Assert.isNotNull(hbaseDataStore.getDataByRowFamilyQualifier("tableName", "startRow", "endRow", "family",
                qualifierList, 10));
    }

    @Test
    public void testGetDataByTimeRange() throws Exception {
        hbaseDataStore.getDataByTimeRange("tableName", 0L, 100);
        hbaseDataStore.getDataByTimeRange("tableName", 100, 200);
        List<String> filters = new ArrayList<>();
        filters.add("filter1");
        filters.add("filter2");
        hbaseDataStore.getDataByTimeRange("tableName", filters, 0L, 100);
    }

    @Test
    public void testCreateEntity() throws Exception {
        HBaseEntity dentity = new HBaseEntity();
        dentity.setRowKey("rowKey1");
        dentity.setTableName("tableName1");
        HBaseColumn column = new HBaseColumn();
        column.setColumnFamily(TsdrHBaseDataStoreConstants.COLUMN_FAMILY_NAME);
        column.setColumnQualifier(TsdrHBaseDataStoreConstants.COLUMN_QUALIFIER_NAME);
        column.setTimeStamp(1000);
        column.setValue("metricValue");
        List<HBaseColumn> columnList1 = new ArrayList<>();
        columnList1.add(column);
        dentity.setColumns(columnList1);
        hbaseDataStore.create(dentity);
        column.setTimeStamp(0);
        List<HBaseColumn> columnList2 = new ArrayList<>();
        columnList2.add(column);
        dentity.setColumns(columnList2);
        hbaseDataStore.create(dentity);
        column.setColumnQualifier(null);
        List<HBaseColumn> columnList3 = new ArrayList<>();
        columnList3.add(column);
        dentity.setColumns(columnList3);
        hbaseDataStore.create(dentity);
        column.setTimeStamp(1000);
        List<HBaseColumn> columnList4 = new ArrayList<>();
        columnList4.add(column);
        dentity.setColumns(columnList4);
        hbaseDataStore.create(dentity);
    }

    @Test
    public void testCreateEntityList() throws TableNotFoundException {
        List<HBaseEntity> entityList = new ArrayList<>();
        hbaseDataStore.create(entityList);
        HBaseEntity dentity1 = new HBaseEntity();
        dentity1.setRowKey("rowKey1");
        dentity1.setTableName("tableName1");
        HBaseColumn column1 = new HBaseColumn();
        column1.setColumnFamily(TsdrHBaseDataStoreConstants.COLUMN_FAMILY_NAME);
        column1.setColumnQualifier(TsdrHBaseDataStoreConstants.COLUMN_QUALIFIER_NAME);
        column1.setTimeStamp(1000);
        column1.setValue("metricValue");
        List<HBaseColumn> columnList1 = new ArrayList<>();
        columnList1.add(column1);
        dentity1.setColumns(columnList1);
        entityList.add(dentity1);
        HBaseEntity dentity2 = new HBaseEntity();
        dentity2.setRowKey("rowKey1");
        dentity2.setTableName("tableName1");
        HBaseColumn column2 = new HBaseColumn();
        column2.setColumnFamily(TsdrHBaseDataStoreConstants.COLUMN_FAMILY_NAME);
        column2.setColumnQualifier(TsdrHBaseDataStoreConstants.COLUMN_QUALIFIER_NAME);
        column2.setTimeStamp(0);
        column2.setValue("metricValue");
        List<HBaseColumn> columnList2 = new ArrayList<>();
        columnList2.add(column2);
        dentity2.setColumns(columnList2);
        entityList.add(dentity2);
        HBaseEntity dentity3 = new HBaseEntity();
        dentity3.setRowKey("rowKey1");
        dentity3.setTableName("tableName1");
        HBaseColumn column3 = new HBaseColumn();
        column3.setColumnFamily(TsdrHBaseDataStoreConstants.COLUMN_FAMILY_NAME);
        column3.setColumnQualifier(null);
        column3.setTimeStamp(100);
        column3.setValue("metricValue");
        List<HBaseColumn> columnList3 = new ArrayList<>();
        columnList3.add(column3);
        dentity3.setColumns(columnList3);
        entityList.add(dentity3);
        HBaseEntity dentity4 = new HBaseEntity();
        dentity4.setRowKey("rowKey1");
        dentity4.setTableName("tableName1");
        HBaseColumn column4 = new HBaseColumn();
        column4.setColumnFamily(TsdrHBaseDataStoreConstants.COLUMN_FAMILY_NAME);
        column4.setColumnQualifier(null);
        column4.setTimeStamp(0);
        column4.setValue("metricValue");
        List<HBaseColumn> columnList4 = new ArrayList<>();
        columnList4.add(column4);
        dentity4.setColumns(columnList4);
        entityList.add(dentity4);
        hbaseDataStore.create(entityList);
    }

    @Test
    public void testDeleteByTimestamp() throws Exception {
        hbaseDataStore.deleteByTimestamp("tableName", 0L);
    }

    @Test
    public void testConvertResultToEntity() {
        HBaseDataStore hbasedatastore = new HBaseDataStore();
        hbasedatastore.convertResultToEntity("tableName", (Result) null);
        hbasedatastore.convertResultToEntity("tableName", result);
    }

    @Test(expected = TableNotFoundException.class)
    public void testDeleteByTimestampException() throws IOException {
        HBaseDataStore hbasedatastore1 = null;
        hbasedatastore1 = new HBaseDataStore() {
            @Override
            public HTableInterface getConnection(String tableName) throws IOException {
                throw new IOException();
            }
        };

        hbasedatastore1.deleteByTimestamp("tableName", 0L);

        hbasedatastore1 = new HBaseDataStore() {
            @Override
            public HTableInterface getConnection(String tableName) throws IOException {
                throw new IOException();
            }
        };

        hbasedatastore1.deleteByTimestamp("tableName", 0L);

        hbasedatastore1 = new HBaseDataStore() {
            @Override
            public HTableInterface getConnection(String tableName) throws IOException {
                throw new TableNotFoundException();
            }
        };

        hbasedatastore1.deleteByTimestamp("tableName", 0L);
    }

    @Test
    public void testGetDataByTimeRangeException() {
        HBaseDataStore hbasedatastore1 = null;
        hbasedatastore1 = new HBaseDataStore() {
            @Override
            public HTableInterface getConnection(String tableName) throws IOException {
                throw new IOException();
            }
        };

        hbasedatastore1.getDataByTimeRange("tableName", 100, 200);

        hbasedatastore1 = new HBaseDataStore() {
            @Override
            public HTableInterface getConnection(String tableName) throws IOException {
                throw new IOException();
            }
        };

        hbasedatastore1.getDataByTimeRange("tableName", 100, 200);
    }

    @Test(expected = TableNotFoundException.class)
    public void testCreateEntityException() throws TableNotFoundException {
        HBaseDataStore hbasedatastore1 = null;
        HBaseEntity dentity = new HBaseEntity();
        dentity.setRowKey("rowKey1");
        dentity.setTableName("tableName1");
        HBaseColumn column = new HBaseColumn();
        column.setColumnFamily(TsdrHBaseDataStoreConstants.COLUMN_FAMILY_NAME);
        column.setColumnQualifier(TsdrHBaseDataStoreConstants.COLUMN_QUALIFIER_NAME);
        column.setTimeStamp(1000);
        column.setValue("metricValue");
        List<HBaseColumn> columnList1 = new ArrayList<>();
        columnList1.add(column);
        dentity.setColumns(columnList1);

        hbasedatastore1 = new HBaseDataStore() {
            @Override
            public HTableInterface getConnection(String tableName) throws IOException {
                throw new IOException();
            }
        };

        hbasedatastore1.create(dentity);
        hbasedatastore1 = new HBaseDataStore() {
            @Override
            public HTableInterface getConnection(String tableName) throws IOException {
                throw new IOException();
            }
        };

        hbasedatastore1.create(dentity);

        hbasedatastore1 = new HBaseDataStore() {
            @Override
            public HTableInterface getConnection(String tableName) throws IOException {
                throw new TableNotFoundException();
            }
        };

        hbasedatastore1.create(dentity);
    }

    @Test(expected = TableNotFoundException.class)
    public void testCreateEntityListException() throws TableNotFoundException {
        HBaseDataStore hbasedatastore1 = null;
        HBaseEntity dentity = new HBaseEntity();
        dentity.setRowKey("rowKey1");
        dentity.setTableName("tableName1");
        HBaseColumn column = new HBaseColumn();
        column.setColumnFamily(TsdrHBaseDataStoreConstants.COLUMN_FAMILY_NAME);
        column.setColumnQualifier(TsdrHBaseDataStoreConstants.COLUMN_QUALIFIER_NAME);
        column.setTimeStamp(1000);
        column.setValue("metricValue");
        List<HBaseColumn> columnList1 = new ArrayList<>();
        columnList1.add(column);
        dentity.setColumns(columnList1);
        List<HBaseEntity> entityList = new ArrayList<>();
        entityList.add(dentity);

        hbasedatastore1 = new HBaseDataStore() {
            @Override
            public HTableInterface getConnection(String tableName) throws IOException {
                throw new IOException();
            }
        };

        hbasedatastore1.create(entityList);

        hbasedatastore1 = new HBaseDataStore() {
            @Override
            public HTableInterface getConnection(String tableName) throws IOException {
                throw new IOException();
            }
        };

        hbasedatastore1.create(entityList);

        hbasedatastore1 = new HBaseDataStore() {
            @Override
            public HTableInterface getConnection(String tableName) throws IOException {
                throw new TableNotFoundException();
            }
        };

        hbasedatastore1.create(entityList);
    }

    @Test
    public void testGetDataByRowFamilyQualifierException() {
        HBaseDataStore hbasedatastore1 = null;
        hbasedatastore1 = new HBaseDataStore() {
            @Override
            public HTableInterface getConnection(String tableName) throws IOException {
                throw new IOException();
            }
        };

        List<String> qualifierList = new ArrayList<>();
        qualifierList.add("qualifier1");
        qualifierList.add("qualifier2");
        hbasedatastore1.getDataByRowFamilyQualifier("tableName", "startRow", "endRow", "family", qualifierList, 0);
        hbasedatastore1.getDataByRowFamilyQualifier("tableName", "startRow", "endRow", "family", "qualifier", 0);

        hbasedatastore1 = new HBaseDataStore() {
            @Override
            public HTableInterface getConnection(String tableName) throws IOException {
                throw new IOException();
            }
        };

        qualifierList = new ArrayList<>();
        qualifierList.add("qualifier1");
        qualifierList.add("qualifier2");
        hbasedatastore1.getDataByRowFamilyQualifier("tableName", "startRow", "endRow", "family", qualifierList, 0);
        hbasedatastore1.getDataByRowFamilyQualifier("tableName", "startRow", "endRow", "family", "qualifier", 0);
    }
}
