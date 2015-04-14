/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hbase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.PageFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is the generic HBase data store plug-in.
 * It realizes the basic data store operations including create,
 * queries, update, and delete.
 *
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 *
 * Created: Feb 24, 2015
 *
 */
public class HBaseDataStore  {
     private static final Logger log = LoggerFactory.getLogger(HBaseDataStore.class);
     private static String zookeeperQuorum;
     private static String zookeeperClientport;
     private static int poolSize;
     private static int writeBufferSize;
     private static HTablePool htablePool;
     private static Configuration conf;
     private static Map<String, HTableInterface> htableMap = new HashMap<String, HTableInterface>();

     /**
      * Default constructor
      */
     public HBaseDataStore(){
        super();
     }

     /**
      * Constructor with specified context info.
      *
      * populate the parameters from the context info.
      * @param context
      */
     public HBaseDataStore(HBaseDataStoreContext context){
         log.debug("Entering constructor HBaseDataStore()");
         zookeeperQuorum = context.getZookeeperQuorum();
         zookeeperClientport = context.getZookeeperClientport();
         poolSize = context.getPoolSize();
         writeBufferSize = context.getWriteBufferSize();
         log.debug("Exiting constructor HBaseDataStore()");
     }

     /**
      * Create a HBase configuration based on the data store context info.
      * @return
      */
     private static Configuration getConfiguration() {
        log.debug("Entering getConfiguration()");
        if(conf == null){
            conf = HBaseConfiguration.create();
            conf.set(HBaseDataStoreConstants.ZOOKEEPER_QUORUM, zookeeperQuorum);
            conf.set(HBaseDataStoreConstants.ZOOKEEPER_CLIENTPORT, zookeeperClientport);
        }
        log.debug("Configuration of HBaseDataStore is initialized");
        log.debug("Exiting getConfiguration()");
        return conf;
     }

     /**
      * Create an HTable pool based on the poolSize obtained from the
      * HBase data store context.
      * @return
      */
     private HTablePool getHTablePool() {
        log.debug("Entering getHTablePool()");
        HTablePool htablePool = new HTablePool(getConfiguration(), poolSize);
        log.debug("Exiting getHTablePool()");
        return htablePool;
     }
     /**
      * Get connection to a HBase table.
      * @param tableName
      * @return HTableInterface, which is used to communicate with the HTable.
      */
     public HTableInterface getConnection(String tableName) {
         log.debug("Entering getConnection()");
         HTableInterface htableResult = null;
         htableResult = htableMap.get(tableName);
         ClassLoader ocl = Thread.currentThread().getContextClassLoader();
         try {
             Thread.currentThread().setContextClassLoader(HBaseConfiguration.class.getClassLoader());
             if (htableResult == null) {
                 if (htablePool == null || htablePool.getTable(tableName) == null) {
                     htablePool = getHTablePool();
                 }
                 if ( htablePool != null){
                     htableResult =   htablePool.getTable(tableName);
                     htableResult.setAutoFlush(false);
                     htableResult.setWriteBufferSize(writeBufferSize);
                 }
              }
         }catch (Exception e) {
              log.error("Error getting connection to the htable", e);
         } finally {
              Thread.currentThread().setContextClassLoader(ocl);
         }
         htableMap.put(tableName, htableResult);
         log.debug("Exiting getConnection()");
         return htableResult;
     }
     /**
      * Create HBase tables.
      * @param tableName
      */
     public void createTable(String tableName){
         log.debug("Entering createTable(tableName)");
         HBaseAdmin hbase = null;
         ClassLoader ocl = Thread.currentThread().getContextClassLoader();
         try{
             Thread.currentThread().setContextClassLoader(HBaseConfiguration.class.getClassLoader())
;
             if (tableName != null){
                hbase = new HBaseAdmin(getConfiguration());
                HTableDescriptor desc = new HTableDescriptor(tableName);
                HColumnDescriptor column = new HColumnDescriptor("c1".getBytes());
                desc.addFamily(column);
                if (!hbase.tableExists(tableName)){
                    hbase.createTable(desc);
                }
             }
         }catch ( IOException ioe){
             log.error("Error creating table.",ioe);
         }catch ( Exception e){
             log.error("Error creating table.", e);
         }
         finally{
             try{
                 if(hbase != null){
                    hbase.close();
                 }
                 Thread.currentThread().setContextClassLoader(ocl);
             }catch(IOException ioe){
                 log.error("Error closing HBaseAdmin.", ioe);
             }
         }
         log.debug("Exiting createTable(tableName)");
     }
     /**
      * Create a row in HTable.
      *
      * @param entity - an object of HBaseEntity.
      * @return HBaseEntity - the object being created in HTable.
      */
     public final HBaseEntity create(final HBaseEntity entity) {
         log.debug("Entering create(HBaseEntity entity)");
         if (entity != null && entity.getRowKey() != null) {
                 Put p = new Put(Bytes.toBytes(entity.getRowKey()));
                 for (HBaseColumn currentColumn : entity.getColumns()) {
                         if(currentColumn.getTimeStamp()==0){
                                 if(currentColumn.getColumnQualifier()!=null){
                                     p.add(Bytes.toBytes(currentColumn.getColumnFamily()),
                                             Bytes.toBytes(currentColumn.getColumnQualifier()),
                                             Bytes.toBytes(currentColumn.getValue()));
                                 }else{
                                     p.add(Bytes.toBytes(currentColumn.getColumnFamily()),
                                         null,
                                         Bytes.toBytes(currentColumn.getValue()));
                                 }
                         }else{
                                 if(currentColumn.getColumnQualifier()!=null){
                                     p.add(Bytes.toBytes(currentColumn.getColumnFamily()),
                                                 Bytes.toBytes(currentColumn.getColumnQualifier()),
                                                 currentColumn.getTimeStamp(),
                                                 Bytes.toBytes(currentColumn.getValue()));
                                 }else{
                                     p.add(Bytes.toBytes(currentColumn.getColumnFamily()),
                                         Bytes.toBytes(currentColumn.getColumnQualifier()),
                                         Bytes.toBytes(currentColumn.getValue()));
                                 }
                         }
                 }
                 HTableInterface htable = null;
                 try {
                     htable = getConnection(entity.getTableName());
                     htable.put(p);
                 } catch (IOException ioe) {
                     log.error("Cannot put Data into Hbase", ioe);
                 } catch ( Exception e){
                     log.error("Cannot put Data into HBase.", e);
                 }
         }
         log.debug("Exiting create(HBaseEntity entity)");
         return entity;
}

