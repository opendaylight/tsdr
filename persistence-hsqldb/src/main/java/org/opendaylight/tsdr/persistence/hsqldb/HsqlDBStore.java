/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hsqldb;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import org.opendaylight.tsdr.spi.util.FormatUtil;
import org.opendaylight.tsdr.spi.util.TSDRKeyCache;
import org.opendaylight.tsdr.spi.util.TSDRKeyCache.TSDRCacheEntry;
import org.opendaylight.tsdr.spi.util.TSDRKeyCache.TSDRLogCollectJob;
import org.opendaylight.tsdr.spi.util.TSDRKeyCache.TSDRMetricCollectJob;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HSQLDB Back-end store.
 *
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class HsqlDBStore {
    private static final Logger LOG = LoggerFactory.getLogger(HsqlDBStore.class);
    private static final String METRIC_TABLE = "METRICS";
    private static final String LOG_TABLE = "LOGS";

    private final Connection connection;
    private final TSDRKeyCache cache = new TSDRKeyCache();

    public HsqlDBStore() {
        LOG.info("Connecting to HSQLDB...");
        this.connection = getConnection();
        try {
            createTSDRTables();
        } catch (SQLException e) {
            LOG.error("Failed To Create TSDR Tables", e);
        }
    }

    public HsqlDBStore(Connection connection) {
        LOG.info("Connecting to HSQLDB...");
        this.connection = connection;
    }

    @SuppressFBWarnings("DMI_EMPTY_DB_PASSWORD")
    private static Connection getConnection() {
        try {
            new org.hsqldb.jdbcDriver();
            return DriverManager.getConnection("jdbc:hsqldb:./tsdr/tsdr-hsqldb", "sa", "");
        } catch (SQLException e) {
            LOG.error("Failed to get connection to database", e);
        }
        return null;
    }

    public void createTSDRTables() throws SQLException {
        DatabaseMetaData dbm = connection.getMetaData();
        //seek the table METRICVAL in the database schema, if it does not
        //exist then create the database schema
        ResultSet rs = dbm.getTables(null, null,METRIC_TABLE, null);
        if (!rs.next()) {
            String sql = "CREATE TABLE " + METRIC_TABLE + " (" + "KeyA bigint, " + "KeyB bigint, " + "Time bigint, "
                    + "value double," + "PRIMARY KEY (KeyA,KeyB,Time))";
            try (Statement st = this.connection.createStatement()) {
                st.execute(sql);
            }

            sql = "CREATE TABLE " + LOG_TABLE + " (" + "KeyA bigint, " + "KeyB bigint, " + "Time bigint, "
                    + "xIndex int," + "value VARCHAR(255)," + "PRIMARY KEY (KeyA,KeyB,Time,xIndex))";
            try (Statement st = this.connection.createStatement()) {
                st.execute(sql);
            }
        }
    }

    @SuppressFBWarnings("SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE")
    public void store(TSDRMetricRecord mr) throws SQLException {
        //create metric key
        String tsdrKey = FormatUtil.getTSDRMetricKey(mr);
        TSDRCacheEntry cacheEntry = cache.getCacheEntry(tsdrKey);

        //if it does not exist, create it
        if (cacheEntry == null) {
            cacheEntry = cache.addTSDRCacheEntry(tsdrKey);
        }

        StringBuilder buf = new StringBuilder();
        buf.append("insert into " + METRIC_TABLE + " (KeyA,KeyB,Time,value) values(");
        buf.append(cacheEntry.getMd5ID().getMd5Long1()).append(",");
        buf.append(cacheEntry.getMd5ID().getMd5Long2()).append(",");
        buf.append(mr.getTimeStamp()).append(",");
        buf.append(mr.getMetricValue()).append(")");
        String sql = buf.toString();

        try (Statement st = this.connection.createStatement()) {
            st.execute(sql);
        }
    }

    @SuppressFBWarnings("SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE")
    public void store(TSDRLogRecord lr) throws SQLException {
        //create log key
        String tsdrKey = FormatUtil.getTSDRLogKey(lr);
        TSDRCacheEntry cacheEntry = cache.getCacheEntry(tsdrKey);
        //if it does not exist, create it
        if (cacheEntry == null) {
            cacheEntry = cache.addTSDRCacheEntry(tsdrKey);
        }

        StringBuilder buf = new StringBuilder();
        buf.append("insert into " + LOG_TABLE + " (KeyA,KeyB,Time,xIndex,value) values(");
        buf.append(cacheEntry.getMd5ID().getMd5Long1()).append(",");
        buf.append(cacheEntry.getMd5ID().getMd5Long2()).append(",");
        buf.append(lr.getTimeStamp()).append(",");
        buf.append(lr.getIndex()).append(",'");
        buf.append(lr.getRecordFullText()).append("')");
        String sql = buf.toString();

        try (Statement st = this.connection.createStatement()) {
            st.execute(sql);
        }
    }

    public List<TSDRMetricRecord> getTSDRMetricRecords(String tsdrMetricKey, long startDateTime, long endDateTime,
            int recordLimit) throws SQLException {
        TSDRCacheEntry entry = this.cache.getCacheEntry(tsdrMetricKey);
        // Exact match was found
        if (entry != null) {
            List<TSDRMetricRecord> result = new LinkedList<>();
            String sql = "select * from " + METRIC_TABLE + " where KeyA=" + entry.getMd5ID().getMd5Long1()
                    + " and KeyB=" + entry.getMd5ID().getMd5Long2() + " and Time>=" + startDateTime + " and Time<="
                    + endDateTime;

            try (Statement st = connection.createStatement()) {
                try (ResultSet rs = st.executeQuery(sql)) {
                    while (rs.next()) {
                        result.add(getTSDRMetricRecord(rs.getLong("Time"), rs.getDouble("value"), entry));
                        if (result.size() >= recordLimit) {
                            break;
                        }
                    }
                }
            }

            return result;
        } else {
            TSDRMetricCollectJob job = (entry1, startDateTime1, endDateTime1, recordLimit1, globalResult) -> {
                String sql = "select * from " + METRIC_TABLE + " where KeyA=" + entry1.getMd5ID().getMd5Long1()
                        + " and KeyB=" + entry1.getMd5ID().getMd5Long2() + " and Time>=" + startDateTime1
                        + " and Time<=" + endDateTime1;
                try (Statement st = connection.createStatement()) {
                    try (ResultSet rs = st.executeQuery(sql)) {
                        while (rs.next()) {
                            globalResult.add(getTSDRMetricRecord(rs.getLong("Time"), rs.getDouble("value"), entry1));
                            if (globalResult.size() >= recordLimit1) {
                                break;
                            }
                        }
                    }
                } catch (SQLException e) {
                    LOG.error("SQL Error while retrieving records", e);
                }
            };
            return this.cache.getTSDRMetricRecords(tsdrMetricKey, startDateTime, endDateTime, recordLimit, job);
        }
    }

    public List<TSDRLogRecord> getTSDRLogRecords(String tsdrLogKey, long startDateTime, long endDateTime,
            int recordLimit) throws SQLException {
        TSDRCacheEntry entry = this.cache.getCacheEntry(tsdrLogKey);
        // Exact match was found
        if (entry != null) {
            List<TSDRLogRecord> result = new LinkedList<>();
            String sql = "select * from " + LOG_TABLE + " where KeyA=" + entry.getMd5ID().getMd5Long1() + " and KeyB="
                    + entry.getMd5ID().getMd5Long2() + " and Time>=" + startDateTime + " and Time<=" + endDateTime;

            try (Statement st = connection.createStatement()) {
                try (ResultSet rs = st.executeQuery(sql)) {
                    while (rs.next()) {
                        result.add(getTSDRLogRecord(rs.getLong("Time"), rs.getString("value"),
                                rs.getInt("xIndex"), entry));
                        if (result.size() >= recordLimit) {
                            break;
                        }
                    }
                }
            }

            return result;
        } else {
            TSDRLogCollectJob job = (entry1, startDateTime1, endDateTime1, recordLimit1, globalResult) -> {
                String sql = "select * from " + LOG_TABLE + " where KeyA=" + entry1.getMd5ID().getMd5Long1()
                        + " and KeyB=" + entry1.getMd5ID().getMd5Long2() + " and Time>=" + startDateTime1
                        + " and Time<=" + endDateTime1;
                try (Statement st = connection.createStatement()) {
                    try (ResultSet rs = st.executeQuery(sql)) {
                        while (rs.next()) {
                            globalResult.add(getTSDRLogRecord(rs.getLong("Time"), rs.getString("value"),
                                    rs.getInt("xIndex"), entry1));
                            if (globalResult.size() >= recordLimit1) {
                                break;
                            }
                        }
                    }
                } catch (SQLException e) {
                    LOG.error("SQL Error while retrieving records", e);
                }
            };
            return this.cache.getTSDRLogRecords(tsdrLogKey, startDateTime, endDateTime, recordLimit, job);
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

    public void shutdown() {
        if (this.connection != null) {
            try {
                this.connection.close();
            } catch (SQLException err) {
                LOG.error("Failed to close the DB Connection", err);
            }
        }
        this.cache.shutdown();
    }

    private void purgeMetrics(DataCategory category, long retentionTime) throws SQLException {
        String sql1 = "Delete from " + METRIC_TABLE + " where keyA = ";
        String sql2 = " and keyB = ";
        String sql3 = " and time < " + retentionTime;
        for (TSDRCacheEntry entry : this.cache.getAll()) {
            if (entry.getDataCategory() == category) {
                String sql = sql1 + entry.getMd5ID().getMd5Long1() + sql2 + entry.getMd5ID().getMd5Long2() + sql3;

                try (Statement st = this.connection.createStatement()) {
                    st.execute(sql);
                }
            }
        }
    }

    private void purgeLogs(DataCategory category, long retentionTime) throws SQLException {
        String sql1 = "Delete from " + LOG_TABLE + " where keyA = ";
        String sql2 = " and keyB = ";
        String sql3 = " and time < " + retentionTime;
        for (TSDRCacheEntry entry : this.cache.getAll()) {
            if (entry.getDataCategory() == category) {
                String sql = sql1 + entry.getMd5ID().getMd5Long1() + sql2 + entry.getMd5ID().getMd5Long2() + sql3;

                try (Statement st = this.connection.createStatement()) {
                    st.execute(sql);
                }
            }
        }
    }

    public void purge(DataCategory category, long retentionTime) throws SQLException {
        purgeMetrics(category, retentionTime);
        purgeLogs(category, retentionTime);
    }
}
