/*
 * Copyright (c) 2016 xFlow Research Inc. and others.  All rights reserved
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.datastorage.persistence.hbase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.tsdr.persistence.hbase.HBaseColumn;
import org.opendaylight.tsdr.persistence.hbase.HBaseDataStore;
import org.opendaylight.tsdr.persistence.hbase.HBaseDataStoreContext;
import org.opendaylight.tsdr.persistence.hbase.HBaseEntity;
import org.opendaylight.tsdr.persistence.hbase.TSDRHBaseDataStoreConstants;
import static org.mockito.Mockito.doAnswer;
import org.apache.hadoop.hbase.client.Result;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.PUT;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableNotFoundException;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.hadoop.hbase.client.ResultScanner;

import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Scan;
import org.eclipse.jdt.internal.core.Assert;

import static org.mockito.Matchers.any;


/**
 * @author <a href="mailto:chaudhry.usama@xflowresearch.com">Chaudhry Muhammad Usama</a>
 * Created by Chaudhry Usama on 1/11/16.
 */
public class HBaseDataStoreTest {
    private HBaseDataStore hbaseDataStore;
    private HBaseDataStoreContext hBaseDataStoreContext;
    private HTablePool htablePool;
    private HTableInterface hTableInterface;
    private ResultScanner resultScanner;
    private Configuration conf;
    private Map<String, HTableInterface> htableMap;
    private Result result;
    @Before
    public void setup() {
        hbaseDataStore = new HBaseDataStore();
        conf = mock(Configuration.class);
        hBaseDataStoreContext = mock(HBaseDataStoreContext.class);
        htablePool = mock(HTablePool.class);
        hTableInterface = mock(HTableInterface.class);
        resultScanner = mock(ResultScanner.class);
        htableMap = mock(Map.class);
        result = mock(Result.class);
        hbaseDataStore = new HBaseDataStore(hBaseDataStoreContext){
            @Override public HTablePool getHTablePool(){
                return htablePool;
            }
            @Override public HBaseEntity convertResultToEntity(String tableName, Result result){
                HBaseEntity dentity = new HBaseEntity();
                dentity.setRowKey("rowKey1");
                dentity.setTableName("tableName1");
                List<HBaseColumn> columnList1 = new ArrayList<HBaseColumn>();
                HBaseColumn column = new HBaseColumn();
                column.setColumnFamily(TSDRHBaseDataStoreConstants.COLUMN_FAMILY_NAME);
                column.setColumnQualifier(TSDRHBaseDataStoreConstants.COLUMN_QUALIFIER_NAME);
                column.setTimeStamp(1000);
                column.setValue("metricValue");
                columnList1.add(column);
                dentity.setColumns(columnList1);
                return dentity;
            }
        };
        hbaseDataStore.setHBaseDataStoreHtableMap(htableMap);
        try{
            doAnswer(new Answer() {
                public Object answer(InvocationOnMock invocation) {
                    Object[] args = invocation.getArguments();
                        return null;
                }}).when(hTableInterface).getTableName();
        }catch(Exception ee){
            System.out.println("Can't Get the connection");
            ee.printStackTrace();
        }
        try{
            Mockito.doNothing().when(hTableInterface).close();
        }catch(Exception ee){
            System.out.println("Can't close the connection");
            ee.printStackTrace();
        }

        try {
             Mockito.doNothing().when(hTableInterface).delete(any(List.class));
        } catch (IOException e2) {
            System.out.println("Can't delete from table");
            e2.printStackTrace();
        }

        try {
             Mockito.when(resultScanner.next()).thenReturn(result).thenReturn(result).thenReturn(null);
        } catch (IOException e1) {
            System.out.println("Can't return result");
            e1.printStackTrace();
        }

        doReturn("".getBytes()).when(result).getRow();

        doReturn(hTableInterface).when(htableMap).get(any(String.class));

        doReturn(hTableInterface).when(htablePool).getTable(any(String.class));
        try {
                doReturn(resultScanner).when(hTableInterface).getScanner(any(Scan.class));
        } catch (IOException e) {
            System.out.println("Can't Scan");
        e.printStackTrace();
        }

        try{
            Mockito.doNothing().when(htablePool).putTable(any(HTableInterface.class));
            }catch(Exception ee){
                System.out.println("Can't put in the table");
                ee.printStackTrace();
            }
        try{
            Mockito.doNothing().when(hTableInterface).put(any(Put.class));
            }catch(Exception ee){
                System.out.println("Can't put anything");
                ee.printStackTrace();
            }

    }

    @Test
    public void testGetConnection() throws Exception{
        hbaseDataStore.getConnection("tableName");
    }

    @Test
    public void testCreateTable() throws Exception{
        hbaseDataStore.createTable(null);
    }
    @Test
    public void testCloseConnection() throws Exception{
        hbaseDataStore.closeConnection(null);
        hbaseDataStore.closeConnection("tableName");
    }

    @Test
    public void testFlushCommit() throws Exception{
        hbaseDataStore.flushCommit("tableName");
        hbaseDataStore.flushCommit(null);
    }

    @Test
    public void testGetDataByRowFamilyQualifier() throws Exception{
        Assert.isNotNull(hbaseDataStore.getDataByRowFamilyQualifier("tableName", "startRow", "endRow", "family", "qualifier"));
        Assert.isNotNull(hbaseDataStore.getDataByRowFamilyQualifier("tableName", "startRow", "endRow", "family", (String) null));
        Assert.isNotNull(hbaseDataStore.getDataByRowFamilyQualifier("tableName", "startRow", "endRow", "family", "qualifier", 10));
        List<String> qualifierList = new ArrayList<String>();
        Assert.isNotNull(hbaseDataStore.getDataByRowFamilyQualifier("tableName", "startRow", "endRow", "family", qualifierList));
        qualifierList.add("qualifier1");
        qualifierList.add("qualifier2");
        Assert.isNotNull(hbaseDataStore.getDataByRowFamilyQualifier("tableName", "startRow", "endRow", "family", qualifierList));
        Assert.isNotNull(hbaseDataStore.getDataByRowFamilyQualifier("tableName", "startRow", "endRow", "family", qualifierList,10));
    }

