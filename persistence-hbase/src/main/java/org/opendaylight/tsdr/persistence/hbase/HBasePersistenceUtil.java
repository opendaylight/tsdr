/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hbase;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.opendaylight.tsdr.model.TSDRConstants;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRMetric;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Utility class for TSDR HBase datastore.
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 *
 * Created: Feb 24, 2015
 */
public class HBasePersistenceUtil {
    private static final Logger log = LoggerFactory.getLogger(HBasePersistenceUtil.class);

    /**
     * Get HBaseEntity from OpenFlow FlowStats object.
     *
     * @param metricData
     * @return
     */
    public static HBaseEntity getEntityFromMetricStats(TSDRMetric metricData
        , DataCategory dataCategory){
        log.debug("Entering getEntityFromFlowStats(TSDRMetric)");
        if ( metricData == null){
            log.error("metricData is null");
            return null;
        }else if ( metricData.getNodeID() == null){
            log.error("NodeID in metric Data is null");
            return null;
        }else if ( metricData.getMetricName() == null){
            log.error("MetricName is null");
            return null;
        }else if ( metricData.getMetricValue() == null){
            log.error("MetricValue is null)");
            return null;
        }

        HBaseEntity entity = new HBaseEntity();
        String metricName = metricData.getMetricName();
        String metricValue = new Long(metricData.getMetricValue().getValue()
            .longValue()).toString();
        Long timeStamp = new Long(metricData.getTimeStamp().longValue());
        String tableName = getTableNameFrom(dataCategory);
        String keyString = getKeyStringFrom(metricData);

        StringBuffer rowKey = new StringBuffer();
        rowKey.append(metricName).append(TSDRHBaseDataStoreConstants.ROWKEY_SPLIT)
            .append(keyString).append(TSDRHBaseDataStoreConstants.ROWKEY_SPLIT)
            .append(timeStamp);
        entity.setTableName(tableName);
        entity.setRowKey(rowKey.toString());
        List<HBaseColumn> columnList = new ArrayList<HBaseColumn>();
        HBaseColumn column = new HBaseColumn();
        column.setColumnFamily(TSDRHBaseDataStoreConstants.COLUMN_FAMILY_NAME);
        column.setColumnQualifier(TSDRHBaseDataStoreConstants.COLUMN_QUALIFIER_NAME);
        column.setTimeStamp(timeStamp);
        column.setValue(metricValue);
        columnList.add(column);
        entity.setColumns(columnList);
        log.debug("Exiting getEntityFromFlowStats(TSDRMetric)");
        return entity;
    }



    /**
     * Return table name based on the data category.
     * @param datacategory
     * @return
     */
    private static String getTableNameFrom(DataCategory datacategory){
        if ( datacategory == DataCategory.FLOWSTATS){
            return TSDRHBaseDataStoreConstants.FLOW_STATS_TABLE_NAME;
        }else if ( datacategory == DataCategory.FLOWTABLESTATS){
            return TSDRHBaseDataStoreConstants.FLOW_TABLE_STATS_TABLE_NAME;
        }else if ( datacategory == DataCategory.FLOWGROUPSTATS){
            return TSDRHBaseDataStoreConstants.GROUP_METRICS_TABLE_NAME;
        }else if (datacategory == DataCategory.PORTSTATS){
            return  TSDRHBaseDataStoreConstants.INTERFACE_METRICS_TABLE_NAME;
        }else if (datacategory == DataCategory.QUEUESTATS){
            return TSDRHBaseDataStoreConstants.QUEUE_METRICS_TABLE_NAME;
        }

        return "";
    }
    /**
     * Get KeyString from TSDRMetric data.
     * @param metricData
     * @return
     */
    private static String getKeyStringFrom(TSDRMetric metricData){
        StringBuffer keyString = new StringBuffer();
        List<RecordKeys> recordKeys = metricData.getRecordKeys();
        if ( recordKeys != null && recordKeys.size() != 0){
            for(RecordKeys key: recordKeys){
                if (key.getKeyValue() != null && key.getKeyValue().length() != 0){
                    keyString = keyString.append(TSDRHBaseDataStoreConstants.ROWKEY_SPLIT)
                            .append(key.getKeyValue());
                }

           }
        }
        return removeLeadingSplit(keyString.toString());
    }

