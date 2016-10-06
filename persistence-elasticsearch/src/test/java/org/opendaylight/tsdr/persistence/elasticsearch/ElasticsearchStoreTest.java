/*
 * Copyright (c) 2016 Frinx s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.persistence.elasticsearch;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Service;
import com.google.gson.Gson;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestResultHandler;
import io.searchbox.core.Bulk;
import io.searchbox.core.BulkResult;
import io.searchbox.core.DeleteByQuery;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.binary.data.rev160325.storetsdrbinaryrecord.input.TSDRBinaryRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRRecord;

/**
 * Test all methods of {@link ElasticsearchStore}
 *
 * @author Lukas Beles(lbeles@frinx.io)
 */
public class ElasticsearchStoreTest {

    private static final BulkResult EMPTY_BULK_RESULT = new BulkResult(new Gson());
    private static final SearchResult EMPTY_SEARCH_RESULT = new SearchResult(new Gson());
    private static final Map<String, String> PROPERTIES = ImmutableMap.of(
            "startTimeout", "1",
            "stopTimeout", "1",
            "syncInterval", "1");
    private static final JestClient CLIENT = Mockito.mock(JestClient.class);
    private static ElasticsearchStore store;

    /**
     * Test setUp method, common scenario.
     *
     * @throws Exception
     */
    @BeforeClass
    public static void setUp() throws Exception {
        store = ElasticsearchStore.create(PROPERTIES, CLIENT);
        store.startAsync().awaitRunning(1, TimeUnit.SECONDS);
    }

    /**
     * Test tearDown method, common scenario.
     *
     * @throws Exception
     */
    @AfterClass
    public static void tearDown() throws Exception {
        store.stopAsync().awaitTerminated(1, TimeUnit.SECONDS);
    }

    /**
     * Test store method, where value is {@link TSDRMetricRecord}. Check whether batch contains the value.
     *
     * @throws Exception
     */
    @Test
    public void storeMetricRecord() throws Exception {
        Mockito.doReturn(EMPTY_BULK_RESULT).when(CLIENT).execute(Mockito.any(Bulk.class));
        TSDRMetricRecord record = TsdrRecordFactory.createMetricRecord();
        store.store(record);
        assertThat(store.getBatch()).contains(record);
    }

    /**
     * Test store method, where value is {@link TSDRLogRecord}. Check whether batch contains the value.
     *
     * @throws Exception
     */
    @Test
    public void storeLogRecord() throws Exception {
        Mockito.doReturn(EMPTY_BULK_RESULT).when(CLIENT).execute(Mockito.any(Bulk.class));
        TSDRLogRecord record = TsdrRecordFactory.createLogRecord();
        store.store(record);
        assertThat(store.getBatch()).contains(record);
    }

    /**
     * Test store method, where value is {@link TSDRBinaryRecord}. Check whether batch contains the value.
     *
     * @throws Exception
     */
    @Test
    public void storeBinaryRecord() throws Exception {
        Mockito.doReturn(EMPTY_BULK_RESULT).when(CLIENT).execute(Mockito.any(Bulk.class));
        TSDRBinaryRecord record = TsdrRecordFactory.createBinaryRecord();
        store.store(record);
        assertThat(store.getBatch()).contains(record);
    }

    /**
     * Test store method, where value is null
     *
     * @throws Exception
     */
    @Test(expected = NullPointerException.class)
    public void storeNull() throws Exception {
        store.store(null);
    }

    /**
     * Test storeAll method. The collection contains one {@link TSDRMetricRecord} and one {@link TSDRLogRecord}.
     *
     * @throws Exception
     */
    @Test
    public void storeAll() throws Exception {
        ArrayList<TSDRRecord> records = Lists.newArrayList(
                TsdrRecordFactory.createMetricRecord(),
                TsdrRecordFactory.createLogRecord());
        store.storeAll(records);
        assertThat(store.getBatch()).isNotEmpty();
        assertThat(store.getBatch()).containsAllIn(records);
    }

    /**
     * Test storeAll method, where value is null.
     *
     * @throws Exception
     */
    @Test(expected = NullPointerException.class)
    public void storeAllNullRecord() throws Exception {
        store.storeAll(null);
    }

    /**
     * Test storeAll method. Collection contains null record.
     *
     * @throws Exception
     */
    @Test(expected = NullPointerException.class)
    public void storeAllRecordListHasNullRecord() throws Exception {
        List records = Collections.singletonList(null);
        store.storeAll(records);
    }

