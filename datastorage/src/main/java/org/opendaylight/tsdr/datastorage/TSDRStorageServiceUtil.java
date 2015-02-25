/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.datastorage;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRMetric;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.openflowstats.ObjectKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storeofstats.input.TSDROFStats;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Counter32;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Counter64;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.statistics.FlowStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.flow.table.statistics.FlowTableStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.group.statistics.GroupStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.flow.capable.node.connector.statistics.FlowCapableNodeConnectorStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.flow.capable.node.connector.queue.statistics.FlowCapableNodeConnectorQueueStatistics;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * This class contains the utility methods used by TSDR Data Storage service.
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 *
 *    Created: March 1, 2015
 *
 */
public class TSDRStorageServiceUtil {
     private static final Logger log = LoggerFactory
            .getLogger(TSDRStorageServiceUtil.class);

      /**
      * Obtain the FlowMetrics from TSDROFStats object.
      *
      * @param statEntry
      * @return
      */
     public static List<TSDRMetricRecord> getFlowMetricsFrom(TSDROFStats statEntry) {
         log.debug("Entering getFlowMetricsFrom(Tsdrstats statEntry)");
         if (statEntry == null) {
             log.error("statEntry is null.");
             return null;
         }
         FlowStatistics flowStats = statEntry.getFlowStatistics();
         if (flowStats == null) {
             log.error("statEntry.getFlowStatistics() is null");
             return null;
         }
         List<ObjectKeys> keys = statEntry.getObjectKeys();
         if (keys == null) {
             log.error("statEntry.getObjectKeys() is null");
             return null;
         }
         String nodeID = getNodeIDFrom(keys);
         if (nodeID == null) {
             log.error("nodeID is null");
             return null;
         }
         List<RecordKeys> recordKeys = getRecordKeysFrom(keys);
         List<TSDRMetricRecord> metricList = new ArrayList<TSDRMetricRecord>();
         String timeStamp = (new Long((new Date()).getTime())).toString();
         List<TSDRMetricsStruct> flowMetricsList = TSDRMetricsMap
             .getTsdrMetricsMap().get(TSDRMetricsMap.FlowMetrics);
         if (flowMetricsList == null || flowMetricsList.size() == 0){
             return null;
         }
         metricList = addMetricRecordToMetricList(flowMetricsList,flowStats,
                 nodeID, recordKeys,DataCategory.FLOWSTATS, timeStamp);

         log.debug("Exiting getFlowMetricsFrom(Tsdrstats statEntry)");
         return metricList;
     }
    /**
     * Obtain the FlowTableMetrics from TSDROFStats object.
     *
     * @param statEntry
     * @return
     */
    public static List<TSDRMetricRecord> getFlowTableMetricsFrom(TSDROFStats statEntry) {
        log.debug("Entering getFlowTableMetricsFrom(Tsdrstats statEntry)");
        if (statEntry == null) {
            log.error("statEntry is null.");
            return null;
        }
        FlowTableStatistics flowTableStats = statEntry.getFlowTableStatistics();
        if (flowTableStats == null) {
            log.error("statEntry.getFlowTableStatistics() is null");
            return null;
        }
        List<ObjectKeys> keys = statEntry.getObjectKeys();
        if (keys == null) {
            log.error("statEntry.getObjectKeys() is null");
            return null;
        }
        String nodeID = getNodeIDFrom(keys);
        if (nodeID == null) {
            log.error("nodeID is null");
            return null;
        }
        List<RecordKeys> recordKeys = getRecordKeysFrom(keys);
        List<TSDRMetricRecord> metricList = new ArrayList<TSDRMetricRecord>();
        String timeStamp = (new Long((new Date()).getTime())).toString();
        List<TSDRMetricsStruct> flowTableMetricsList = TSDRMetricsMap
            .getTsdrMetricsMap().get(TSDRMetricsMap.FlowTableMetrics);
        if (flowTableMetricsList == null || flowTableMetricsList.size() == 0){
            return null;
        }
        metricList = addMetricRecordToMetricList(flowTableMetricsList,flowTableStats,
                nodeID, recordKeys,DataCategory.FLOWTABLESTATS, timeStamp);
        log.debug("Exiting getFlowTableMetricsFrom(Tsdrstats statEntry)");
        return metricList;
    }
    /**
     * Obtain the PortMetrics from TSDROFStats object.
     *
     * @param statEntry
     * @return
     */
     public static List<TSDRMetricRecord> getPortMetricsFrom(TSDROFStats statEntry) {
        log.debug("Entering getPortMetricsFrom(Tsdrstats statEntry)");
        if (statEntry == null) {
            log.error("statEntry is null.");
            return null;
        }
        FlowCapableNodeConnectorStatistics portStats =
            statEntry.getFlowCapableNodeConnectorStatistics();
        if (portStats == null) {
            log.error("statEntry.getFlowCapableNodeConnectorStatistics() is null");
            return null;
        }
        List<ObjectKeys> keys = statEntry.getObjectKeys();
        if (keys == null) {
            log.error("statEntry.getObjectKeys() is null");
            return null;
        }
        String nodeID = getNodeIDFrom(keys);
        if (nodeID == null) {
            log.error("nodeID is null");
            return null;
        }
        List<RecordKeys> recordKeys = getRecordKeysFrom(keys);
        List<TSDRMetricRecord> metricList = new ArrayList<TSDRMetricRecord>();
        String timeStamp = (new Long((new Date()).getTime())).toString();
        List<TSDRMetricsStruct> portMetricsList = TSDRMetricsMap
            .getTsdrMetricsMap().get(TSDRMetricsMap.PortMetrics);
        if (portMetricsList == null || portMetricsList.size() == 0){
            return null;
        }
        metricList = addMetricRecordToMetricList(portMetricsList,portStats,
                nodeID, recordKeys,DataCategory.PORTSTATS, timeStamp);
        log.debug("Exiting getPortMetricsFrom(Tsdrstats statEntry)");
        return metricList;
    }
    /**
     * Obain the NodeID from the list of ObjectKeys.
     *
     * @param objectKeys
     * @return
     */
    private static String getNodeIDFrom(List<ObjectKeys> objectKeys) {
        if (objectKeys == null) {
            log.error("objectKeys is null.");
            return "";
        }
        for (ObjectKeys objectKey : objectKeys) {
            if (objectKey != null && objectKey.getKeyName() != null
                    && objectKey.getKeyName().equalsIgnoreCase("NodeID")) {
                return objectKey.getKeyValue();
            } else {
                if (objectKey == null) {
                    log.error("objectKey is null");
                } else if (objectKey.getKeyName() == null) {
                    log.error("objectKey.getKeyName() is null");
                }
            }
        }
        log.error("Did not find the NodeID in the object keys.");
        return "";
    }