     /**
      * Retrieve data by specified tableName, startRowkey, endRowkey,
      * column family name, and column qualifier name.
      * @param tableName
      * @param startRow
      * @param endRow
      * @param family
      * @param qualifier
      * @return
      */
     public List<HBaseEntity> getDataByRowFamilyQualifier(String tableName, String startRow, String endRow, String family, String qualifier){
         return getDataByRowFamilyQualifier(tableName, startRow, endRow, family, qualifier, 0);
     }


     /**
      * Retrieve data by specified tableName, startRowkey, endRowkey,
      * column family name, column qualifier name, and page size.
      * @param tableName
      * @param startRow
      * @param endRow
      * @param family
      * @param qualifier
      * @param pageSize
      * @return
      */
     public List<HBaseEntity> getDataByRowFamilyQualifier(String tableName, String startRow, String endRow, String family, String qualifier, long pageSize){
         List<HBaseEntity> resultEntityList=new ArrayList<HBaseEntity>();
                 Scan scan =new Scan(Bytes.toBytes(startRow), Bytes.toBytes(endRow));
         if (qualifier!=null) {
                 scan.addColumn(Bytes.toBytes(family), Bytes.toBytes(qualifier));
                 }else {
                         scan.addFamily(Bytes.toBytes(family));
                 }
         if (pageSize>0){
                  Filter filter = new PageFilter(pageSize);
                  scan.setFilter(filter);
         }
                 HTableInterface htable = null;
         ResultScanner rs=null;
         try {
                         htable=getConnection(tableName);
                         rs = htable.getScanner(scan);
                         for (Result currentResult = rs.next(); currentResult != null; currentResult = rs.next()) {
                                 resultEntityList.add(convertResultToEntity(tableName, currentResult));
                         }
                 } catch (IOException e) {
                     log.error("Scanner error", e);
                 }finally{
                         if (rs!=null){
                         rs.close();
                         rs=null;
                         }
                         closeConnection(htable);
                 }
         return resultEntityList;
     }

     /**
      * Retrieve data by the specified tableName, startRowkey, endRowkey,
      * column family name, and a list of column qualifier names.
      * @param tableName
      * @param startRow
      * @param endRow
      * @param family
      * @param qualifierList
      * @return
      */
     public List<HBaseEntity> getDataByRowFamilyQualifier(String tableName, String startRow, String endRow, String family, List<String> qualifierList){
         return getDataByRowFamilyQualifier(tableName, startRow, endRow, family, qualifierList, 0);
     }

     /**
      * Retrieve data by the specified tableName, startRowkey, endRowkey,
      * column family name, a list of column qualifier names, and page size.
      * @param tableName
      * @param startRow
      * @param endRow
      * @param family
      * @param qualifierList
      * @param pageSize
      * @return
      */
     public List<HBaseEntity> getDataByRowFamilyQualifier(String tableName, String startRow, String endRow, String family, List<String> qualifierList, long pageSize){
         List<HBaseEntity> resultEntityList=new ArrayList<HBaseEntity>();
                 Scan scan =new Scan(Bytes.toBytes(startRow), Bytes.toBytes(endRow));
                 if ((qualifierList != null) && qualifierList.size() > 0) {
                         for (String qualifier : qualifierList) {
                                 scan.addColumn(Bytes.toBytes(family), Bytes.toBytes(qualifier));
                         }
                 } else {
                         scan.addFamily(Bytes.toBytes(family));
                 }
         if (pageSize>0){
                  Filter filter = new PageFilter(pageSize);
                  scan.setFilter(filter);
         }
                 HTableInterface htable = null;
         ResultScanner rs=null;
                 try {
                         htable=getConnection(tableName);
                         rs = htable.getScanner(scan);
                         for (Result currentResult = rs.next(); currentResult != null; currentResult = rs.next()) {
                                 resultEntityList.add(convertResultToEntity(tableName, currentResult));
                         }
                 } catch (IOException e) {
                     log.error("Scanner error", e);
                 }finally{
                         if (rs!=null){
                         rs.close();
                         rs=null;
                         }
                         closeConnection(htable);
                 }
         return resultEntityList;
     }

