/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.cassandra;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.RegularStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SimpleStatement;
import com.datastax.driver.core.exceptions.AuthenticationException;
import com.datastax.driver.core.exceptions.InvalidQueryException;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.opendaylight.tsdr.spi.util.FormatUtil;
import org.opendaylight.tsdr.spi.util.TSDRKeyCache;
import org.opendaylight.tsdr.spi.util.TSDRKeyCache.TSDRCacheEntry;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.binary.data.rev160325.storetsdrbinaryrecord.input.TSDRBinaryRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.binary.data.rev160325.storetsdrbinaryrecord.input.TSDRBinaryRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The back-end Cassandra store.
 *
 * @author Sharon Aicler(saichler@gmail.com)
 */
public class CassandraStore {
    private static final Logger LOG = LoggerFactory.getLogger(CassandraStore.class);

    private static final int MAX_BATCH_SIZE = 500;
    private static final String CONF_FILE = "./etc/tsdr-persistence-cassandra.properties";

    private final TSDRKeyCache cache = new TSDRKeyCache();

    private final Session session;
    private BatchStatement batch;

    public CassandraStore() {
        LOG.info("Connecting to Cassandra...");

        session = createSession();
    }

    public CassandraStore(Session session, Cluster cluster) {
        LOG.info("Connecting to Cassandra...");
        this.session = session;
    }