    /**
     * Obtain the RecordKeys from the list of ObjectKeys.
     *
     * @param objectKeys
     * @return
     */
     private static List<RecordKeys> getRecordKeysFrom(List<ObjectKeys> objectKeys) {
        log.debug("Entering getRecordKeysFrom()");
        List<RecordKeys> recordKeyList = new ArrayList<RecordKeys>();
        for (ObjectKeys objectKey : objectKeys) {
            /*
             * Skip "NodeID" key
            */
            if (!objectKey.getKeyName().equalsIgnoreCase("NodeID")) {
                RecordKeys recordKeys = new RecordKeysBuilder()
                        .setKeyName(objectKey.getKeyName())
                        .setKeyValue(objectKey.getKeyValue()).build();
            recordKeyList.add(recordKeys);
            }
         }
        log.debug("Exiting getRecordKeysFrom()");
        return recordKeyList;
    }

     /**
      * Obtain the QueueMetrics from TSDROFStats object.
      *
      * @param statEntry
      * @return
      */
     public static List<TSDRMetricRecord> getQueueMetricsFrom(TSDROFStats statEntry) {
         log.debug("Entering getQueueMetricsFrom(Tsdrstats statEntry)");
         if (statEntry == null) {
             log.error("statEntry is null.");
             return null;
         }
         FlowCapableNodeConnectorQueueStatistics queueStats = statEntry.getFlowCapableNodeConnectorQueueStatistics();
         if (queueStats == null) {
             log.error("statEntry.getFlowTableStatistics() is null");
             return null;
         }
         List<ObjectKeys> keys = statEntry.getObjectKeys();
         if (keys == null) {
             log.error("statEntry.getObjectKeys() is null");
             return null;
         }
         String nodeID = getNodeIDFrom(keys);
         if (nodeID == null) {
             log.error("nodeID is null");
             return null;
         }
         List<RecordKeys> recordKeys = getRecordKeysFrom(keys);
         List<TSDRMetricRecord> metricList = new ArrayList<TSDRMetricRecord>();
         String timeStamp = (new Long((new Date()).getTime())).toString();
         List<TSDRMetricsStruct> queueMetricsList = TSDRMetricsMap
             .getTsdrMetricsMap().get(TSDRMetricsMap.QueueMetrics);
         if (queueMetricsList == null || queueMetricsList.size() == 0){
             return null;
         }
         metricList = addMetricRecordToMetricList(queueMetricsList,queueStats,
                 nodeID, recordKeys,DataCategory.FLOWGROUPSTATS, timeStamp);

         log.debug("Exiting getQueueMetricsFrom(Tsdrstats statEntry)");
         return metricList;
     }
     /**
      * Obtain the QueueMetrics from TSDROFStats object.
      *
      * @param statEntry
      * @return
      */
     public static List<TSDRMetricRecord> getGroupMetricsFrom(TSDROFStats statEntry) {
         log.debug("Entering getGroupMetricsFrom(Tsdrstats statEntry)");
         if (statEntry == null) {
             log.error("statEntry is null.");
             return null;
         }
         GroupStatistics groupStats = statEntry.getGroupStatistics();
         if (groupStats == null) {
             log.error("statEntry.getGroupStatistics() is null");
             return null;
         }
         List<ObjectKeys> keys = statEntry.getObjectKeys();
         if (keys == null) {
             log.error("statEntry.getObjectKeys() is null");
             return null;
         }
         String nodeID = getNodeIDFrom(keys);
         if (nodeID == null) {
             log.error("nodeID is null");
             return null;
         }
         List<RecordKeys> recordKeys = getRecordKeysFrom(keys);
         List<TSDRMetricRecord> metricList = new ArrayList<TSDRMetricRecord>();
         String timeStamp = (new Long((new Date()).getTime())).toString();
         List<TSDRMetricsStruct> groupMetricsList = TSDRMetricsMap
             .getTsdrMetricsMap().get(TSDRMetricsMap.GroupMetrics);
         if (groupMetricsList == null || groupMetricsList.size() == 0){
             return null;
         }
         metricList = addMetricRecordToMetricList(groupMetricsList,groupStats,
           nodeID, recordKeys,DataCategory.FLOWGROUPSTATS, timeStamp);

         log.debug("Exiting getGroupMetricsFrom(Tsdrstats statEntry)");
         return metricList;
     }
/**
 * Return the metric value based on the passed method Name and Tsdrstats data structure.
 * @param statEntry
 * @param methodName
 * @return the metricValue with the type of Counter64
 */
   private static Counter64 getMetricValue(DataObject stats, String methodName){
         Counter64 metricValue = new Counter64(new BigInteger("0"));
         Object result = null;
      try{
         if ( stats instanceof FlowStatistics){
             FlowStatistics flowStats = (FlowStatistics) stats;
             Method method = FlowStatistics.class.getMethod(methodName);
             result = method.invoke(flowStats);
         }else if (stats instanceof FlowTableStatistics){
             FlowTableStatistics flowTableStats = (FlowTableStatistics)stats;
             Method method = FlowTableStatistics.class.getMethod(methodName);
             result = method.invoke(flowTableStats);
         }
         else if(stats instanceof FlowCapableNodeConnectorStatistics){
            FlowCapableNodeConnectorStatistics portStats =
                 (FlowCapableNodeConnectorStatistics)stats;
            Method method = FlowCapableNodeConnectorStatistics.class.getMethod(methodName);
            result = method.invoke(portStats);
         }else if(stats instanceof FlowCapableNodeConnectorQueueStatistics){
            FlowCapableNodeConnectorQueueStatistics queueStats
             = (FlowCapableNodeConnectorQueueStatistics)stats;
            Method method = FlowCapableNodeConnectorQueueStatistics.class.getMethod(methodName);
            result = method.invoke(queueStats);
         }else if(stats instanceof GroupStatistics){
            GroupStatistics groupStats = (GroupStatistics)stats;
            Method method = GroupStatistics.class.getMethod(methodName);
            result = method.invoke(groupStats);
         }else {
             log.error("Not a supported TSDR metrics category  {}", stats.getClass().toString());
             return null;
         }
     }catch(InvocationTargetException ite){
         log.error("Error executing method {}",methodName);
     }catch(NoSuchMethodException nsme){
         log.error("No such method {} in {}class.",methodName, stats.getClass().toString());
     }catch(IllegalAccessException iae){
         log.error("Illegal access of the method {} in {} class.",methodName,stats.getClass().toString());
     }
     if (result instanceof Counter32){
         Counter32 counter = (Counter32) result;
             metricValue = new Counter64(new BigInteger(counter.getValue().toString()));
     }else if ( result instanceof Counter64){
             metricValue = (Counter64)result;
     }
     return metricValue;
  }