    /**
     * Obtain TSDR HBase Tables name list.
     * @return
     */
    public static List<String> getTSDRHBaseTables(){
        List<String>  hbaseTables = new ArrayList<String>();
        hbaseTables.add(TSDRHBaseDataStoreConstants.FLOW_STATS_TABLE_NAME);
        hbaseTables.add(TSDRHBaseDataStoreConstants.FLOW_TABLE_STATS_TABLE_NAME);
        hbaseTables.add(TSDRHBaseDataStoreConstants.INTERFACE_METRICS_TABLE_NAME);
        hbaseTables.add(TSDRHBaseDataStoreConstants.GROUP_METRICS_TABLE_NAME);
        hbaseTables.add(TSDRHBaseDataStoreConstants.QUEUE_METRICS_TABLE_NAME);
        return hbaseTables;
    }
    /**
     * Convert HBaseEntity to String result list
     * @param entities
     * @return
     */
        public static List<String> convertToStringResultList(List<HBaseEntity> entities){
            List<String> result = new ArrayList<String>();
            for ( HBaseEntity entity : entities ){
                 String rowKey = entity.getRowKey();
                 String metricID = getMetricIDFromRowKey(rowKey);
                 String formattedTimeStamp = getFormattedTimeStampFromRowKey(rowKey);
                 String objectKeys = getObjectKeysFromRowKey(rowKey);
                 String cellValue = entity.getColumns().get(0).getValue();
                 StringBuffer recordbuff = new StringBuffer();
                 recordbuff.append("MetricID = ");
                 recordbuff.append(metricID);
                 recordbuff.append("|");
                 recordbuff.append("ObjectKeys = ");
                 recordbuff.append(objectKeys);
                 recordbuff.append("|");
                 recordbuff.append("TimeStamp = ");
                 recordbuff.append(formattedTimeStamp);
                 recordbuff.append("|");
                 recordbuff.append("MetricValue = ");
                 recordbuff.append(cellValue);
                 result.add(recordbuff.toString());
            }
            return result;
        }

        /**
         * Get metricsID, which is at the leading position of rowKey, from the rowKey
         * @param rowKey
         * @return
         */
        public static String getMetricIDFromRowKey(String rowKey){
            String[] sections = rowKey.split(TSDRHBaseDataStoreConstants.ROWKEY_SPLIT);
            if (sections != null && sections.length != 0){
                //the leading section is the metric ID
                return sections[0];
            }
            return null;
        }

        public static String getFormattedTimeStampFromRowKey(String rowKey){
            String[] sections = rowKey.split(TSDRHBaseDataStoreConstants.ROWKEY_SPLIT);
            if (sections != null && sections.length >2 ){
                //the last section of rowkey is the timestamp
                String timestamp = getFormattedTimeStamp(new Long(sections[sections.length-1]));
                return timestamp;
            }
            return null;
        }

        public static String getFormattedTimeStamp(long timestamp){
            Date date = new Date(timestamp);
            DateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US);
            return(formatter.format(date));
        }

        public static String getObjectKeysFromRowKey(String rowKey){
           String[] sections = rowKey.split(TSDRHBaseDataStoreConstants.ROWKEY_SPLIT);
           if (sections != null && sections.length >2 ){
               //the middle sections of rowkey are the object keys
               StringBuffer buffer = new StringBuffer();
               for (int i = 1; i < sections.length - 1; i++){
                   buffer.append(sections[i]);
                   if ( i != sections.length - 2){
                       buffer.append(TSDRHBaseDataStoreConstants.ROWKEY_SPLIT);
                   }
               }
                return buffer.toString();

               }
           return null;
        }

        /**
         * remove the leading split from the object key string
         * @param keyString
         * @return
         */
        private static String removeLeadingSplit(String keyString){
            String result = "";
            if (keyString != null && keyString.length() != 0 &&
                keyString.startsWith(TSDRHBaseDataStoreConstants.ROWKEY_SPLIT)){
                result = keyString.substring(1);
            }
            return result;
        }
}
