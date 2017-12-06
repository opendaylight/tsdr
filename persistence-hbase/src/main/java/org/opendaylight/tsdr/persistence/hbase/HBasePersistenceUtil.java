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
import org.opendaylight.tsdr.spi.util.FormatUtil;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.tsdrlog.RecordAttributes;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.TSDRMetric;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for TSDR HBase datastore.
 *
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 */
public class HBasePersistenceUtil {
    private static final Logger LOG = LoggerFactory.getLogger(HBasePersistenceUtil.class);

    /**
     * Get HBaseEntity from TSDRMetric data structure.
     *
     * @param metricData - the metric data
     * @param dataCategory - the data category
     * @return - an hbase entity
     */
    public static HBaseEntity getEntityFromMetricStats(TSDRMetric metricData, DataCategory dataCategory) {
        LOG.debug("Entering getEntityFromMetricStats(TSDRMetric)");
        if (!validateMetricInput(metricData)) {
            return null;
        }

        HBaseEntity entity = new HBaseEntity();
        long timeStamp;
        // If there's no timestamp in the metric Data, append the current
        // system timestamp
        if (metricData.getTimeStamp() != null) {
            timeStamp = metricData.getTimeStamp().longValue();
        } else {
            timeStamp = System.currentTimeMillis();
        }

        entity.setTableName(dataCategory.name());
        entity.setRowKey(FormatUtil.getTSDRMetricKeyWithTimeStamp(metricData));
        HBaseColumn column = new HBaseColumn();
        column.setColumnFamily(TsdrHBaseDataStoreConstants.COLUMN_FAMILY_NAME);
        column.setColumnQualifier(TsdrHBaseDataStoreConstants.COLUMN_QUALIFIER_NAME);
        column.setTimeStamp(timeStamp);
        column.setValue(metricData.getMetricValue().toString());

        List<HBaseColumn> columnList = new ArrayList<>();
        columnList.add(column);
        entity.setColumns(columnList);
        LOG.debug("Exiting getEntityFromMetricStats(TSDRMetric)");
        return entity;
    }

    /**
     * Check if the input of TSDRMetric is valid.
     * @return true - valid
     *         false - invalid
     */
    private static boolean validateMetricInput(TSDRMetric metricData) {
        if (metricData == null) {
            LOG.error("metricData is null. The data is invalid and will not be persisted.");
            return false;
        } else if (metricData.getNodeID() == null || metricData.getNodeID().trim().length() == 0) {
            LOG.error("NodeID in metric Data is null. The data is invalid and will not be persisted.");
            return false;
        } else if (metricData.getMetricName() == null || metricData.getMetricName().trim().length() == 0) {
            LOG.error("MetricName is null. The data is invalid and will not be persisted.");
            return false;
        } else if (metricData.getMetricValue() == null) {
            LOG.error("MetricValue is null. The data is invalid and will not be persisted.)");
            return false;
        }
        return true;
    }

    /**
     * Check if the input of TSDRMetric is valid.
     * @return true - valid
     *         false - invalid
     */
    private static boolean validateLogRecordInput(TSDRLogRecord logrecordData) {
        if (logrecordData == null) {
            LOG.error("logrecordData is null. The data is invalid and will not be persisted.");
            return false;
        } else if (logrecordData.getNodeID() == null || logrecordData.getNodeID().trim().length() == 0) {
            LOG.error("NodeID in logrecord Data is null. The data is invalid and will not be persisted.");
            return false;
        } else if (logrecordData.getRecordFullText() == null
                || logrecordData.getRecordFullText().trim().length() == 0) {
            LOG.error("RecordFullText is null. The data is invalid and will not be persisted.");
            return false;
        }
        return true;
    }

    /**
     * Get HBaseEntity from TSDRLogRecord data structure.
     *
     * @param logRecord - the log record
     * @param dataCategory - the data category
     * @return - an hbase entity
     */
    public static HBaseEntity getEntityFromLogRecord(TSDRLogRecord logRecord, DataCategory dataCategory) {
        LOG.debug("Entering getEntityFromLogRecord(TSDRLogRecord)");
        if (!validateLogRecordInput(logRecord)) {
            return null;
        }
        HBaseEntity entity = new HBaseEntity();
        long timeStamp;
        // If there's no timestamp in the metric Data, append the current
        // system timestamp
        if (logRecord.getTimeStamp() != null) {
            timeStamp = logRecord.getTimeStamp().longValue();
        } else {
            timeStamp = System.currentTimeMillis();
        }

        entity.setTableName(dataCategory.name());
        entity.setRowKey(FormatUtil.getTSDRLogKeyWithTimeStamp(logRecord));
        List<HBaseColumn> columnList = new ArrayList<>();

        // add attribute names as columns
        List<RecordAttributes> attributes = logRecord.getRecordAttributes();
        if (attributes != null && attributes.size() != 0) {
            for (RecordAttributes attribute : attributes) {
                HBaseColumn column = new HBaseColumn();
                column.setColumnFamily(TsdrHBaseDataStoreConstants.COLUMN_FAMILY_NAME);
                column.setTimeStamp(timeStamp);
                column.setColumnQualifier(attribute.getName());
                column.setValue(attribute.getValue());
                columnList.add(column);
            }
        }
        // add FullLengthText as the last column
        HBaseColumn column = new HBaseColumn();
        column.setColumnFamily(TsdrHBaseDataStoreConstants.COLUMN_FAMILY_NAME);
        column.setTimeStamp(timeStamp);
        column.setColumnQualifier(TsdrHBaseDataStoreConstants.LOGRECORD_FULL_TEXT);
        column.setValue(logRecord.getRecordFullText());
        columnList.add(column);

        entity.setColumns(columnList);
        LOG.debug("Exiting getEntityFromLogRecord(TSDRLogRecord)");
        return entity;
    }

    /**
     * Obtain TSDR HBase Tables name list.
     * @return - List of String
     */
    public static List<String> getTsdrHBaseTables() {
        List<String> hbaseTables = new ArrayList<>();
        DataCategory[] values = DataCategory.values();
        for (DataCategory c : values) {
            hbaseTables.add(c.name());
        }
        return hbaseTables;
    }
}
