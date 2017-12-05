/*
 * Copyright (c) 2016 Frinx s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.persistence.elasticsearch;

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.binary.data.rev160325.storetsdrbinaryrecord.input.TSDRBinaryRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRRecord;

/**
 * Test methods of {@link TsdrElasticSearchPersistenceServiceImpl}.
 *
 * @author Lukas Beles(lbeles@frinx.io)
 */
public class TsdrElasticSearchPersistenceServiceImplTest {

    private final ElasticSearchStore store = Mockito.mock(ElasticSearchStore.class);
    private final TsdrElasticSearchPersistenceServiceImpl service =
            TsdrElasticSearchPersistenceServiceImpl.create(store);

    /**
     * Test storing a metric record. Verify whether ElasticsearchStore.store was called.
     */
    @Test
    public void storeMetricRecord() throws Exception {
        TSDRMetricRecord record = TsdrRecordFactory.createMetricRecord();
        service.storeMetric(record);
        Mockito.verify(store, Mockito.only()).store(record);
        Mockito.doThrow(IllegalStateException.class).when(store).store(Mockito.any(TSDRRecord.class));
        service.storeMetric(record);
    }

    /**
     * Test storing a log record. Verify whether ElasticsearchStore.store was called.
     */
    @Test
    public void storeLogRecord() {
        TSDRLogRecord record = TsdrRecordFactory.createLogRecord();
        service.storeLog(record);
        Mockito.verify(store, Mockito.only()).store(record);
        Mockito.doThrow(IllegalStateException.class).when(store).store(Mockito.any(TSDRRecord.class));
        service.storeLog(record);
    }

    /**
     * Test storing a binary record. Verify whether ElasticsearchStore.store was called.
     */
    @Test
    public void storeBinaryRecord() {
        TSDRBinaryRecord record = TsdrRecordFactory.createBinaryRecord();
        service.storeBinary(record);
        Mockito.verify(store, Mockito.only()).store(record);
        Mockito.doThrow(IllegalStateException.class).when(store).store(Mockito.any(TSDRRecord.class));
        service.storeBinary(record);
    }

    /**
     * Test storing a list of the Metrics records. Verify whether ElasticsearchStore.storeAll was called.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void storeListOfMetricRecords() throws Exception {
        List<TSDRMetricRecord> records = Lists.newArrayList(TsdrRecordFactory.createMetricRecord());
        service.storeMetric(records);
        Mockito.verify(store, Mockito.only()).storeAll(records);
        Mockito.doThrow(IllegalStateException.class).when(store).storeAll(Mockito.any(List.class));
        service.storeMetric(records);
    }

    /**
     * Test storing a list of the Log records. Verify whether ElasticsearchStore.storeAll was called.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void storeListOfLogRecords() throws Exception {
        List<TSDRLogRecord> records = Lists.newArrayList(TsdrRecordFactory.createLogRecord());
        service.storeLog(records);
        Mockito.verify(store, Mockito.only()).storeAll(records);
        Mockito.doThrow(IllegalStateException.class).when(store).storeAll(Mockito.any(List.class));
        service.storeLog(records);
    }

    /**
     * Test storing a list of the Binary records. Verify whether ElasticsearchStore.storeAll was called.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void storeListOfBinaryRecords() throws Exception {
        List<TSDRBinaryRecord> records = Lists.newArrayList(TsdrRecordFactory.createBinaryRecord());
        service.storeBinary(records);
        Mockito.verify(store, Mockito.only()).storeAll(records);
        Mockito.doThrow(IllegalStateException.class).when(store).storeAll(Mockito.any(List.class));
        service.storeBinary(records);
    }

    /**
     * Test searching the metric records. Verify whether ElasticsearchStore.search was called.
     */
    @Test
    public void getTSDRMetricRecords() throws Exception {
        long start = 0L;
        long end = 0L;
        int size = 1000;
        String key = DataCategory.EXTERNAL.name();
        ElasticSearchStore.RecordType type = ElasticSearchStore.RecordType.METRIC;
        service.getTSDRMetricRecords(key, start, end);
        Mockito.verify(store, Mockito.only()).search(type, key, start, end, size);
        Mockito.doThrow(IllegalStateException.class).when(store).search(
                Mockito.any(ElasticSearchStore.RecordType.class),
                Mockito.anyString(),
                Mockito.anyLong(),
                Mockito.anyLong(),
                Mockito.anyInt());
        service.getTSDRMetricRecords(key, start, end);
    }

    /**
     * Test searching the log records. Verify whether ElasticsearchStore.search was called.
     */
    @Test
    public void getTSDRLogRecords() throws Exception {
        long start = 0L;
        long end = 0L;
        int size = 1000;
        String key = DataCategory.EXTERNAL.name();
        ElasticSearchStore.RecordType type = ElasticSearchStore.RecordType.LOG;
        service.getTSDRLogRecords(key, start, end);
        Mockito.verify(store, Mockito.only()).search(type, key, start, end, size);
        Mockito.doThrow(IllegalStateException.class).when(store).search(
                Mockito.any(ElasticSearchStore.RecordType.class),
                Mockito.anyString(),
                Mockito.anyLong(),
                Mockito.anyLong(),
                Mockito.anyInt());
        service.getTSDRLogRecords(key, start, end);
    }

    /**
     * Test searching the binary records. Verify whether ElasticsearchStore.search was called.
     */
    @Test
    public void getTSDRBinaryRecords() throws Exception {
        long start = 0L;
        long end = 0L;
        int size = 1000;
        String key = DataCategory.EXTERNAL.name();
        ElasticSearchStore.RecordType type = ElasticSearchStore.RecordType.BINARY;
        service.getTSDRBinaryRecords(key, start, end);
        Mockito.verify(store, Mockito.only()).search(type, key, start, end, size);
        Mockito.doThrow(IllegalStateException.class).when(store).search(
                Mockito.any(ElasticSearchStore.RecordType.class),
                Mockito.anyString(),
                Mockito.anyLong(),
                Mockito.anyLong(),
                Mockito.anyInt());
        service.getTSDRBinaryRecords(key, start, end);
    }

    /**
     * Test delete a record. Verify whether ElasticsearchStore.delete was called.
     */
    @Test
    public void purge() throws Exception {
        long until = 0L;
        DataCategory category = DataCategory.QUEUESTATS;
        service.purge(category, until);
        Mockito.verify(store, Mockito.only()).delete(category, until);
        Mockito.doThrow(IllegalStateException.class).when(store).delete(
                Mockito.any(DataCategory.class),
                Mockito.anyLong());
        service.purge(category, until);

    }

    /**
     * Test deleting all records. Verify whether ElasticsearchStore.deleteAll was called.
     */
    @Test
    public void purgeAll() throws Exception {
        long until = 0L;
        service.purge(until);
        for (DataCategory category : DataCategory.values()) {
            Mockito.verify(store, Mockito.atLeastOnce()).delete(category, until);
        }
    }
}
