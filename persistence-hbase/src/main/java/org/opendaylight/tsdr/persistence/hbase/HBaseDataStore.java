/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hbase;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.HTablePool;
import org.apache.hadoop.hbase.client.Put;
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
     private String zookeeperQuorum;
     private String zookeeperClientport;
     private int poolSize;
     private HTablePool htablePool;
     private Map<String, HTableInterface> htableMap = new HashMap<String, HTableInterface>();

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
         log.debug("Exiting constructor HBaseDataStore()");
     }

     /**
      * Create a HBase configuration based on the data store context info.
      * @return
      */
     private Configuration getConfiguration() {
        log.debug("Entering getConfiguration()");
        Configuration conf;
        conf = HBaseConfiguration.create();
        conf.set(HBaseDataStoreConstants.ZOOKEEPER_QUORUM, zookeeperQuorum);
        conf.set(HBaseDataStoreConstants.ZOOKEEPER_CLIENTPORT, zookeeperClientport);
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
         if (htableResult == null) {
              if (htablePool == null) {
                 htablePool = getHTablePool();
             }
             htableResult =   htablePool.getTable(tableName);
             htableMap.put(tableName, htableResult);
         }
         log.debug("Exiting getConnection()");
         return htableResult;
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
                 ClassLoader ocl = Thread.currentThread().getContextClassLoader();
                 try {
                     Thread.currentThread().setContextClassLoader(HBaseConfiguration.class.getClassLoader());
                     htable = getConnection(entity.getTableName());
                     htable.put(p);
                 } catch (IOException e) {
                     log.error("Cannot put Data to Hbase", e);
                 } finally {
                     closeConnection(htable);
                     Thread.currentThread().setContextClassLoader(ocl);
                 }
         }
         log.debug("Exiting create(HBaseEntity entity)");
         return entity;
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