     /**
      * Retrieve data by the specified tableName, start timestamp, and end timestamp.
      * @param tableName
      * @param startTime
      * @param endTime
      * @return
      */
     public List<HBaseEntity> getDataByTimeRange(String tableName, long startTime, long endTime){
            List<HBaseEntity> resultEntityList=new ArrayList<HBaseEntity>();
            Scan scan =new Scan();
            HTableInterface htable = null;
            ResultScanner rs=null;
            try {
                    scan.setTimeRange(startTime, endTime);
                    htable=getConnection(tableName);
                    rs = htable.getScanner(scan);
                    int count = 0;
                    for (Result currentResult = rs.next(); currentResult != null; currentResult = rs.next()) {
                        if ( count++ < TSDRHBaseDataStoreConstants.MAX_QUERY_RECORDS){
                            resultEntityList.add(convertResultToEntity(tableName, currentResult));
                        }
                    }
            } catch (IOException ioe) {
                    log.error("Scanner error", ioe);
            } catch (Exception e) {
                    log.error("Scanner error", e);
            }finally{

                    if (rs!=null){
                    rs.close();
                    rs=null;
                    }
                    closeConnection(htable);
            }
            return resultEntityList;

     }

     /**
      * Convert the result from HBase query API to HBaseEntity.
      * @param tableName
      * @param result
      * @return
      */
     private HBaseEntity convertResultToEntity(String tableName, Result result){
         if(result==null){
                 return null;
         }
         HBaseEntity resultEntity=new HBaseEntity();
         resultEntity.setTableName(tableName);
         resultEntity.setRowKey(Bytes.toString(result.getRow()));
         List <HBaseColumn> noSQLColumnList=new ArrayList<HBaseColumn>();
                 NavigableMap <byte[], NavigableMap<byte[], NavigableMap<Long, byte[]>>> map=result.getMap();
                 for(byte [] currentByteFamily:  map.keySet()){
             NavigableMap<byte[], NavigableMap<Long, byte[]>> columnMap=map.get(currentByteFamily);
             for(byte [] currentByteColumn: columnMap.keySet()){
                 HBaseColumn noSQLColumn=new HBaseColumn();
                 noSQLColumn.setColumnFamily(Bytes.toString(currentByteFamily));
                 noSQLColumn.setColumnQualifier(Bytes.toString(currentByteColumn));
                 noSQLColumn.setValue(Bytes.toString(result.getValue(currentByteFamily, currentByteColumn)));
                 noSQLColumnList.add(noSQLColumn);
             }
                 }
                 resultEntity.setColumns(noSQLColumnList);
         return resultEntity;

     }

     /**
      * Close the connection to the specified HTable.
      * @param tableName - The name of the HTable.
      */
     public void closeConnection(String tableName){
         log.debug("Entering closeConnection(String tableName)");
         HTableInterface htableResult=null;
         htableResult=htableMap.get(tableName);
         if (htableResult != null) {
                         try {
                                 htableResult.close();
                                 htableMap.remove(tableName);
                         } catch (IOException e) {
                                    log.error("Cannot close connection:", e);
                         }
         }
         log.debug("Exiting closeConnection(String tableName)");
     }
     /**
      * Close the connection to the specified HTable.
      * @param htable - HTable
      */
     private void closeConnection(HTable htable){
         log.debug("Entering closeConnection(HTable htable)");
         if (htable != null) {
                         try {
                                 htable.close();
                                 htableMap.remove(Bytes.toString(htable.getTableName()));
                         } catch (IOException e) {
                                    log.error("Cannot close connection:", e);
                         }
         }
         log.debug("Exiting closeConnection(HTable htable)");
     }
     /**
      * Close the connection to the specified HTable.
      * @param htable
      */
     private void closeConnection(HTableInterface htable){
         log.debug("Entering closeConnection(HTableInterface htable)");
         if (htable != null) {
                         try {
                                 htable.close();
                                 htableMap.remove(Bytes.toString(htable.getTableName()));
                         } catch (IOException e) {
                                    log.error("Cannot close connection:", e);
                         }
         }
         log.debug("Exiting closeConnection(HTableInterface htable)");
     }
}
