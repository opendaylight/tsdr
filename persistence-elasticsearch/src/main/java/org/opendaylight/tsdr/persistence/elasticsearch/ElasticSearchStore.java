/*
 * Copyright (c) 2016 Frinx s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.persistence.elasticsearch;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;
import io.searchbox.action.Action;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.JestResult;
import io.searchbox.client.JestResultHandler;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Bulk;
import io.searchbox.core.BulkResult;
import io.searchbox.core.DeleteByQuery;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.indices.CreateIndex;
import io.searchbox.indices.IndicesExists;
import io.searchbox.indices.mapping.PutMapping;
import io.searchbox.params.Parameters;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.tsdr.spi.util.ConfigFileUtil;
import org.opendaylight.tsdr.spi.util.FormatUtil;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.binary.data.rev160325.storetsdrbinaryrecord.input.TSDRBinaryRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.tsdrlog.RecordAttributes;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A service that handles an elasticsearch data store operation.
 *
 * @author Lukas Beles(lbeles@frinx.io)
 */
class ElasticSearchStore extends AbstractScheduledService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String ELK_QUERY = ""
            + "{\n"
            + "    \"query\": {\n"
            + "        \"filtered\": {\n"
            + "            \"query\": {\n"
            + "                \"query_string\": {\n"
            + "                    \"query\": \"%s\"\n"
            + "                }\n"
            + "            },\n"
            + "            \"filter\": {\n"
            + "                \"range\": {\n"
            + "                   \"TimeStamp\": {\n"
            + "                      \"gte\": %d,\n"
            + "                      \"lte\": %d\n"
            + "                   }\n"
            + "                }\n"
            + "            }\n"
            + "        }\n"
            + "    }\n"
            + "}";
    private static final String QUERY_CONDITION = "%s:\\\"%s\\\"";

    private static final String INDEX = "tsdr";

    /**
     * Enumerates the type of records with their properties.
     */
    enum RecordType {
        METRIC,
        LOG,
        BINARY;

        private final String name;
        private final String mapping;

        RecordType() {
            name = name().toLowerCase();
            String json = null;
            try {
                File file = new File(ConfigFileUtil.CONFIG_DIR + "tsdr-persistence-elasticsearch_"
                        + name + "_mapping.json");
                json = Files.asCharSource(file, StandardCharsets.UTF_8).read();
            } catch (IOException | IllegalArgumentException e) {
                LOGGER.error("Mapping for {} cannot be set: {}", name, e);
                LOGGER.warn("Using the default mapping strategy for {} type "
                        + "that may result it suboptimal types representation", name);
            }
            mapping = json;
        }

        /**
         * Returns a {@link RecordType} for {@link TSDRRecord}.
         * An {@link IllegalArgumentException} is thrown when a record type is unknown.
         */
        public static RecordType resolve(TSDRRecord record) {
            if (record instanceof TSDRMetricRecord) {
                return METRIC;
            } else if (record instanceof TSDRLogRecord) {
                return LOG;
            } else if (record instanceof TSDRBinaryRecord) {
                return BINARY;
            }
            throw new IllegalArgumentException("Unknown record type");
        }
    }

    private final Map<String, String> properties;

    private final Lock batchLock = new ReentrantLock();

    private final EvictingQueue<TSDRRecord> batch = EvictingQueue.create(1 << 10);

    private JestClient client;

    /**
     * Creates a new instance of {@link ElasticSearchStore} backed by the client.
     * If the client is null, then a client based on properties files will
     * be created, setup, and used.
     */
    static ElasticSearchStore create(Map<String, String> properties, JestClient client) {
        return new ElasticSearchStore(checkNotNull(properties), client);
    }

    private ElasticSearchStore(Map<String, String> properties, JestClient client) {
        this.properties = properties;
        this.client = client;
    }

    /**
     * Empty constructor. We use it only because of tests
     */
    @VisibleForTesting
    ElasticSearchStore() {
        properties = null;
    }

    /**
     * Method is a wrapper for {@link JestClient#execute(Action)}
     * in order to avoid repeatedly handle failures.
     */
    private <T extends JestResult> T execute(Action<T> action) {
        try {
            T result = client.execute(action);
            if (result == null) {
                LOGGER.error("Failed to execute action: {}, got null result", action);
                return null;
            }
            if (!result.isSucceeded()) {
                LOGGER.error("Failed to execute action: {}, cause: {}", action, result.getErrorMessage());
            }
            return result;
        } catch (IOException ioe) {
            LOGGER.error("Failed to execute action {}, cause: {}", action, ioe);
        }
        return null;
    }

    /**
     * Method is a wrapper for {@link JestClient#executeAsync(Action, JestResultHandler)}
     * in order to avoid repeatedly handle failures.
     */
    private <T extends JestResult> void executeAsync(final Action<T> action) {
        client.executeAsync(action, new JestResultHandler<JestResult>() {
            @Override
            public void completed(JestResult result) {
                if (result == null) {
                    LOGGER.error("Failed to execute action: {}, got null result", action);
                    return;
                }
                if (!result.isSucceeded()) {
                    LOGGER.error("Failed to execute action: {}, cause: {}", action, result.getErrorMessage());
                }
            }

            @Override
            public void failed(Exception ex) {
                LOGGER.error("Failed to execute action: {}, cause: {}", action, ex);
            }
        });
    }

    /**
     * Writes the batch of {@link RecordType} into the elasticsearch data store.
     */
    private void sync() {
        batchLock.lock();
        try {
            if (!batch.isEmpty()) {
                Bulk.Builder bulk = new Bulk.Builder();
                for (TSDRRecord r : batch) {
                    try {
                        RecordType type = RecordType.resolve(r);
                        bulk.addAction(new Index.Builder(r).index(INDEX).type(type.name).build());
                    } catch (IllegalArgumentException iae) {
                        LOGGER.error("Cannot resolve type: {}, {}", r, iae);
                    }
                }
                BulkResult result = execute(bulk.build());
                if (result != null && result.isSucceeded()) {
                    batch.clear();
                }
            }
        } finally {
            batchLock.unlock();
        }
    }

    /**
     * Stores the given record.
     * A {@link NullPointerException} is thrown if record is {@code null}.
     * An {@link IllegalStateException} is thrown if this service is not running.
     */
    void store(TSDRRecord record) {
        checkNotNull(record);
        checkState(isRunning(), "The service is not running");

        batchLock.lock();
        try {
            if (batch.remainingCapacity() == 0) {
                sync();
            }
            batch.add(record);
        } finally {
            batchLock.unlock();
        }
    }

    /**
     * Stores the given chunk of records.
     * A {@link NullPointerException} is thrown if record is {@code null}.
     * An {@link IllegalStateException} is thrown if this service is not running.
     */
    void storeAll(List<? extends TSDRRecord> records) {
        checkNotNull(records);

        // check whether content of the list doesn't contain a null value.
        for (TSDRRecord record : records) {
            checkNotNull(record);
        }
        checkState(isRunning(), "The service is not running");

        batchLock.lock();
        try {
            if (batch.remainingCapacity() < records.size()) {
                sync();
            }
            batch.addAll(records);
        } finally {
            batchLock.unlock();
        }
    }

    /**
     * Searches for in a given type for key bounded by start and stop timestamps.
     * A {@link NullPointerException} is thrown if record is {@code null}.
     * An {@link IllegalStateException} is thrown if this service is not running.
     */
    @SuppressWarnings("unchecked")
    <T extends TSDRRecord> List<T> search(RecordType type, String key, long start, long end, int size) {
        checkNotNull(type);
        checkNotNull(key);
        checkState(isRunning(), "The service is not running");

        if (end < start) {
            return Collections.emptyList();
        }
        String query = buildELKQuery(type, key, start, end);
        SearchResult result = execute(new Search.Builder(query)
                .addIndex(INDEX)
                .addType(type.name)
                .setParameter(Parameters.SIZE, size)
                .build());
        if (result == null || !result.isSucceeded() || result.getTotal() == 0) {
            return Collections.emptyList();
        }
        return result.getHits(TsdrRecordPayload.class).stream()
                .map(hit -> (T) hit.source.toRecord(type))
                .collect(Collectors.toList());
    }

    /**
     * Create ELK Query.
     */
    private String buildELKQuery(RecordType type, String tsdrKey, long start, long end) {
        String queryString = buildQueryString(type, tsdrKey);
        String query = String.format(
                ElasticSearchStore.ELK_QUERY,
                queryString,
                start,
                Math.min(end, 9999999999999L));
        LOGGER.info("The Query is {}", query);
        return query;
    }

    /**
     * Create queryString of the ELK query.
     */
    String buildQueryString(RecordType type, String tsdrKey) {
        StringBuffer queryBuffer = new StringBuffer();
        appendCondition(queryBuffer, TsdrRecordPayload.ELK_DATA_CATEGORY, resolveDataCategory(tsdrKey));

        try {
            Long timestamp = FormatUtil.getTimeStampFromTSDRKey(tsdrKey);
            if (timestamp != null) {
                appendCondition(queryBuffer, TsdrRecordPayload.ELK_TIMESTAMP, String.valueOf(timestamp));
            }
        } catch (NumberFormatException e) {
            // do nothing, timestamp is not in query
        }

        if (type == RecordType.METRIC) {
            appendCondition(queryBuffer, TsdrRecordPayload.ELK_NODE_ID, FormatUtil.getNodeIdFromTSDRKey(tsdrKey));
            appendCondition(queryBuffer, TsdrRecordPayload.ELK_METRIC_NAME,
                    FormatUtil.getMetriNameFromTSDRKey(tsdrKey));
            List<RecordKeys> recKeys = FormatUtil.getRecordKeysFromTSDRKey(tsdrKey);
            if (recKeys != null) {
                for (RecordKeys recKey : recKeys) {
                    appendCondition(queryBuffer, TsdrRecordPayload.ELK_RK_KEY_NAME, recKey.getKeyName());
                    appendCondition(queryBuffer, TsdrRecordPayload.ELK_RK_KEY_VALUE, recKey.getKeyValue());
                }
            }
        }

        if (type == RecordType.LOG) {
            List<RecordAttributes> recAttrs = FormatUtil.getRecordAttributesFromTSDRKey(tsdrKey);
            if (recAttrs != null) {
                for (RecordAttributes recAttr : recAttrs) {
                    appendCondition(queryBuffer, TsdrRecordPayload.ELK_RA_KEY_NAME, recAttr.getName());
                    appendCondition(queryBuffer, TsdrRecordPayload.ELK_RA_KEY_VALUE, recAttr.getValue());
                }
            }
        }


        return queryBuffer.toString();
    }

    /**
     * Create an one part of condition queryString of the ELK query.
     */
    void appendCondition(StringBuffer queryBuffer, String fieldName, String fieldValue) {
        if (StringUtils.isNoneEmpty(fieldValue)) {
            // If it is not the first condition then we must add clausule And
            if (queryBuffer.length() > 0) {
                queryBuffer.append(" AND ");
            }
            queryBuffer.append(String.format(QUERY_CONDITION, fieldName, fieldValue));
        }
    }

    /**
     * Resolve TSDR data category from the String.
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    private String resolveDataCategory(String key) {
        String dataCategory = FormatUtil.getDataCategoryFromTSDRKey(key);
        if (dataCategory == null) {
            try {
                DataCategory dc = DataCategory.valueOf(key);
                dataCategory = dc.name();
            } catch (RuntimeException e) {
                LOGGER.error("TSDR Metric Key {} is not a DataCategory", key);
            }
        }
        return dataCategory;
    }

    /**
     * Deletes all records with given category and older than the retention timestamp.
     * A {@link NullPointerException} is thrown if record is {@code null}.
     * An {@link IllegalStateException} is thrown if this service is not running.
     */
    void delete(final DataCategory category, long timestamp) {
        checkNotNull(category);
        checkState(isRunning(), "The service is not running");

        String query = String.format(
                ElasticSearchStore.ELK_QUERY,
                category,
                0,
                Math.min(timestamp - 1, 9999999999999L));
        executeAsync(new DeleteByQuery.Builder(query).addIndex(INDEX).build());
    }

    /**
     * Returns a copy of {@link TSDRRecord} batch which will be synced to the data store.
     */
    List<TSDRRecord> getBatch() {
        batchLock.lock();
        try {
            return Lists.newArrayList(batch);
        } finally {
            batchLock.unlock();
        }
    }

    /**
     * Built a client configuration also base on properties file.
     */
    private HttpClientConfig buildClientConfig() throws IOException {
        GsonBuilder gson = new GsonBuilder()
                .setFieldNamingStrategy(field -> {
                    String name = FieldNamingPolicy.UPPER_CAMEL_CASE.translateName(field);
                    if (name.startsWith("_")) {
                        return name.substring(1);
                    }
                    return name;
                })
                .setExclusionStrategies(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes fieldAttributes) {
                        String name = fieldAttributes.getName().toLowerCase();
                        return name.equals("hash") || name.equals("hashvalid");
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> clazz) {
                        return false;
                    }
                });

        String serverUrl = properties.get("serverUrl");
        HttpClientConfig.Builder configBuilder = new HttpClientConfig
                .Builder(serverUrl)
                .multiThreaded(true)
                .gson(gson.create());

        if (Boolean.valueOf(properties.get("nodeDiscovery"))) {
            configBuilder.discoveryEnabled(true).discoveryFrequency(1L, TimeUnit.MINUTES);
        }

        String username = properties.get("username");
        String password = properties.get("password");
        if (!Strings.isNullOrEmpty(username) && !Strings.isNullOrEmpty(password)) {
            configBuilder.defaultCredentials(username, password);
        }

        return configBuilder.build();
    }

    /**
     * Setup elasticsearch data storage index, related types, and mappings.
     */
    private void setupStorage(boolean indexExists) throws IOException {
        // Create an index if it doesn't exist.
        if (!indexExists) {
            execute(new CreateIndex.Builder(INDEX).build());
        }

        // Setup mappings for types.
        for (RecordType type : RecordType.values()) {
            if (!Strings.isNullOrEmpty(type.mapping)) {
                execute(new PutMapping.Builder(
                        INDEX,
                        type.name,
                        type.mapping).build());
            }
        }
    }

    /**
     * Setup the connection and properties of the elasticsearch data store.
     */
    @Override
    protected void startUp() throws Exception {
        if (client != null) {
            return;
        }

        client = createJestClient();
        IndicesExists action = new IndicesExists.Builder(INDEX).build();
        while (state() == State.STARTING) {
            JestResult result = execute(action);
            if (result == null) {
                LOGGER.warn("Setting up elasticsearch data store failed, next retry in 10s");
                TimeUnit.SECONDS.sleep(10L);
                continue;
            }
            setupStorage(result.isSucceeded());
            LOGGER.info("Elasticsearch data store was setup successfully");
            return;
        }
    }

    /**
     * Create Jest client according properties from config file.
     */
    JestClient createJestClient() throws IOException {
        HttpClientConfig config = buildClientConfig();
        JestClientFactory factory = new JestClientFactory();
        factory.setHttpClientConfig(config);
        return factory.getObject();
    }


    /**
     * Gracefully shutdown the connection to the elasticsearch data store.
     */
    @Override
    protected void shutDown() throws Exception {
        sync();
        if (client != null) {
            client.shutdownClient();
        }
    }

    /**
     * Periodically synchronize the {@link #batch} to the elasticsearch data store.
     */
    @Override
    protected void runOneIteration() throws Exception {
        sync();
    }

    /**
     * Returns a scheduler which will periodically trigger the {@link #sync()} method to run.
     */
    @Override
    protected Scheduler scheduler() {
        long delay = 1L;
        if (properties.containsKey("syncInterval")) {
            delay = Math.max(Long.valueOf(properties.get("syncInterval")), delay);
        }
        return Scheduler.newFixedDelaySchedule(0L, delay, TimeUnit.SECONDS);
    }
}
