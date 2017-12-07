/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.cassandra;

import java.util.Date;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.tsdr.spi.model.TSDRConstants;
import org.opendaylight.tsdr.spi.persistence.TSDRBinaryPersistenceService;
import org.opendaylight.tsdr.spi.persistence.TSDRLogPersistenceService;
import org.opendaylight.tsdr.spi.persistence.TSDRMetricPersistenceService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.binary.data.rev160325.storetsdrbinaryrecord.input.TSDRBinaryRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TSDRCassandraPersistenceServiceImpl implements TSDRMetricPersistenceService,TSDRLogPersistenceService,
        TSDRBinaryPersistenceService {
    private static final Logger LOG = LoggerFactory.getLogger(TSDRCassandraPersistenceServiceImpl.class);
    private final CassandraStore store;

    @Inject
    public TSDRCassandraPersistenceServiceImpl(CassandraStore store) {
        this.store = store;
        LOG.info("TSDR Cassandra Store initialized.");
    }

    @Override
    public void storeMetric(TSDRMetricRecord metricRecord) {
        store.startBatch();
        store.store(metricRecord);
        store.executeBatch();
    }

    @Override
    public void storeMetric(List<TSDRMetricRecord> metricRecordList) {
        store.startBatch();
        for (TSDRMetricRecord record : metricRecordList) {
            store.store(record);
        }
        store.executeBatch();
    }

    @Override
    public void storeLog(TSDRLogRecord logRecord) {
        store.startBatch();
        store.store(logRecord);
        store.executeBatch();
    }

    @Override
    public void storeLog(List<TSDRLogRecord> metricRecordList) {
        store.startBatch();
        for (TSDRLogRecord record : metricRecordList) {
            store.store(record);
        }
        store.executeBatch();
    }

    @Override
    public void storeBinary(TSDRBinaryRecord binaryRecord) {
        store.startBatch();
        store.store(binaryRecord);
        store.executeBatch();
    }

    @Override
    public void storeBinary(List<TSDRBinaryRecord> recordList) {
        store.startBatch();
        for (TSDRBinaryRecord record : recordList) {
            store.store(record);
        }
        store.executeBatch();
    }

    @Override
    public void purge(DataCategory category, long retentionTime) {
        LOG.info("Execute Purge with Category {} and earlier than {}.",category.name(),new Date(retentionTime));
        store.purge(category,retentionTime);
    }

    @Override
    public void purge(long retentionTime) {
        for (DataCategory dataCategory : DataCategory.values()) {
            store.purge(dataCategory,retentionTime);
        }
    }

    @Override
    public List<TSDRMetricRecord> getTSDRMetricRecords(String tsdrMetricKey, long startDateTime, long endDateTime) {
        return store.getTSDRMetricRecords(tsdrMetricKey, startDateTime, endDateTime,
                TSDRConstants.MAX_RESULTS_FROM_LIST_METRICS_COMMAND);
    }

    @Override
    public List<TSDRLogRecord> getTSDRLogRecords(String tsdrMetricKey, long startTime, long endTime) {
        return store.getTSDRLogRecords(tsdrMetricKey, startTime, endTime,
                TSDRConstants.MAX_RESULTS_FROM_LIST_METRICS_COMMAND);
    }

    @Override
    public List<TSDRBinaryRecord> getTSDRBinaryRecords(String tsdrBinaryKey, long startTime, long endTime) {
        return store.getTSDRBinaryRecords(tsdrBinaryKey, startTime, endTime,
                TSDRConstants.MAX_RESULTS_FROM_LIST_METRICS_COMMAND);
    }
}