    /**
     * Test search method. Test whether result is empty. It is expected. And verify whether method execute of {@link JestClient} was called at least one.
     *
     * @throws Exception
     */
    @Test
    public void search() throws Exception {
        Mockito.doReturn(EMPTY_SEARCH_RESULT).when(CLIENT).execute(Mockito.any(Search.class));
        List<TSDRRecord> result = store.search(
                ElasticsearchStore.RecordType.METRIC,
                DataCategory.EXTERNAL.name(),
                1L,
                0L,
                1000);
        assertThat(result).isEmpty();

        result = store.search(
                ElasticsearchStore.RecordType.METRIC,
                DataCategory.EXTERNAL.name(),
                0L,
                0L,
                1000);
        assertThat(result).isEqualTo(Collections.emptyList());
        Mockito.verify(CLIENT, Mockito.atLeastOnce()).execute(Mockito.any(Bulk.class));
    }

    /**
     * Test appendCondition method. Test all possible ways of the condition
     *
     * @throws Exception
     */
    @Test
    public void appendCondition() throws Exception {
        StringBuffer buffer = new StringBuffer();

        store.appendCondition(buffer, "empty1", null);
        assertThat(buffer.toString()).isEqualTo("");

        store.appendCondition(buffer, "empty2", null);
        assertThat(buffer.toString()).isEqualTo("");

        store.appendCondition(buffer, "field1", "value1");
        assertThat(buffer.toString()).isEqualTo("field1:\\\"value1\\\"");

        store.appendCondition(buffer, "field2", "value2");
        assertThat(buffer.toString()).isEqualTo("field1:\\\"value1\\\" AND field2:\\\"value2\\\"");
    }

    /**
     * Test of building query String for Binary request
     *
     * @throws Exception
     */
    @Test
    public void buildQueryStringOfBinary() throws Exception {
        String result = store.buildQueryString(ElasticsearchStore.RecordType.BINARY, "FLOWSTATS");
        assertThat(result.contains(TsdrRecordPayload.ELK_DATA_CATEGORY + ":\\\"FLOWSTATS\\\"")).isTrue();

        result = store.buildQueryString(ElasticsearchStore.RecordType.BINARY, "[NID=][DC=FLOWSTATS][RK=][MN=]");
        assertThat(result.contains(TsdrRecordPayload.ELK_DATA_CATEGORY + ":\\\"FLOWSTATS\\\"")).isTrue();

        result = store.buildQueryString(ElasticsearchStore.RecordType.BINARY, "[NID=][DC=FLOWSTATS][RK=][MN=PacketCount]");
        assertThat(result.contains(TsdrRecordPayload.ELK_DATA_CATEGORY + ":\\\"FLOWSTATS\\\"")).isTrue();
        assertThat(result.contains("PacketCount")).isFalse();
    }

    /**
     * Test of building query String for Log request
     *
     * @throws Exception
     */
    @Test
    public void buildQueryStringOfLog() throws Exception {
        String result = store.buildQueryString(ElasticsearchStore.RecordType.LOG, "FLOWSTATS");
        assertThat(result.contains(TsdrRecordPayload.ELK_DATA_CATEGORY + ":\\\"FLOWSTATS\\\"")).isTrue();

        result = store.buildQueryString(ElasticsearchStore.RecordType.LOG, "[NID=][DC=FLOWSTATS][RK=][MN=]");
        assertThat(result.contains(TsdrRecordPayload.ELK_DATA_CATEGORY + ":\\\"FLOWSTATS\\\"")).isTrue();

        result = store.buildQueryString(ElasticsearchStore.RecordType.LOG, "[NID=][DC=FLOWSTATS][RK=][MN=PacketCount]");
        assertThat(result.contains(TsdrRecordPayload.ELK_DATA_CATEGORY + ":\\\"FLOWSTATS\\\"")).isTrue();
        assertThat(result.contains("PacketCount")).isFalse();

        result = store.buildQueryString(ElasticsearchStore.RecordType.LOG, "[NID=][DC=FLOWSTATS][RK=][RA=key:value]");
        assertThat(result.contains(TsdrRecordPayload.ELK_DATA_CATEGORY + ":\\\"FLOWSTATS\\\"")).isTrue();
        assertThat(result.contains(TsdrRecordPayload.ELK_RA_KEY_NAME + ":\\\"key\\\"")).isTrue();
        assertThat(result.contains(TsdrRecordPayload.ELK_RA_KEY_VALUE + ":\\\"value\\\"")).isTrue();
    }

