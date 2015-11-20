/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hbase;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.opendaylight.tsdr.spi.model.TSDRConstants;
import org.opendaylight.tsdr.spi.util.FormatUtil;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRMetric;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.gettsdrmetrics.output.TSDRMetricRecordList;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.gettsdrmetrics.output.TSDRMetricRecordListBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrlog.RecordAttributes;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Counter64;
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
     * Get HBaseEntity from TSDRMetric data structure.
     *
     * @param metricData
     * @return
     */
    public static HBaseEntity getEntityFromMetricStats(TSDRMetric metricData
        , DataCategory dataCategory){
        log.debug("Entering getEntityFromMetricStats(TSDRMetric)");
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
        if ( metricName == null || metricName.trim().length() == 0){
            return null;
        }
        String metricValue = metricData.getMetricValue().toString();
        Long timeStamp = new Long(metricData.getTimeStamp().longValue());
        String tableName = getTableNameFrom(dataCategory);
        if ( tableName == null || tableName.trim().length() == 0){
            return null;
        }
        String keyString = getKeyStringFrom(metricData);
        if ( keyString == null || keyString.trim().length() == 0){
            return null;
        }
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
        log.debug("Exiting getEntityFromMetricStats(TSDRMetric)");
        return entity;
    }

    /**
     * Get HBaseEntity from TSDRLogRecord data structure.
     *
     * @param logRecord
     * @return
     */
    public static HBaseEntity getEntityFromLogRecord(TSDRLogRecord logRecord
        , DataCategory dataCategory){
        log.debug("Entering getEntityFromLogRecord(TSDRLogRecord)");
        if ( logRecord == null){
            log.error("logRecord is null");
            return null;
        }else if ( logRecord.getNodeID() == null){
            log.error("NodeID in metric Data is null");
            return null;
        } 

        HBaseEntity entity = new HBaseEntity();
        String nodeID = logRecord.getNodeID();
        if ( nodeID == null || nodeID.trim().length() == 0){
            return null;
        }
         
        Long timeStamp = new Long(logRecord.getTimeStamp().longValue());
        String tableName = getTableNameFrom(dataCategory);
        if ( tableName == null || tableName.trim().length() == 0){
            return null;
        }
        String keyString = getKeyStringFrom(logRecord);
        if ( keyString == null || keyString.trim().length() == 0){
            return null;
        }
        StringBuffer rowKey = new StringBuffer();
        rowKey.append(keyString).append(TSDRHBaseDataStoreConstants.ROWKEY_SPLIT)
            .append(timeStamp);
        entity.setTableName(tableName);
        entity.setRowKey(rowKey.toString());
        List<HBaseColumn> columnList = new ArrayList<HBaseColumn>();

        //add attribute names as columns
        List<RecordAttributes> attributes = logRecord.getRecordAttributes();
        if ( attributes != null && attributes.size() != 0){
            for ( RecordAttributes attribute: attributes){
                HBaseColumn column = new HBaseColumn();
                column.setColumnFamily(TSDRHBaseDataStoreConstants.COLUMN_FAMILY_NAME);
                column.setTimeStamp(timeStamp);
                column.setColumnQualifier(attribute.getName());
                column.setValue(attribute.getValue());
                columnList.add(column);
            }
        }
        //add FullLengthText as the last column
        HBaseColumn column = new HBaseColumn();
        column.setColumnFamily(TSDRHBaseDataStoreConstants.COLUMN_FAMILY_NAME);
        column.setTimeStamp(timeStamp);
        column.setColumnQualifier(TSDRHBaseDataStoreConstants.LOGRECORD_FULL_TEXT);
        column.setValue(logRecord.getRecordFullText());
        columnList.add(column);

        entity.setColumns(columnList);
        log.debug("Exiting getEntityFromLogRecord(TSDRLogRecord)");
        return entity;
    }

    /**
     * Return table name based on the data category.
     * @param datacategory
     * @return
     */
    public static String getTableNameFrom(DataCategory datacategory){
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
        }else if (datacategory == DataCategory.FLOWMETERSTATS){
            return TSDRHBaseDataStoreConstants.METER_METRICS_TABLE_NAME;
        }else if (datacategory == DataCategory.SYSLOG){
            return TSDRHBaseDataStoreConstants.SYSLOG_TABLE_NAME;
        } else if (datacategory == DataCategory.NETFLOW){
            return TSDRHBaseDataStoreConstants.NETFLOW_TABLE_NAME;
        } else{
            log.error("The category is not supported:{}" , datacategory.toString());
            return "";
        }

    }


    /**
     * Return Data Category Name from HBase Table name.
     * @param tableName
     * @return
     */
    public static String getCategoryNameFrom(String tableName){
        if ( tableName.equalsIgnoreCase(
            TSDRHBaseDataStoreConstants.FLOW_STATS_TABLE_NAME)){
            return TSDRConstants.FLOW_STATS_CATEGORY_NAME;
        }else  if ( tableName.equalsIgnoreCase(
            TSDRHBaseDataStoreConstants.FLOW_TABLE_STATS_TABLE_NAME)){
            return TSDRConstants.FLOW_TABLE_STATS_CATEGORY_NAME;
        }else if ( tableName.equalsIgnoreCase(
                TSDRHBaseDataStoreConstants.INTERFACE_METRICS_TABLE_NAME)){
                return TSDRConstants.PORT_STATS_CATEGORY_NAME;
        }else if ( tableName.equalsIgnoreCase(
                TSDRHBaseDataStoreConstants.GROUP_METRICS_TABLE_NAME)){
                return TSDRConstants.FLOW_GROUP_STATS_CATEGORY_NAME;
        }else if ( tableName.equalsIgnoreCase(
                TSDRHBaseDataStoreConstants.QUEUE_METRICS_TABLE_NAME)){
                return TSDRConstants.QUEUE_STATS_CATEGORY_NAME;
        }if ( tableName.equalsIgnoreCase(
                TSDRHBaseDataStoreConstants.METER_METRICS_TABLE_NAME)){
                return TSDRConstants.FLOW_METER_STATS_CATEGORY_NAME;
        }if ( tableName.equalsIgnoreCase(
                TSDRHBaseDataStoreConstants.SYSLOG_TABLE_NAME)){
                return TSDRConstants.SYSLOG_CATEGORY_NAME;
        }if ( tableName.equalsIgnoreCase(
                TSDRHBaseDataStoreConstants.NETFLOW_TABLE_NAME)){
                return TSDRConstants.NETFLOW_CATEGORY_NAME;
        }else{
            log.warn("The table name is not supported: {}", tableName);
            return null;
        }
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
     * Get KeyString from TSDRLogRecord data.
     * @param logRecord
     * @return
     */
    private static String getKeyStringFrom(TSDRLogRecord logRecord){
        StringBuffer keyString = new StringBuffer();
        keyString.append(logRecord.getNodeID());
      //  keyString = keyString.append(TSDRHBaseDataStoreConstants.ROWKEY_SPLIT);
        List<RecordKeys> recordKeys = logRecord.getRecordKeys();
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
        hbaseTables.add(TSDRHBaseDataStoreConstants.METER_METRICS_TABLE_NAME);
        hbaseTables.add(TSDRHBaseDataStoreConstants.NETFLOW_TABLE_NAME);
        hbaseTables.add(TSDRHBaseDataStoreConstants.SYSLOG_TABLE_NAME);
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
                 recordbuff.append("TimeStamp = ");
                 recordbuff.append(formattedTimeStamp);
                 recordbuff.append("|");
                 recordbuff.append("MetricName = ");
                 recordbuff.append(metricID);
                 recordbuff.append("|");
                 recordbuff.append("MetricValue = ");
                 recordbuff.append(cellValue);
                 recordbuff.append("|");
                 recordbuff.append("MetricCategory = ");
                 recordbuff.append(getCategoryNameFrom(entity.getTableName()));
                 recordbuff.append("|");
                 recordbuff.append("MetricDetails = ");
                 recordbuff.append(FormatUtil.convertToMetricDetailsJSON(objectKeys, getCategoryNameFrom(entity.getTableName())));
                 result.add(recordbuff.toString());
            }
            return result;
        }
        /**
         * Convert the HBaseEntity list to TSDRMetricRecord
         * @param entities
         * @return
         */
        public static List<TSDRMetricRecordList> convertToTSDRMetrics(DataCategory category,List<HBaseEntity> entities){
            List<TSDRMetricRecordList> result = new ArrayList<TSDRMetricRecordList>();
            for ( HBaseEntity entity : entities ){
                 String rowKey = entity.getRowKey();
                 String metricID = getMetricIDFromRowKey(rowKey);
                 String nodeID = getNodeIDFromRowKey(rowKey);
                 String cellValue = entity.getColumns().get(0).getValue();
                 TSDRMetricRecordListBuilder mb = new TSDRMetricRecordListBuilder();
                 mb.setMetricName(metricID);
                 mb.setMetricValue(new BigDecimal(Double.parseDouble(cellValue)));
                 mb.setNodeID(nodeID);
                 mb.setRecordKeys(getRecordKeyListFromRowKey(category,rowKey));
                 TSDRMetricRecordList record = mb.build();
                 result.add(record);
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

        public static String getNodeIDFromRowKey(String rowKey){
            String[] sections = rowKey.split(TSDRHBaseDataStoreConstants.ROWKEY_SPLIT);
            if (sections != null && sections.length > 0){
                //the second section of the rowkey is the NodeID
                return sections[1];
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
         * Get the record keys string from the rowkey.
         * The reocord keys starts from the third position to the second last position of the rowkey.
         * @param rowKey
         * @return
         */
        public static String getRecordKeysFromRowKey(String rowKey){
            String[] sections = rowKey.split(TSDRHBaseDataStoreConstants.ROWKEY_SPLIT);
            if (sections != null && sections.length >3 ){
                //object keys starts from section[2]
                StringBuffer buffer = new StringBuffer();
                for (int i = 2; i < sections.length - 1; i++){
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
         * Get RecordKeyList from the RowKey.
         * @param category
         * @param rowKey
         * @return
         */
        public static List<RecordKeys> getRecordKeyListFromRowKey(DataCategory category,String rowKey){
            List<RecordKeys> recordKeyList = new ArrayList<RecordKeys>();
            RecordKeysBuilder keybuilder = new RecordKeysBuilder();
            /*Get the RecordKeys string from the rowKey*/
            String keystring = getRecordKeysFromRowKey(rowKey);
            String[] keys = keystring.split(TSDRConstants.ROWKEY_SPLIT);
            if (category == DataCategory.FLOWSTATS) {
                if (keys == null || keys.length < 2){
                    return null;
                }
                keybuilder.setKeyName("TableID");
                keybuilder.setKeyValue(keys[0]);
                recordKeyList.add(keybuilder.build());
                keybuilder.setKeyName("FlowID");
                keybuilder.setKeyValue(keys[1]);
                recordKeyList.add(keybuilder.build());
            }else if(category == DataCategory.FLOWTABLESTATS){
               if (keys == null || keys.length < 1){
                       return null;
                   }
               keybuilder.setKeyName("TableID");
               keybuilder.setKeyValue(keys[0]);
               recordKeyList.add(keybuilder.build());
            }else if(category == DataCategory.PORTSTATS){
               if (keys == null || keys.length < 1){
                    return null;
                }
                keybuilder.setKeyName("InterfaceName");
                keybuilder.setKeyValue(keys[0]);
                recordKeyList.add(keybuilder.build());
            }else if(category == DataCategory.QUEUESTATS){
                if (keys == null || keys.length < 2){
                    return null;
                }
                keybuilder.setKeyName("InterfaceName");
                keybuilder.setKeyValue(keys[0]);
                recordKeyList.add(keybuilder.build());
                keybuilder.setKeyName("QueueName");
                keybuilder.setKeyValue(keys[1]);
                recordKeyList.add(keybuilder.build());
            }else if(category == DataCategory.FLOWGROUPSTATS){
                if (keys == null || keys.length < 2){
                    return null;
                }
                keybuilder.setKeyName("GroupName");
                keybuilder.setKeyValue(keys[0]);
                recordKeyList.add(keybuilder.build());
                keybuilder.setKeyName("BucketID");
                keybuilder.setKeyValue(keys[1]);
                recordKeyList.add(keybuilder.build());
            }else if(category == DataCategory.FLOWMETERSTATS){
                if (keys == null || keys.length < 2){
                    return null;
                }
                keybuilder.setKeyName("GroupName");
                keybuilder.setKeyValue(keys[0]);
                recordKeyList.add(keybuilder.build());
                keybuilder.setKeyName("MeterName");
                keybuilder.setKeyValue(keys[1]);
                recordKeyList.add(keybuilder.build());
            }else {
                log.warn("The category is not supported:{}", category);
                return null;
            }

            return recordKeyList;
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
            }else{
                result = keyString;
            }
            return result;
        }
}
