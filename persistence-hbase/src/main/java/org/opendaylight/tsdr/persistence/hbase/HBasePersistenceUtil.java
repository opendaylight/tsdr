/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hbase;

import java.util.ArrayList;
import java.util.List;

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
        String nodeID = metricData.getNodeID();
        String metricName = metricData.getMetricName();
        String metricValue = new Long(metricData.getMetricValue().getValue()
            .longValue()).toString();
        Long timeStamp = new Long(metricData.getTimeStamp().longValue());
        String tableName = getTableNameFrom(dataCategory);
        String keyString = getKeyStringFrom(metricData);

        StringBuffer rowKey = new StringBuffer();
        rowKey.append(metricName).append(TSDRHBaseDataStoreConstants.ROWKEY_SPLIT)
            .append(nodeID)
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
                if (key.getKeyName() != null && key.getKeyName()
                   .equalsIgnoreCase(TSDRConstants.FLOW_TABLE_KEY_NAME)){
                        keyString = keyString.append(TSDRHBaseDataStoreConstants.ROWKEY_SPLIT)
                          .append(key.getKeyValue());
                        }else if ( key.getKeyName() != null && key.getKeyName()
                              .equalsIgnoreCase(TSDRConstants.FLOW_KEY_NAME)){
                              keyString = keyString.append(TSDRHBaseDataStoreConstants.ROWKEY_SPLIT)
                               .append(key.getKeyValue());
                        }else if (key.getKeyName() != null && key.getKeyName()
                                 .equalsIgnoreCase(TSDRConstants.GROUP_KEY_NAME)){
                                 keyString = keyString.append(TSDRHBaseDataStoreConstants
                                     .ROWKEY_SPLIT).append(key.getKeyValue());
                        }else if (key.getKeyName() != null && key.getKeyName()
                                         .equalsIgnoreCase(TSDRConstants.BUCKET_KEY_NAME)){
                                         keyString = keyString.append(TSDRHBaseDataStoreConstants
                                             .ROWKEY_SPLIT).append(key.getKeyValue());
                        }else if (key.getKeyName() != null && key.getKeyName()
                                 .equalsIgnoreCase(TSDRConstants.QUEUE_KEY_NAME)){
                                     keyString = keyString.append(TSDRHBaseDataStoreConstants
                                 .ROWKEY_SPLIT).append(key.getKeyValue());
                        }else if (key.getKeyName() != null && key.getKeyName()
                                 .equalsIgnoreCase(TSDRConstants.INTERNFACE_KEY_NAME)){
                                     keyString = keyString.append(TSDRHBaseDataStoreConstants
                                     .ROWKEY_SPLIT).append(key.getKeyValue());
                        }
           }
        }
        return keyString.toString();
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
}
