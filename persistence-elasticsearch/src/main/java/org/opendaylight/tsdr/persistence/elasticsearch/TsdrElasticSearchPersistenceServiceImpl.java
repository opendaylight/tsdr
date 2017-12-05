/*
 * Copyright (c) 2016 Frinx s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.persistence.elasticsearch;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.opendaylight.tsdr.persistence.elasticsearch.ElasticSearchStore.RecordType;
import org.opendaylight.tsdr.spi.model.TSDRConstants;
import org.opendaylight.tsdr.spi.persistence.TSDRBinaryPersistenceService;
import org.opendaylight.tsdr.spi.persistence.TSDRLogPersistenceService;
import org.opendaylight.tsdr.spi.persistence.TSDRMetricPersistenceService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.binary.data.rev160325.storetsdrbinaryrecord.input.TSDRBinaryRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of Elasticsearch data store for TSDR.
 *
 * @author Lukas Beles(lbeles@frinx.io)
 */
class TsdrElasticSearchPersistenceServiceImpl implements TSDRMetricPersistenceService, TSDRLogPersistenceService,
        TSDRBinaryPersistenceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ElasticSearchStore store;

    /**
     * Returns a new instance of {@link TsdrElasticSearchPersistenceServiceImpl} with given data store.
     */
    static TsdrElasticSearchPersistenceServiceImpl create(ElasticSearchStore store) {
        return new TsdrElasticSearchPersistenceServiceImpl(store);
    }

    private TsdrElasticSearchPersistenceServiceImpl(ElasticSearchStore store) {
        this.store = store;
    }

    private void store(TSDRRecord record) {
        try {
            store.store(record);
        } catch (IllegalStateException ise) {
            LOGGER.error("Cannot store the record: {}, cause: {}", record, ise);
        }
    }

    private void storeAll(List<? extends TSDRRecord> records) {
        try {
            store.storeAll(records);
        } catch (IllegalStateException ise) {
            LOGGER.error("Cannot store the records: {}, cause: {}", records.toArray(), ise);
        }
    }

    private <T extends TSDRRecord> List<T> getTSDRRecords(RecordType type, String key, long start, long end) {
        try {
            return store.search(
                    type,
                    key,
                    start,
                    end,
                    TSDRConstants.MAX_RESULTS_FROM_LIST_METRICS_COMMAND);
        } catch (IllegalStateException iae) {
            LOGGER.error("Cannot retrieve the records: {}", iae);
        }
        return Collections.emptyList();
    }

    @Override
    public void storeMetric(TSDRMetricRecord metricRecord) {
        store(metricRecord);
    }

    @Override
    public void storeMetric(List<TSDRMetricRecord> recordList) {
        storeAll(recordList);
    }

    @Override
    public List<TSDRMetricRecord> getTSDRMetricRecords(String key, long start, long end) {
        return getTSDRRecords(RecordType.METRIC, key, start, end);
    }

    @Override
    public void storeLog(TSDRLogRecord logRecord) {
        store(logRecord);
    }

    @Override
    public void storeLog(List<TSDRLogRecord> recordList) {
        storeAll(recordList);
    }

    @Override
    public List<TSDRLogRecord> getTSDRLogRecords(String key, long start, long end) {
        return getTSDRRecords(RecordType.LOG, key, start, end);
    }

    @Override
    public void storeBinary(TSDRBinaryRecord binaryRecord) {
        store(binaryRecord);
    }

    @Override
    public void storeBinary(List<TSDRBinaryRecord> recordList) {
        storeAll(recordList);
    }

    @Override
    public List<TSDRBinaryRecord> getTSDRBinaryRecords(String key, long start, long end) {
        return getTSDRRecords(RecordType.BINARY, key, start, end);
    }

    @Override
    public void purge(long timestamp) {
        // TODO: rewrite this to a more effective version, by using one bulk delete instead of several separate calls.
        for (DataCategory category : DataCategory.values()) {
            purge(category, timestamp);
        }
    }

    @Override
    public void purge(DataCategory category, long timestamp) {
        LOGGER.info("Purging data Category {} earlier than {}.", category.name(), new Date(timestamp));
        try {
            store.delete(category, timestamp);
        } catch (IllegalStateException iae) {
            LOGGER.error("Cannot retrieve the records: {}", iae);
        }
    }
}