   /**
    * Add the metric records to the metric list with type List<TSDRMetricRecord>
    * @param tsdrMetricsStructList
    * @param stats
    * @param nodeID
    * @param recordKeys
    * @param dataCategory
    * @param timeStamp
    * @return
    */
  private static List<TSDRMetricRecord> addMetricRecordToMetricList(
          List<TSDRMetricsStruct> tsdrMetricsStructList,
          DataObject stats, String nodeID, List<RecordKeys> recordKeys,
          DataCategory dataCategory,String timeStamp){

      List<TSDRMetricRecord> metricList = new ArrayList<TSDRMetricRecord>();
      for ( TSDRMetricsStruct struct: tsdrMetricsStructList){
          if ( struct == null ){
              //continue to the next struct
              continue;
          }
          TSDRMetricRecordBuilder builder = new TSDRMetricRecordBuilder();
          String methodName = struct.getMethodName();
          if ( methodName == null || methodName.length() == 0){
              //the methodName is invalid, continue to the next struct
              continue;
          }
          TSDRMetric tsdrMetric =   builder.setMetricName(struct.getMetricName())
               .setMetricValue(getMetricValue(stats,methodName))
               .setNodeID(nodeID)
               .setRecordKeys(recordKeys)
               .setTSDRDataCategory(dataCategory)
               .setTimeStamp(new BigInteger(timeStamp)).build();
          metricList.add((TSDRMetricRecord) tsdrMetric);
      }//end of for
      return metricList;
  }
}