    @Test
    public void testGetDataByTimeRange() throws Exception{
        hbaseDataStore.getDataByTimeRange("tableName", 0L, 100);
        hbaseDataStore.getDataByTimeRange("tableName", 100, 200);
    }

    @Test
    public void testCreateEntity() throws Exception{
        HBaseEntity dentity = new HBaseEntity();
        dentity.setRowKey("rowKey1");
        dentity.setTableName("tableName1");
        List<HBaseColumn> columnList1 = new ArrayList<HBaseColumn>();
        HBaseColumn column = new HBaseColumn();
        column.setColumnFamily(TSDRHBaseDataStoreConstants.COLUMN_FAMILY_NAME);
        column.setColumnQualifier(TSDRHBaseDataStoreConstants.COLUMN_QUALIFIER_NAME);
        column.setTimeStamp(1000);
        column.setValue("metricValue");
        columnList1.add(column);
        dentity.setColumns(columnList1);
        hbaseDataStore.create(dentity);
        column.setTimeStamp(0);
        List<HBaseColumn> columnList2 = new ArrayList<HBaseColumn>();
        columnList2.add(column);
        dentity.setColumns(columnList2);
        hbaseDataStore.create(dentity);
        column.setColumnQualifier(null);
        List<HBaseColumn> columnList3 = new ArrayList<HBaseColumn>();
        columnList3.add(column);
        dentity.setColumns(columnList3);
        hbaseDataStore.create(dentity);
        column.setTimeStamp(1000);
        List<HBaseColumn> columnList4 = new ArrayList<HBaseColumn>();
        columnList4.add(column);
        dentity.setColumns(columnList4);
        hbaseDataStore.create(dentity);
    }

    @Test
    public void testCreateEntityList() throws Exception{
        List<HBaseEntity> entityList = new ArrayList<HBaseEntity>();
        hbaseDataStore.create(entityList);
        HBaseEntity dentity1 = new HBaseEntity();
        dentity1.setRowKey("rowKey1");
        dentity1.setTableName("tableName1");
        List<HBaseColumn> columnList1 = new ArrayList<HBaseColumn>();
        HBaseColumn column1 = new HBaseColumn();
        column1.setColumnFamily(TSDRHBaseDataStoreConstants.COLUMN_FAMILY_NAME);
        column1.setColumnQualifier(TSDRHBaseDataStoreConstants.COLUMN_QUALIFIER_NAME);
        column1.setTimeStamp(1000);
        column1.setValue("metricValue");
        columnList1.add(column1);
        dentity1.setColumns(columnList1);
        entityList.add(dentity1);
        HBaseEntity dentity2 = new HBaseEntity();
        dentity2.setRowKey("rowKey1");
        dentity2.setTableName("tableName1");
        List<HBaseColumn> columnList2 = new ArrayList<HBaseColumn>();
        HBaseColumn column2 = new HBaseColumn();
        column2.setColumnFamily(TSDRHBaseDataStoreConstants.COLUMN_FAMILY_NAME);
        column2.setColumnQualifier(TSDRHBaseDataStoreConstants.COLUMN_QUALIFIER_NAME);
        column2.setTimeStamp(0);
        column2.setValue("metricValue");
        columnList2.add(column2);
        dentity2.setColumns(columnList2);
        entityList.add(dentity2);
        HBaseEntity dentity3 = new HBaseEntity();
        dentity3.setRowKey("rowKey1");
        dentity3.setTableName("tableName1");
        List<HBaseColumn> columnList3 = new ArrayList<HBaseColumn>();
        HBaseColumn column3 = new HBaseColumn();
        column3.setColumnFamily(TSDRHBaseDataStoreConstants.COLUMN_FAMILY_NAME);
        column3.setColumnQualifier(null);
        column3.setTimeStamp(100);
        column3.setValue("metricValue");
        columnList3.add(column3);
        dentity3.setColumns(columnList3);
        entityList.add(dentity3);
        HBaseEntity dentity4 = new HBaseEntity();
        dentity4.setRowKey("rowKey1");
        dentity4.setTableName("tableName1");
        List<HBaseColumn> columnList4 = new ArrayList<HBaseColumn>();
        HBaseColumn column4 = new HBaseColumn();
        column4.setColumnFamily(TSDRHBaseDataStoreConstants.COLUMN_FAMILY_NAME);
        column4.setColumnQualifier(null);
        column4.setTimeStamp(0);
        column4.setValue("metricValue");
        columnList4.add(column4);
        dentity4.setColumns(columnList4);
        entityList.add(dentity4);
        hbaseDataStore.create(entityList);
    }

    @Test
    public void testDeleteByTimestamp() throws Exception{
        hbaseDataStore.deleteByTimestamp("tableName", 0L);
    }

    @Test
    public void testGetHTablePool(){
        HBaseDataStore hbasedatastore = new HBaseDataStore();
        Configuration mockconf = mock(Configuration.class);
        hbasedatastore.setHBaseDataStoreHtableMap(mockconf);
        hbasedatastore.getHTablePool();
        hbasedatastore.setHBaseDataStoreHtableMap(mockconf);
    }


    @After
    public void teardown() {
        hbaseDataStore = null;
    }

}