    /**
     * Test of building query String for Metric request
     *
     * @throws Exception
     */
    @Test
    public void buildQueryStringOfMetric() throws Exception {
        String result = store.buildQueryString(ElasticsearchStore.RecordType.METRIC, "FLOWSTATS");
        assertThat(result.contains(TsdrRecordPayload.ELK_DATA_CATEGORY)).isTrue();
        assertThat(result.contains(TsdrRecordPayload.ELK_DATA_CATEGORY + ":\\\"FLOWSTATS\\\"")).isTrue();

        result = store.buildQueryString(ElasticsearchStore.RecordType.METRIC, "[NID=][DC=FLOWSTATS][RK=][MN=]");
        assertThat(result.contains(TsdrRecordPayload.ELK_DATA_CATEGORY + ":\\\"FLOWSTATS\\\"")).isTrue();

        result = store.buildQueryString(ElasticsearchStore.RecordType.METRIC, "[NID=node][DC=FLOWSTATS][MN=PacketCount][RK=key:value]");
        assertThat(result.contains(TsdrRecordPayload.ELK_DATA_CATEGORY + ":\\\"FLOWSTATS\\\"")).isTrue();
        assertThat(result.contains(TsdrRecordPayload.ELK_METRIC_NAME + ":\\\"PacketCount\\\"")).isTrue();
        assertThat(result.contains(TsdrRecordPayload.ELK_NODE_ID + ":\\\"node\\\"")).isTrue();
        assertThat(result.contains(TsdrRecordPayload.ELK_RK_KEY_NAME + ":\\\"key\\\"")).isTrue();
        assertThat(result.contains(TsdrRecordPayload.ELK_RK_KEY_VALUE + ":\\\"value\\\"")).isTrue();
    }


    /**
     * Test delete method. Verify whether method executeAsync of {@link JestClient} was called at least one.
     *
     * @throws Exception
     */
    @Test
    @SuppressWarnings("unchecked")
    public void delete() throws Exception {
        store.delete(DataCategory.EXTERNAL, 0L);
        Mockito.verify(CLIENT, Mockito.atLeastOnce()).executeAsync(
                Mockito.any(DeleteByQuery.class),
                Mockito.any(JestResultHandler.class));
    }

    /**
     * Test start and then stop service
     *
     * @throws Exception
     */
    @Test
    public void createAndStartAndShutDown() throws Exception {
        ElasticsearchStore store = ElasticsearchStore.create(PROPERTIES, CLIENT);
        assertThat(store.state()).isEqualTo(Service.State.NEW);
        store.startAsync().awaitRunning(2, TimeUnit.SECONDS);
        assertThat(store.state()).isEqualTo(Service.State.RUNNING);
        store.stopAsync().awaitTerminated(2, TimeUnit.SECONDS);
        assertThat(store.state()).isEqualTo(Service.State.TERMINATED);
    }

    /**
     * Test store record and sync method. Verify whether method execute of {@link JestClient} was called at least one.
     *
     * @throws Exception
     */
    @Test
    public void runOneIteration() throws Exception {
        TSDRMetricRecord record = TsdrRecordFactory.createMetricRecord();
        Mockito.doReturn(null).when(CLIENT).execute(Mockito.any(Bulk.class));
        store.store(record);
        store.runOneIteration();
        Mockito.verify(CLIENT, Mockito.atLeastOnce()).execute(Mockito.any(Bulk.class));
        Mockito.doThrow(IOException.class).when(CLIENT).execute(Mockito.any(Bulk.class));
        store.store(record);
        store.runOneIteration();
        Mockito.verify(CLIENT, Mockito.atLeastOnce()).execute(Mockito.any(Bulk.class));
    }

    /**
     * Test whether scheduler is implemented
     *
     * @throws Exception
     */
    @Test
    public void scheduler() throws Exception {
        assertThat(store.scheduler()).isNotNull();
    }

    /**
     * Test service with wrong state
     *
     * @throws Exception
     */
    @Test(expected = IllegalStateException.class)
    public void unknownClientConfiguration() throws Exception {
        ElasticsearchStore store = ElasticsearchStore.create(PROPERTIES, null);
        store.startAsync().awaitRunning();
    }

    /**
     * Test method resolve of {@link org.opendaylight.tsdr.persistence.elasticsearch.ElasticsearchStore.RecordType}, when argument is null
     *
     * @throws Exception
     */
    @Test(expected = IllegalArgumentException.class)
    public void resolveUnknownRecordType() throws Exception {
        ElasticsearchStore.RecordType.resolve(null);
    }

    /**
     * Test starup service
     *
     * @throws Exception
     */
    @Test
    public void startup() throws Exception {
        ElasticsearchStore store = Mockito.spy(ElasticsearchStore.class);
        Mockito.doReturn(null).when(store).createJestClient();
        store.startUp();
    }
}