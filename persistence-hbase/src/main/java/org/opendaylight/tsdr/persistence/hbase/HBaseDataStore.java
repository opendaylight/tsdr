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
import org.apache.hadoop.hbase.MasterNotRunningException;
import org.apache.hadoop.hbase.TableNotFoundException;
import org.apache.hadoop.hbase.ZooKeeperConnectionException;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HConnectionManager;
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
     private static boolean autoFlush;
     private static HTablePool htablePool;
     private static Configuration conf;
     private static Map<String, HTableInterface> htableMap = new HashMap<String, HTableInterface>();

     /**
      * Default constructor
      */
     public HBaseDataStore(){
        super();
     }

     /*
      * Setter for UT purpose
      */

     public void setHBaseDataStoreHtableMap(Map sethtableMap){
         if(htableMap.isEmpty()){
             htableMap = sethtableMap; //just for UT purpose
         }
     }

     public void setHBaseDataStoreHtableMap(Configuration setconf){
         if(conf == null){
             conf = setconf; //just for UT purpose
         }
     }

     public HBaseAdmin getnewHBaseAdmin() throws Throwable{
         return new HBaseAdmin(getConfiguration());

     }
     /**
      * Constructor with specified context info.
      *
      * populate the parameters from the context info.
      * @param context - the context
      */
     public HBaseDataStore(HBaseDataStoreContext context){
         log.debug("Entering constructor HBaseDataStore()");
         zookeeperQuorum = context.getZookeeperQuorum();
         zookeeperClientport = context.getZookeeperClientport();
         poolSize = context.getPoolSize();
         writeBufferSize = context.getWriteBufferSize();
         autoFlush = context.getAutoFlush();
         log.debug("Exiting constructor HBaseDataStore()");
     }

     /**
      * Create a HBase configuration based on the data store context info.
      * @return Configuration
      */
     private static Configuration getConfiguration() {
        log.debug("Entering getConfiguration()");
        if(conf == null){
            conf = HBaseConfiguration.create();
            conf.set(HBaseDataStoreConstants.ZOOKEEPER_QUORUM, zookeeperQuorum);
            conf.set(HBaseDataStoreConstants.ZOOKEEPER_CLIENTPORT, zookeeperClientport);
            conf.set(HBaseDataStoreConstants.HBASE_CLIENT_RETRIES_NUMBER,"7");
            conf.set(HBaseDataStoreConstants.HBASE_CLIENT_PAUSE, "500");
            conf.setInt("timeout", 5000);

        }
        log.debug("Configuration of HBaseDataStore is initialized");
        log.debug("Exiting getConfiguration()");
        return conf;
     }

     /**
      * Create an HTable pool based on the poolSize obtained from the
      * HBase data store context.
      * @return HTablePool
      */
     public HTablePool getHTablePool() throws Exception{
        log.debug("Entering getHTablePool()");
        HTablePool htablePool = new HTablePool(getConfiguration(), 1);
        log.debug("Exiting getHTablePool()");
        return htablePool;
     }
     /**
      * Get connection to a HBase table.
      * @param tableName - The name of the table
      * @return HTableInterface, which is used to communicate with the HTable.
      * @throws TableNotFoundException - a table not found exception
      */
     public HTableInterface getConnection(String tableName) throws Exception {
         log.debug("Entering getConnection()");
         HTableInterface htableResult = null;
         ClassLoader ocl = Thread.currentThread().getContextClassLoader();
         try {
             Thread.currentThread().setContextClassLoader(HBaseConfiguration.class.getClassLoader());
                 if (htablePool == null || htablePool.getTable(tableName) == null) {
                     htablePool = getHTablePool();
                 }
                 if ( htablePool != null){
                     htableResult =   htablePool.getTable(tableName);
                     log.debug("Obtained connection to table:" + tableName);
                     htableResult.setAutoFlush(autoFlush);
                     htableResult.setWriteBufferSize(writeBufferSize);
                 }
             htableMap.put(tableName, htableResult);
         }catch(TableNotFoundException nfe){
              throw nfe;
         }catch(IOException ioe){
              closeConnection(tableName);
             log.error("Error getting connection to the table", ioe);
         }catch (Exception e) {
              closeConnection(tableName);
              log.error("Error getting connection to the htable", e);
              log.trace("Error getting connection to the htable. StackTrace is:", e);
         } finally {
              Thread.currentThread().setContextClassLoader(ocl);
         }
         log.debug("Exiting getConnection()");
         return htableResult;
     }
     /**
      * Create HBase tables.
      * @param tableName - table name
      * @throws  Exception - some exception
      */
     public void createTable(String tableName) throws Exception{
         log.debug("Entering createTable(tableName)");
         HBaseAdmin hbase = null;
         ClassLoader ocl = Thread.currentThread().getContextClassLoader();
         try{
             Thread.currentThread().setContextClassLoader(HBaseConfiguration.class.getClassLoader());
             if (tableName != null){
                hbase = getnewHBaseAdmin();
                HTableDescriptor desc = new HTableDescriptor(tableName);
                HColumnDescriptor column = new HColumnDescriptor("c1".getBytes());
                desc.addFamily(column);
                if (!hbase.tableExists(tableName)){
                    hbase.createTable(desc);
                }
             }
         }catch ( IOException ioe){
             log.error("Error creating htable {}",tableName, ioe.getMessage());
             log.trace("Error creating htable. StackTrace is:", ioe);
             throw new Exception("Error creating table.", ioe);
         }catch ( Exception e){
             log.error("Error creating table.", e.getMessage());
             log.trace("Error creating htable. StackTrace is:", e);
             throw new Exception("Error creating table.", e);
         }catch (Throwable t){
             log.error("Error creating table.", t.getMessage());
             log.trace("Error creating htable. StackTrace is:", t);
             throw new Exception("Error creating table.", t);
         }finally{
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
      * @throws TableNotFoundException - a table not found exception
      */
     public HBaseEntity create(final HBaseEntity entity) throws TableNotFoundException{
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
                                         null,//Bytes.toBytes(currentColumn.getColumnQualifier()),
                                         Bytes.toBytes(currentColumn.getValue()));
                                 }
                         }
                 }
                 HTableInterface htable = null;
                 try {
                     htable = getConnection(entity.getTableName());
                     htable.put(p);
                     flushCommit(entity.getTableName());
                 } catch (TableNotFoundException nfe) {
                     throw nfe;
                 } catch ( IOException ioe){
                     log.error("Cannot put Data into HBase", ioe.getMessage());
                     closeConnection(entity.getTableName());
                     HConnectionManager.deleteAllConnections();
                 } catch (Exception exception) {
                     log.error("Cannot put Data into Hbase", exception.getMessage());
                     log.trace("Cannot put Data into HBase", exception);
                     closeConnection(entity.getTableName());
                     HConnectionManager.deleteAllConnections();
                 } catch (Throwable t){
                     log.error("Cannot put Data into HBase", t.getMessage());
                     log.trace("Cannot put Data into HBase", t);
                 } finally{
                    try{
                           htablePool.putTable(htable);
                           htable.close();
                           log.info("returned connection back to pool" + htable.getTableName());
                        }catch ( IOException ioe){
                            log.error("IOException caught");
                        }

                 }
         }
         log.debug("Exiting create(HBaseEntity entity)");
         return entity;
}
     /**
      * Create a list of rows in HTable.
      * The assumption is that all the entities belong to the same htable.
      *
      * @param entityList - a list of objects of HBaseEntity.
      * @return HBaseEntity - the object being created in HTable.
      * @throws TableNotFoundException - a table not found exception
      */
     public List<HBaseEntity> create(List<HBaseEntity> entityList) throws TableNotFoundException{
         log.debug("Entering create(HBaseEntity entity)");
         if((entityList==null)||(entityList.size()==0)){
             return entityList;
         }
         List <Put> putList=new ArrayList<Put>();
         String tableName = "";
         for(HBaseEntity entity: entityList){
             if (entity != null && entity.getRowKey() != null) {
                 tableName = entity.getTableName();
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
                                         null,//Bytes.toBytes(currentColumn.getColumnQualifier()),
                                         Bytes.toBytes(currentColumn.getValue()));
                                 }
                         }
                 }
                 putList.add(p);
             }
         }
                 HTableInterface htable = null;
                 try {
                     htable = getConnection(tableName);
                     htable.put(putList);
                     flushCommit(tableName);
                 } catch (TableNotFoundException nfe) {
                     throw nfe;
                 } catch ( IOException ioe){
                     log.error("Cannot put Data into HBase", ioe.getMessage());
                     closeConnection(tableName);
                     HConnectionManager.deleteAllConnections();
                 } catch (Exception exception) {
                     log.error("Cannot put Data into Hbase", exception.getMessage());
                     log.trace("Cannot put Data into HBase", exception);
                     closeConnection(tableName);
                     HConnectionManager.deleteAllConnections();
                 } catch (Throwable t){
                     log.error("Cannot put Data into HBase", t.getMessage());
                     log.trace("Cannot put Data into HBase", t);
                 } finally{
                     try{
                           htablePool.putTable(htable);
                           htable.close();
                           log.debug("Returned connection back to pool" + htable.getTableName());
                        }catch ( IOException ioe){
                            log.error("IOException caught");
                        }

                 }
         log.debug("Exiting create(HBaseEntity entity)");
         return entityList;
}

     /**
      * Retrieve data by specified tableName, startRowkey, endRowkey,
      * column family name, and column qualifier name.
      * @param tableName - the table name
      * @param startRow - the start row
      * @param endRow - the end row
      * @param family - the column family
      * @param qualifier - the qualifier
      * @return - a list of HBaseEntity
      */
     public List<HBaseEntity> getDataByRowFamilyQualifier(String tableName, String startRow, String endRow, String family, String qualifier){
         return getDataByRowFamilyQualifier(tableName, startRow, endRow, family, qualifier, 0);
     }


     /**
      * Retrieve data by specified tableName, startRowkey, endRowkey,
      * column family name, column qualifier name, and page size.
      * @param tableName - the table name
      * @param startRow - the start row
      * @param endRow - the end row
      * @param family - the family
      * @param qualifier - qualifier
      * @param pageSize - page size
      * @return - return a list of hbase entity
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
      * Retrieve data by the specified tableName, startRowkey, endRowkey,
      * column family name, and a list of column qualifier names.
      * @param tableName - the table name
      * @param startRow - the start row
      * @param endRow - the end row
      * @param family - the family
      * @param qualifierList - qualifier list
      * @return - list of hbase entity
      */
     public List<HBaseEntity> getDataByRowFamilyQualifier(String tableName, String startRow, String endRow, String family, List<String> qualifierList){
         return getDataByRowFamilyQualifier(tableName, startRow, endRow, family, qualifierList, 0);
     }

     /**
      * Retrieve data by the specified tableName, startRowkey, endRowkey,
      * column family name, a list of column qualifier names, and page size.
      * @param tableName - the table name
      * @param startRow - the start row
      * @param endRow - the end row
      * @param family - the family
      * @param qualifierList - qualifier list
      * @param pageSize - page size
      * @return - a list of hbase entity
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
                 } catch(Exception e) {
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
     * @param tableName - table name
     * @param startTime - start time
     * @param endTime - end time
     * @return a list of hbase entity
     */
     public List<HBaseEntity> getDataByTimeRange(String tableName,long startTime, long endTime) {
         return getDataByTimeRange(tableName,null,startTime,endTime);
     }
     /**
      * Retrieve data by the specified tableName, start timestamp, and end timestamp.
      * @param tableName - table name
      * @param filters - the substring filter
      * @param startTime - start time
      * @param endTime - end time
      * @return a list of hbase entity
      */
     public List<HBaseEntity> getDataByTimeRange(String tableName,List<String> filters,long startTime, long endTime){
            List<HBaseEntity> resultEntityList=new ArrayList<HBaseEntity>();
            Scan scan =new Scan();
            HTableInterface htable = null;
            ResultScanner rs=null;
            try {
                    if ( startTime != 0 && endTime != 0){
                        scan.setTimeRange(startTime, endTime);
                    }
                    /*
                        This is the proper code to use to filter the data, undortunatly I am getting a NoClassDefFound issue on FilterList

                    if(filters!=null && !filters.isEmpty()){
                        FilterList filterList = new FilterList();
                        filterList.addFilter(new PageFilter(TSDRHBaseDataStoreConstants.MAX_QUERY_RECORDS));
                        for(String filter:filters){
                            filterList.addFilter(new SingleColumnValueFilter(TSDRHBaseDataStoreConstants.COLUMN_FAMILY_NAME.getBytes(),
                                    TSDRHBaseDataStoreConstants.COLUMN_QUALIFIER_NAME.getBytes(), CompareFilter.CompareOp.EQUAL,
                                    new SubstringComparator(filter)));

                        }
                        scan.setFilter(filterList);
                    }else
                    */
                    scan.setCaching(TSDRHBaseDataStoreConstants.MAX_QUERY_RECORDS);
                    htable=getConnection(tableName);
                    rs = htable.getScanner(scan);
                    int count = 0;
                    for (Result currentResult = rs.next(); currentResult != null; currentResult = rs.next()) {
                        /*
                            This is an improper way for doing this as it void all the hbase capabilities in scaling!!!
                            However as I was not able to use HBase FilterList due to missing dependency, I had to resolve
                            to using this method to satisfy fetching a single metric.
                            We need to find a way to instantiate FilterList and install SubstringFilter inside it so we could put it
                            in the scan and allow hbase agents to filter the data.
                         */
                        if(filters!=null && !filters.isEmpty()){
                            String rowKey = Bytes.toString(currentResult.getRow());
                            boolean doesFitFilter = true;
                            for(String filter:filters){
                                if(rowKey.indexOf(filter)==-1){
                                    doesFitFilter = false;
                                    break;
                                }
                            }
                            if(!doesFitFilter){
                                continue;
                            }
                        }
                        /*
                            End of improper method.
                         */
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
      * Delete records from hbase data store based on tableName and timestamp.
      * @param tableName - table name
      * @param timestamp - time stamp
      * @throws IOException - an IOException
      */
     public void deleteByTimestamp(String tableName, long timestamp)
        throws IOException
        {
           log.info("YuLing == entering deleteByTimestamp ");
           int batchSize = 500;
           List <Delete> deleteList=new ArrayList <Delete> ();
           Scan scan = new Scan();
           scan.setTimeRange(Long.MIN_VALUE, timestamp);
           HTableInterface htable = null;
           try {
               htable = getConnection(tableName);
               ResultScanner rs = htable.getScanner(scan);
               int count = 0;
              for ( Result rr= rs.next(); rr!= null; rr = rs.next()){
                  deleteList.add(new Delete(rr.getRow()));
                  count++;
                  if ( count >= batchSize){
                     htable.delete(deleteList);
                     count = 0;
                     deleteList.clear();
                  }
               }
               if ( count > 0 && deleteList != null
                   && deleteList.size() != 0){
                   htable.delete(deleteList);
               }
           }catch (TableNotFoundException nfe) {
               throw nfe;
           } catch ( IOException ioe){
               log.error("Deletion from HBase Data Store failed!", ioe.getMessage());
               closeConnection(tableName);
               HConnectionManager.deleteAllConnections();
           } catch (Exception exception) {
               log.error("Deletion from HBase Data Store failed!", exception.getMessage());
               closeConnection(tableName);
               HConnectionManager.deleteAllConnections();
           } catch (Throwable t){
               log.error("Deletion from HBase Data Store failed!", t.getMessage());
               log.trace("Deletion from HBase Data Store failed!", t);
           } finally{
              try{
                     htablePool.putTable(htable);
                     htable.close();
                     log.info("returned connection back to pool" + htable.getTableName());
                  }catch ( IOException ioe){
                      log.error("IOException caught");
                  }

           }
           log.debug("Exiting deleteByTimeStamp()");
   }

     /**
      * Convert the result from HBase query API to HBaseEntity.
      * @param tableName - table name
      * @param result - result
      * @return - a list of hbase entity
      */
     public HBaseEntity convertResultToEntity(String tableName, Result result){
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
      * Flush the commits for the given tablename
      * @param tableName - the table name
      */
     public void flushCommit(String tableName){
         log.debug("Entering flushCommit(tableName)");
         HTableInterface htableResult=null;
         htableResult=htableMap.get(tableName);
         if(htableResult != null) {
             if(!autoFlush){
                 try{
                     htableResult.flushCommits();
                 }catch(IOException e){
                     log.error("Flushcommit failed", e);
                 }catch(Exception e){
                     log.error("Exception during flushcommit", e);
                 }
             }
         }
         log.debug("Exiting flushCommit(tableName)");
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
     public void closeConnection(HTable htable){
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
      * @param htable - the htable
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