    private static Map<String,String> loadConfig() throws IOException {
        Map<String, String> result = new HashMap<>();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(CONF_FILE)))) {
            String line = in.readLine();
            while (line != null) {
                int index = line.indexOf("=");
                String key = line.substring(0, index).trim();
                String value = line.substring(index + 1).trim();
                result.put(key, value);
                line = in.readLine();
            }
        }

        return result;
    }

    public BatchStatement getBatch() {
        return this.batch;
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private static Session createSession() {
        Map<String, String> config;
        try {
            config = loadConfig();
        } catch (IOException e) {
            LOG.error("Error loading config file", e);
            return null;
        }

        String host = config.get("host");
        boolean isMaster = Boolean.parseBoolean(config.get("master"));
        int replicationFactor = Integer.parseInt(config.get("replication_factor"));

        LOG.info("Trying to work with " + host + ", Which cassandra master is set to=" + isMaster);
        Cluster cluster = Cluster.builder().addContactPoint(host).build();

        // Try 5 times to connect to cassandra with a 5 seconds delay
        // between each try
        for (int index = 0; index < 5; index++) {
            try {
                return cluster.connect("tsdr");
            } catch (InvalidQueryException e) {
                try {
                    LOG.info("Failed to get tsdr keyspace...");
                    if (isMaster) {
                        LOG.info("This is the main node, trying to create keyspace and tables...");
                        Session session = cluster.connect();
                        session.execute("CREATE KEYSPACE tsdr WITH replication "
                                + "= {'class':'SimpleStrategy', 'replication_factor':" + replicationFactor + "};");
                        session = cluster.connect("tsdr");
                        createTSDRTables(session);
                        return session;
                    }
                } catch (RuntimeException e2) {
                    LOG.error("Failed to create keyspace & tables, will retry in 5 seconds...", e2);
                }
            } catch (NoHostAvailableException | AuthenticationException | IllegalStateException e) {
                LOG.error("Error connecting to the Cassandra cluster", e);
                return null;
            }

            LOG.info("Sleeping for 5 seconds...");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                LOG.error("Interrupted", e);
            }
        }

        return null;
    }

    private static void createTSDRTables(Session session) {
        String cql = "CREATE TABLE MetricVal (" + "KeyA bigint, " + "KeyB bigint, " + "Time bigint, " + "value double,"
                + "PRIMARY KEY (KeyA,KeyB,Time))";
        session.execute(cql);
        cql = "CREATE TABLE MetricLog (" + "KeyA bigint, " + "KeyB bigint, " + "Time bigint, " + "xIndex int,"
                + "value text," + "PRIMARY KEY (KeyA,KeyB,Time,xIndex))";
        session.execute(cql);
        cql = "CREATE TABLE MetricBlob (" + "KeyA bigint, " + "KeyB bigint, " + "Time bigint, " + "xIndex int,"
                + "value blob," + "PRIMARY KEY (KeyA,KeyB,Time,xIndex))";
        session.execute(cql);

    }

    public void startBatch() {
        this.batch = new BatchStatement();
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public void executeBatch() {
        try {
            this.session.execute(this.batch);
        } catch (RuntimeException e) {
            LOG.error("Failed to run batch", e);
        }
    }

    public void store(TSDRMetricRecord mr) {
        //create metric key
        String tsdrKey = FormatUtil.getTSDRMetricKey(mr);
        TSDRCacheEntry cacheEntry = cache.getCacheEntry(tsdrKey);

        //if it does not exist, create it
        if (cacheEntry == null) {
            cacheEntry = cache.addTSDRCacheEntry(tsdrKey);
        }
        RegularStatement st = QueryBuilder.insertInto("tsdr", "MetricVal")
                .value("KeyA", cacheEntry.getMd5ID().getMd5Long1()).value("KeyB", cacheEntry.getMd5ID().getMd5Long2())
                .value("Time", mr.getTimeStamp()).value("value", mr.getMetricValue().doubleValue());

        this.batch.add(st);

        if (this.batch.size() >= MAX_BATCH_SIZE) {
            this.executeBatch();
            this.startBatch();
        }
    }

    public void store(TSDRLogRecord lr) {
        //create log key
        String tsdrKey = FormatUtil.getTSDRLogKey(lr);
        TSDRCacheEntry cacheEntry = cache.getCacheEntry(tsdrKey);
        //if it does not exist, create it
        if (cacheEntry == null) {
            cacheEntry = cache.addTSDRCacheEntry(tsdrKey);
        }
        RegularStatement st = QueryBuilder.insertInto("tsdr", "MetricLog")
                .value("KeyA", cacheEntry.getMd5ID().getMd5Long1()).value("KeyB", cacheEntry.getMd5ID().getMd5Long2())
                .value("Time", lr.getTimeStamp()).value("xIndex", lr.getIndex()).value("value", lr.getRecordFullText());

        this.batch.add(st);

        if (this.batch.size() >= MAX_BATCH_SIZE) {
            this.executeBatch();
            this.startBatch();
        }
    }

    public void store(TSDRBinaryRecord lr) {
        //create log key
        String tsdrKey = FormatUtil.getTSDRBinaryKey(lr);
        TSDRCacheEntry cacheEntry = cache.getCacheEntry(tsdrKey);
        //if it does not exist, create it
        if (cacheEntry == null) {
            cacheEntry = cache.addTSDRCacheEntry(tsdrKey);
        }
        RegularStatement st = QueryBuilder.insertInto("tsdr", "MetricBlob")
                .value("KeyA", cacheEntry.getMd5ID().getMd5Long1()).value("KeyB", cacheEntry.getMd5ID().getMd5Long2())
                .value("Time", lr.getTimeStamp()).value("xIndex", lr.getIndex()).value("value", lr.getData());

        this.batch.add(st);

        if (this.batch.size() >= MAX_BATCH_SIZE) {
            this.executeBatch();
            this.startBatch();
        }
    }

    public List<TSDRMetricRecord> getTSDRMetricRecords(String tsdrMetricKey, long startDateTime, long endDateTime,
            int recordLimit) {
        TSDRCacheEntry entry = this.cache.getCacheEntry(tsdrMetricKey);
        //Exact match was found
        if (entry != null) {
            final List<TSDRMetricRecord> result = new LinkedList<>();
            String cql = "select * from MetricVal where KeyA=" + entry.getMd5ID().getMd5Long1()
                    + " and KeyB=" + entry.getMd5ID().getMd5Long2() + " and Time>=" + startDateTime
                    + " and Time<=" + endDateTime + " limit " + recordLimit;
            ResultSet rs = session.execute(cql);
            for (Row r : rs.all()) {
                result.add(getTSDRMetricRecord(r.getLong("Time"), r.getDouble("value"), entry));
            }
            return result;
        } else {
            final TSDRKeyCache.TSDRMetricCollectJob job = (entry1, startDateTime1, endDateTime1, recordLimit1,
                    globalResult) -> {
                String cql = "select * from MetricVal where KeyA=" + entry1.getMd5ID().getMd5Long1() + " and KeyB="
                        + entry1.getMd5ID().getMd5Long2() + " and Time>=" + startDateTime1 + " and Time<="
                        + endDateTime1 + " limit " + (recordLimit1 - globalResult.size());
                ResultSet rs = session.execute(cql);
                for (Row r : rs.all()) {
                    globalResult.add(getTSDRMetricRecord(r.getLong("Time"), r.getDouble("value"), entry1));
                }
            };
            return this.cache.getTSDRMetricRecords(tsdrMetricKey, startDateTime, endDateTime, recordLimit, job);
        }
    }

    public List<TSDRLogRecord> getTSDRLogRecords(String tsdrLogKey, long startDateTime, long endDateTime,
            int recordLimit) {
        TSDRCacheEntry entry = this.cache.getCacheEntry(tsdrLogKey);
        // Exact match was found
        if (entry != null) {
            final List<TSDRLogRecord> result = new LinkedList<>();
            String cql = "select * from MetricLog where KeyA=" + entry.getMd5ID().getMd5Long1() + " and KeyB="
                    + entry.getMd5ID().getMd5Long2() + " and Time>=" + startDateTime + " and Time<=" + endDateTime
                    + " limit " + recordLimit;
            ResultSet rs = session.execute(cql);
            for (Row r : rs.all()) {
                result.add(getTSDRLogRecord(r.getLong("Time"), r.getString("value"), r.getInt("xIndex"), entry));
            }
            return result;
        } else {
            TSDRKeyCache.TSDRLogCollectJob job = (entry1, startDateTime1, endDateTime1, recordLimit1, globalResult) -> {
                String cql = "select * from MetricLog where KeyA=" + entry1.getMd5ID().getMd5Long1() + " and KeyB="
                        + entry1.getMd5ID().getMd5Long2() + " and Time>=" + startDateTime1 + " and Time<="
                        + endDateTime1 + " limit " + (recordLimit1 - globalResult.size());
                ResultSet rs = session.execute(cql);
                for (Row r : rs.all()) {
                    globalResult
                            .add(getTSDRLogRecord(r.getLong("Time"), r.getString("value"), r.getInt("xIndex"), entry1));
                }
            };
            return this.cache.getTSDRLogRecords(tsdrLogKey, startDateTime, endDateTime, recordLimit, job);
        }
    }

    public List<TSDRBinaryRecord> getTSDRBinaryRecords(String tsdrBinaryKey, long startDateTime, long endDateTime,
            int recordLimit) {
        TSDRCacheEntry entry = this.cache.getCacheEntry(tsdrBinaryKey);
        // Exact match was found
        if (entry != null) {
            final List<TSDRBinaryRecord> result = new LinkedList<>();
            String cql = "select * from MetricBlob where KeyA=" + entry.getMd5ID().getMd5Long1() + " and KeyB="
                    + entry.getMd5ID().getMd5Long2() + " and Time>=" + startDateTime + " and Time<=" + endDateTime
                    + " limit " + recordLimit;
            ResultSet rs = session.execute(cql);
            for (Row r : rs.all()) {
                result.add(
                        getTSDRBinaryRecord(r.getLong("Time"), r.getBytes("value").array(), r.getInt("xIndex"), entry));
            }
            return result;
        } else {
            TSDRKeyCache.TSDRBinaryCollectJob job = (entry1, startDateTime1, endDateTime1, recordLimit1,
                    globalResult) -> {
                String cql = "select * from MetricBlob where KeyA=" + entry1.getMd5ID().getMd5Long1() + " and KeyB="
                        + entry1.getMd5ID().getMd5Long2() + " and Time>=" + startDateTime1 + " and Time<="
                        + endDateTime1 + " limit " + (recordLimit1 - globalResult.size());
                ResultSet rs = session.execute(cql);
                for (Row r : rs.all()) {
                    globalResult.add(getTSDRBinaryRecord(r.getLong("Time"), r.getBytes("value").array(),
                            r.getInt("xIndex"), entry1));
                }
            };
            return this.cache.getTSDRBinaryRecords(tsdrBinaryKey, startDateTime, endDateTime, recordLimit, job);
        }
    }

    private static TSDRMetricRecord getTSDRMetricRecord(long time, double value, TSDRCacheEntry entry) {
        TSDRMetricRecordBuilder rb = new TSDRMetricRecordBuilder();
        rb.setMetricName(entry.getMetricName());
        rb.setMetricValue(new BigDecimal(value));
        rb.setNodeID(entry.getNodeID());
        rb.setRecordKeys(FormatUtil.getRecordKeysFromTSDRKey(entry.getTsdrKey()));
        rb.setTimeStamp(time);
        rb.setTSDRDataCategory(entry.getDataCategory());
        return rb.build();
    }

    private static TSDRLogRecord getTSDRLogRecord(long time, String value, int index, TSDRCacheEntry entry) {
        TSDRLogRecordBuilder lb = new TSDRLogRecordBuilder();
        lb.setTSDRDataCategory(entry.getDataCategory());
        lb.setTimeStamp(time);
        lb.setRecordKeys(FormatUtil.getRecordKeysFromTSDRKey(entry.getTsdrKey()));
        lb.setNodeID(entry.getNodeID());
        lb.setIndex(index);
        lb.setRecordAttributes(null);
        lb.setRecordFullText(value);
        return lb.build();
    }

    private static TSDRBinaryRecord getTSDRBinaryRecord(long time, byte[] value, int index, TSDRCacheEntry entry) {
        TSDRBinaryRecordBuilder lb = new TSDRBinaryRecordBuilder();
        lb.setTSDRDataCategory(entry.getDataCategory());
        lb.setTimeStamp(time);
        lb.setRecordKeys(FormatUtil.getRecordKeysFromTSDRKey(entry.getTsdrKey()));
        lb.setNodeID(entry.getNodeID());
        lb.setIndex(index);
        lb.setRecordAttributes(null);
        lb.setData(value);
        return lb.build();
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    public void shutdown() {
        if (this.session != null) {
            try {
                this.session.close();
            } catch (RuntimeException e) {
                LOG.error("Failed to close the cassandra session", e);
            }
        }
        this.cache.shutdown();
    }

    public void purge(DataCategory category, long retentionTime) {
        purgeMetrics(category,retentionTime);
        purgeLogs(category,retentionTime);
        purgeBinary(category,retentionTime);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void purgeMetrics(DataCategory category, long retentionTime) {
        //Cassandra does not support range delete prior to verison 3.
        //To overcome, we need to do a "select" and then batch delete one by one.
        String cql1 = "Select * from MetricVal where keyA = ";
        String dcql1 = "delete from MetricVal where keyA = ";
        String cql2 = " and keyB = ";
        String cql3 = " and time < " + retentionTime;
        String dcql3 = " and time =";
        this.startBatch();
        for (TSDRCacheEntry entry : this.cache.getAll()) {
            if (entry.getDataCategory() == category) {
                String cql = cql1 + entry.getMd5ID().getMd5Long1() + cql2 + entry.getMd5ID().getMd5Long2() + cql3;
                final ResultSet rs = session.execute(cql);
                for (Row row : rs.all()) {
                    String deleteCql = dcql1 + row.getLong("keyA") + cql2 + row.getLong("keyB") + dcql3
                            + row.getLong("time");
                    try {
                        batch.add(new SimpleStatement(deleteCql));
                    } catch (RuntimeException e) {
                        LOG.error("Error creating simpleStatement", e);
                    }
                    if (this.batch.size() >= MAX_BATCH_SIZE) {
                        this.executeBatch();
                        this.startBatch();
                    }
                }
            }
        }
        if (this.batch.size() > 0) {
            this.executeBatch();
            this.startBatch();
        }
    }

    private void purgeLogs(DataCategory category, long retentionTime) {
        //Cassandra does not support range delete prior to verison 3.
        //To overcome, we need to do a "select" and then batch delete one by one.
        String cql1 = "Select * from MetricLog where keyA = ";
        String cql2 = " and keyB = ";
        String cql3 = " and time < " + retentionTime;
        this.startBatch();
        for (TSDRCacheEntry entry : this.cache.getAll()) {
            if (entry.getDataCategory() == category) {
                String cql = cql1 + entry.getMd5ID().getMd5Long1() + cql2 + entry.getMd5ID().getMd5Long2() + cql3;
                final ResultSet rs = session.execute(cql);
                for (Row row : rs.all()) {
                    if (this.batch.size() >= MAX_BATCH_SIZE) {
                        this.executeBatch();
                        this.startBatch();
                    }
                }
            }
        }
        if (this.batch.size() > 0) {
            this.executeBatch();
            this.startBatch();
        }
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void purgeBinary(DataCategory category, long retentionTime) {
        //Cassandra does not support range delete prior to verison 3.
        //To overcome, we need to do a "select" and then batch delete one by one.
        String cql1 = "Select * from MetricBlob where keyA = ";
        String dcql1 = "delete from MetricBlov where keyA = ";
        String cql2 = " and keyB = ";
        String cql3 = " and time < " + retentionTime;
        String dcql3 = " and time = ";
        String dcql4 = " and xIndex = ";
        this.startBatch();
        for (TSDRCacheEntry entry : this.cache.getAll()) {
            if (entry.getDataCategory() == category) {
                String cql = cql1 + entry.getMd5ID().getMd5Long1() + cql2 + entry.getMd5ID().getMd5Long2() + cql3;
                final ResultSet rs = session.execute(cql);
                for (Row row : rs.all()) {
                    String deleteCql = dcql1 + row.getLong("keyA") + cql2 + row.getLong("keyB") + dcql3
                            + row.getLong("time") + dcql4 + row.getInt("xIndex");
                    try {
                        batch.add(new SimpleStatement(deleteCql));
                    } catch (RuntimeException e) {
                        LOG.error("Error creating simpleStatement", e);
                    }
                    if (this.batch.size() >= MAX_BATCH_SIZE) {
                        this.executeBatch();
                        this.startBatch();
                    }
                }
            }
        }
        if (this.batch.size() > 0) {
            this.executeBatch();
            this.startBatch();
        }
    }
}
