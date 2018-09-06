/*
 * Copyright (c) 2019 Bell, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.kafka;

import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

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
public class TSDRKafkaPersistenceServiceImpl implements TSDRMetricPersistenceService,TSDRLogPersistenceService,
        TSDRBinaryPersistenceService {
    private static final Logger LOG = LoggerFactory.getLogger(TSDRKafkaPersistenceServiceImpl.class);
    private final KafkaStore producer;

    @Inject
    public TSDRKafkaPersistenceServiceImpl(KafkaStore store) {
        this.producer = store;
        LOG.info("TSDR kafka Store initialized.");
    }

    @Override
    public void storeMetric(TSDRMetricRecord metricRecord) {
        producer.store(metricRecord);
    }

    @Override
    public void storeMetric(List<TSDRMetricRecord> metricRecordList) {
        for (TSDRMetricRecord record : metricRecordList) {
            producer.store(record);
        }
    }

    @Override
    public void storeLog(TSDRLogRecord logRecord) {
        producer.store(logRecord);
    }

    @Override
    public void storeLog(List<TSDRLogRecord> metricRecordList) {
        for (TSDRLogRecord record : metricRecordList) {
            producer.store(record);
        }
    }

    @Override
    public void storeBinary(TSDRBinaryRecord binaryRecord) {
        producer.store(binaryRecord);
    }

    @Override
    public void storeBinary(List<TSDRBinaryRecord> recordList) {
        for (TSDRBinaryRecord record : recordList) {
            producer.store(record);
        }
    }

    @Override
    public void purge(DataCategory category, long retentionTime) {
        LOG.info("purge is not supported for a kafka producer");
    }

    @Override
    public void purge(long retentionTime) {
        LOG.info("purge is not supported for a kafka producer");
    }

    @Override
    public List<TSDRMetricRecord> getTSDRMetricRecords(String tsdrMetricKey, long startDateTime, long endDateTime) {
        LOG.info("getTSDRMetricRecords is not supported for a kafka producer");
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<TSDRLogRecord> getTSDRLogRecords(String tsdrMetricKey, long startTime, long endTime) {
        LOG.info("getTSDRLogRecords is not supported for a kafka producer");
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<TSDRBinaryRecord> getTSDRBinaryRecords(String tsdrBinaryKey, long startTime, long endTime) {
        LOG.info("getTSDRLogRecords is not supported for a kafka producer");
        return Collections.EMPTY_LIST;

    }
}
