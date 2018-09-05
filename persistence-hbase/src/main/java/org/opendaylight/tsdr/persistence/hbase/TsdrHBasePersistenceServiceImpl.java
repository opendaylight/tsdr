/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hbase;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.hadoop.hbase.TableNotFoundException;
import org.opendaylight.tsdr.spi.persistence.TSDRBinaryPersistenceService;
import org.opendaylight.tsdr.spi.persistence.TSDRLogPersistenceService;
import org.opendaylight.tsdr.spi.persistence.TSDRMetricPersistenceService;
import org.opendaylight.tsdr.spi.scheduler.SchedulerService;
import org.opendaylight.tsdr.spi.util.FormatUtil;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.binary.data.rev160325.storetsdrbinaryrecord.input.TSDRBinaryRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides HBase implementation of TSDRPersistenceService.
 *
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 * @author <a href="mailto:syedbahm@cisco.com">Basheeruddin Ahmed </a>
 * @author <a href="mailto:saichler@gmail.com">Sharon Aicler</a>
 * @author Thomas Pantelis
 */
@Singleton
public class TsdrHBasePersistenceServiceImpl implements TSDRLogPersistenceService, TSDRMetricPersistenceService,
        TSDRBinaryPersistenceService {
    private static final Logger LOG = LoggerFactory.getLogger(TsdrHBasePersistenceServiceImpl.class);

    private interface DatabaseOperation {
        void run() throws TableNotFoundException;
    }

    private final SchedulerService schedulerService;
    private final HBaseDataStoreFactory dataStoreFactory;

    @Nonnull
    private volatile CreateTableTask createTableTask;

    /**
     * Constructor.
     */
    @Inject
    public TsdrHBasePersistenceServiceImpl(HBaseDataStoreFactory dataStoreFactory, SchedulerService schedulerService) {
        this.dataStoreFactory = dataStoreFactory;
        this.schedulerService = schedulerService;

        createTableTask = startNewCreateTableTask();

        LOG.info("TSDR HBase Data Store is initialized.");
    }

    @PreDestroy
    public void close() {
        LOG.debug("Entering close");
        closeConnections();
        LOG.debug("Exiting close");
    }

    private CreateTableTask startNewCreateTableTask() {
        long retryInterval = dataStoreFactory.getDataStoreContext().getCreateTableRetryInterval();
        return new CreateTableTask(dataStoreFactory.getHBaseDataStore(), HBasePersistenceUtil.getTsdrHBaseTables(),
                schedulerService, TimeUnit.SECONDS.toMillis(retryInterval)).start();
    }

    private void executeDatabaseOperationWithRetries(DatabaseOperation operation) {
        while (true) {
            try {
                createTableTask.completionFuture().get(15, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                LOG.error("Failure creating hbase tables", e);
                return;
            }

            try {
                operation.run();
                return;
            } catch (TableNotFoundException e) {
                LOG.warn("hbase operation failed - attempting retry", e);

                synchronized (this) {
                    if (createTableTask.completionFuture().isDone()) {
                        LOG.debug("Triggering CreateTableTask");
                        createTableTask = startNewCreateTableTask();
                    }
                }
            }
        }
    }

    private <T extends TSDRRecord> void storeRecords(List<T> recordList, Function<T, HBaseEntity> convertor) {
        if (recordList == null || recordList.isEmpty()) {
            return;
        }

        Map<String, List<HBaseEntity>> entityListMap = new HashMap<>();
        for (T record : recordList) {
            HBaseEntity entity = convertor.apply(record);
            if (entity == null) {
                LOG.debug("the entity is null when converting TSDRMetricRecords into hbase entity");
                return;
            }

            List<HBaseEntity> entityList = entityListMap.get(entity.getTableName());
            if (entityList == null) {
                entityList = new ArrayList<>();
                entityListMap.put(entity.getTableName(), entityList);
            }

            entityList.add(entity);
        }

        executeDatabaseOperationWithRetries(() -> {
            for (List<HBaseEntity> entry: entityListMap.values()) {
                dataStoreFactory.getHBaseDataStore().create(entry);
            }
        });
    }

    /**
     * Store TSDRMetricRecord.
     */
    @Override
    public void storeMetric(TSDRMetricRecord metrics) {
        LOG.debug("Entering store(TSDRMetricRecord)");

        // convert TSDRRecord to HBaseEntities

        HBaseEntity entity = convertToHBaseEntity(metrics);
        if (entity == null) {
            LOG.debug("{} could not be converted to an hbase entity", metrics);
            return;
        }

        executeDatabaseOperationWithRetries(() -> dataStoreFactory.getHBaseDataStore().create(entity));

        LOG.debug("Exiting store(TSDRMetricRecord)");
    }

    /**
     * Store a list of TSDRMetricRecord.
    */
    @Override
    public void storeMetric(List<TSDRMetricRecord> recordList) {
        LOG.debug("Entering store(List<TSDRRecord>)");

        storeRecords(recordList, this::convertToHBaseEntity);

        LOG.debug("Exiting store(List<TSDRRecord>)");
    }

    @Override
    public void storeLog(TSDRLogRecord logRecord) {
        HBaseEntity entity = convertToHBaseEntity(logRecord);
        if (entity == null) {
            LOG.debug("{} could not be converted to an hbase entity", logRecord);
            return;
        }

        executeDatabaseOperationWithRetries(() -> dataStoreFactory.getHBaseDataStore().create(entity));

        LOG.debug("Exiting store(TSDRMetricRecord)");
    }

    /**
     * Store a list of TSDRMetricRecord.
     */
    @Override
    public void storeLog(List<TSDRLogRecord> recordList) {
        LOG.debug("Entering store(List<TSDRRecord>)");

        storeRecords(recordList, this::convertToHBaseEntity);

        LOG.debug("Exiting store(List<TSDRRecord>)");
    }

    /**
     * Retrieve a list of TSDRMetricRecords from HBase data store based on the
     * specified data category, startTime, and endTime.
     */
    @Override
    public List<TSDRMetricRecord> getTSDRMetricRecords(String tsdrMetricKey, long startTime, long endTime) {
        final List<TSDRMetricRecord> resultRecords = new ArrayList<>();
        final List<String> substringFilterList = new ArrayList<>(4);

        if (tsdrMetricKey == null) {
            LOG.error("The tsdr metric key is null");
            return resultRecords;
        }

        //This is getting all data from the hbase table
        List<HBaseEntity> resultEntities = null;
        if (FormatUtil.isDataCategoryKey(tsdrMetricKey)
                || FormatUtil.isDataCategory(tsdrMetricKey)) {
            String dataCategory = FormatUtil.isDataCategoryKey(tsdrMetricKey)
                    ? FormatUtil.getDataCategoryFromTSDRKey(tsdrMetricKey) : tsdrMetricKey;
            resultEntities = dataStoreFactory.getHBaseDataStore().getDataByTimeRange(dataCategory, startTime, endTime);
            for (HBaseEntity e : resultEntities) {
                resultRecords.add(getTSDRMetricRecord(e));
            }
            return resultRecords;
        } else {

            // A valid tsdr metric key does need to contain all the keys but
            // DOES NOT need to contain all the values.
            // e.g. [NID=][DC=PORTSTATS][MN=][RK=] is a valid metric key
            if (!FormatUtil.isValidTSDRKey(tsdrMetricKey)) {
                LOG.error("TSDR Key {} is not in the correct format", tsdrMetricKey);
                return resultRecords;
            }

            String dataCategory = FormatUtil.getDataCategoryFromTSDRKey(tsdrMetricKey);

            // The data category is a mandatory key for hbase as it defines the
            // table name.
            if (!FormatUtil.isDataCategory(dataCategory)) {
                LOG.error("Data Category is unknown {}", dataCategory);
                return resultRecords;
            }

            //Add filter for node id
            String nodeID = FormatUtil.getNodeIdFromTSDRKey(tsdrMetricKey);
            if (!nodeID.isEmpty()) {
                substringFilterList.add("[NID=" + nodeID + "]");
            }

            // Add filter for metric name
            String metricName = FormatUtil.getMetriNameFromTSDRKey(tsdrMetricKey);
            if (!metricName.isEmpty()) {
                substringFilterList.add("[MN=" + metricName + "]");
            }

            // Add filter for record keys
            List<RecordKeys> recKeys = FormatUtil.getRecordKeysFromTSDRKey(tsdrMetricKey);
            String recKeyString = "[RK=";
            if (!recKeys.isEmpty()) {
                StringBuilder buf = new StringBuilder(recKeyString);
                for (RecordKeys recKey : recKeys) {
                    buf.append(recKey.getKeyName()).append(':').append(recKey.getKeyValue()).append(',');
                }
                recKeyString = buf.toString().substring(0, buf.length() - 1) + "]";
                substringFilterList.add(recKeyString);
            }

            resultEntities = dataStoreFactory.getHBaseDataStore().getDataByTimeRange(
                    dataCategory,substringFilterList, startTime, endTime);
            for (HBaseEntity e : resultEntities) {
                resultRecords.add(getTSDRMetricRecord(e));
            }
            return resultRecords;
        }
    }

    /**
     * Retrieve a list of TSDRLogRecords from HBase data store based on the
     * specified data tsdrLogKey, startTime, and endTime.
     */
    @Override
    public List<TSDRLogRecord> getTSDRLogRecords(String tsdrLogKey, long startTime, long endTime) {
        List<HBaseEntity> resultEntities = new ArrayList<>();
        final List<TSDRLogRecord> resultRecords = new ArrayList<>(resultEntities.size());
        final List<String> substringFilterList = new ArrayList<>(4);

        if (tsdrLogKey == null) {
            LOG.error("The data tsdrLogKey is not supported");
            return resultRecords;
        }

        //the tsdr log key is just the data category
        if (FormatUtil.isDataCategoryKey(tsdrLogKey)
                || FormatUtil.isDataCategory(tsdrLogKey)) {
            String dataCategory = FormatUtil.isDataCategoryKey(tsdrLogKey)
                    ? FormatUtil.getDataCategoryFromTSDRKey(tsdrLogKey) : tsdrLogKey;
            resultEntities = dataStoreFactory.getHBaseDataStore().getDataByTimeRange(dataCategory, startTime, endTime);
            for (HBaseEntity e : resultEntities) {
                resultRecords.add(getTSDRLogRecord(e));
            }
            return resultRecords;
        } else {
            // A valid tsdr metric key does need to contain all the keys but
            // DOES NOT need to contain all the values.
            // e.g. [NID=][DC=PORTSTATS][MN=][RK=] is a valid metric key
            if (!FormatUtil.isValidTSDRLogKey(tsdrLogKey)) {
                LOG.error("TSDR Key {} is not in the correct format", tsdrLogKey);
                return resultRecords;
            }

            String dataCategory = FormatUtil.getDataCategoryFromTSDRKey(tsdrLogKey);

            // The data category is a mandatory key for hbase as it defines the
            // table name.
            if (!FormatUtil.isDataCategory(dataCategory)) {
                LOG.error("Data Category is unknown {}", dataCategory);
                return resultRecords;
            }

            // Add filter for node id
            String nodeID = FormatUtil.getNodeIdFromTSDRKey(tsdrLogKey);
            if (!nodeID.isEmpty()) {
                substringFilterList.add("[NID=" + nodeID + "]");
            }

            // Add filter for record keys
            List<RecordKeys> recKeys = FormatUtil.getRecordKeysFromTSDRKey(tsdrLogKey);
            String recKeyString = "[RK=";
            if (!recKeys.isEmpty()) {
                StringBuilder buf = new StringBuilder(recKeyString);
                for (RecordKeys recKey : recKeys) {
                    buf.append(recKey.getKeyName()).append(':').append(recKey.getKeyValue()).append(',');
                }
                recKeyString = buf.toString().substring(0, buf.length() - 1) + "]";
                substringFilterList.add(recKeyString);
            }

            resultEntities = dataStoreFactory.getHBaseDataStore().getDataByTimeRange(dataCategory,
                    substringFilterList, startTime, endTime);
        }
        for (HBaseEntity e : resultEntities) {
            resultRecords.add(getTSDRLogRecord(e));
        }
        return resultRecords;
    }

    @Override
    public void purge(DataCategory category, long retentionTime) {
        try {
            dataStoreFactory.getHBaseDataStore().deleteByTimestamp(category.name(), retentionTime);
        } catch (IOException ioe) {
            LOG.error("Error purging TSDR records in HBase data store {}", ioe);
        }
    }

    @Override
    public void purge(long retentionTime) {
        for (DataCategory category : DataCategory.values()) {
            purge(category, retentionTime);
        }
    }

    /**
     * convert TSDRMetricRecord to HBaseEntity.
     * @return HBaseEntity
    */
    private HBaseEntity convertToHBaseEntity(@Nonnull TSDRMetricRecord metric) {
        LOG.debug("Entering convertToHBaseEntity(TSDRMetricRecord)");

        HBaseEntity entity;
        DataCategory dataCategory = metric.getTSDRDataCategory();
        if (dataCategory != null) {
            entity = HBasePersistenceUtil.getEntityFromMetricStats(metric, dataCategory);
        } else {
            entity = new HBaseEntity();
        }

        LOG.debug("Exiting convertToHBaseEntity(TSDRMetricRecord)");
        return entity;
    }

    /**
     * convert TSDRMetricRecord to HBaseEntity.
     * @return HBaseEntity
     */
    private HBaseEntity convertToHBaseEntity(@Nonnull TSDRLogRecord logRecord) {
        LOG.debug("Entering convertToHBaseEntity(TSDRLogRecord)");

        HBaseEntity entity;
        DataCategory dataCategory = logRecord.getTSDRDataCategory();
        if (dataCategory != null) {
            entity = HBasePersistenceUtil.getEntityFromLogRecord(logRecord, dataCategory);
        } else {
            entity = new HBaseEntity();
        }

        LOG.debug("Exiting convertToHBaseEntity(TSDRLogRecord)");
        return entity;
    }

    /**
     * Close connections to the data store.
     */
    public void closeConnections() {
        LOG.debug("Entering closeConnections()");
        for (String tableName : HBasePersistenceUtil.getTsdrHBaseTables()) {
            dataStoreFactory.getHBaseDataStore().closeConnection(tableName);
        }
        LOG.debug("Exiting closeConnections()");
        return;
    }

    private static TSDRMetricRecord getTSDRMetricRecord(HBaseEntity entity) {
        TSDRMetricRecordBuilder tsdrMetricRecordBuilder = new TSDRMetricRecordBuilder();
        tsdrMetricRecordBuilder.setMetricName(FormatUtil.getMetriNameFromTSDRKey(entity.getRowKey()));
        tsdrMetricRecordBuilder
                .setMetricValue(new BigDecimal(Double.parseDouble(entity.getColumns().get(0).getValue())));
        tsdrMetricRecordBuilder.setNodeID(FormatUtil.getNodeIdFromTSDRKey(entity.getRowKey()));
        tsdrMetricRecordBuilder.setRecordKeys(FormatUtil.getRecordKeysFromTSDRKey(entity.getRowKey()));
        tsdrMetricRecordBuilder.setTimeStamp(FormatUtil.getTimeStampFromTSDRKey(entity.getRowKey()));
        tsdrMetricRecordBuilder.setTSDRDataCategory(DataCategory.valueOf(entity.getTableName()));
        return tsdrMetricRecordBuilder.build();
    }

    private static TSDRLogRecord getTSDRLogRecord(HBaseEntity entity) {
        TSDRLogRecordBuilder tsdrLogRecordBuilder = new TSDRLogRecordBuilder();
        tsdrLogRecordBuilder.setTSDRDataCategory(DataCategory.valueOf(entity.getTableName()));
        tsdrLogRecordBuilder.setTimeStamp(FormatUtil.getTimeStampFromTSDRKey(entity.getRowKey()));
        tsdrLogRecordBuilder.setRecordKeys(FormatUtil.getRecordKeysFromTSDRKey(entity.getRowKey()));
        tsdrLogRecordBuilder.setNodeID(FormatUtil.getNodeIdFromTSDRKey(entity.getRowKey()));
        tsdrLogRecordBuilder.setIndex(-1);
        tsdrLogRecordBuilder.setRecordAttributes(null);
        String fullText = null;
        for (HBaseColumn column : entity.getColumns()) {
            if (column.getColumnQualifier().equalsIgnoreCase(TsdrHBaseDataStoreConstants.LOGRECORD_FULL_TEXT)) {
                fullText = column.getValue();
                break;
            }
        }
        tsdrLogRecordBuilder.setRecordFullText(fullText);
        return tsdrLogRecordBuilder.build();
    }

    @Override
    public void storeBinary(TSDRBinaryRecord binaryRecord) {
        //@TODO - Add code to support binary store
    }

    @Override
    public void storeBinary(List<TSDRBinaryRecord> recordList) {
        //@TODO - Add code to support binary store
    }

    @Override
    public List<TSDRBinaryRecord> getTSDRBinaryRecords(String tsdrBinaryKey, long startTime, long endTime) {
        //@TODO - Add code to collect binary records
        return null;
    }
}
