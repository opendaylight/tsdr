/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hsqldb;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.opendaylight.tsdr.spi.util.FormatUtil;
import org.opendaylight.tsdr.spi.util.TSDRKeyCache;
import org.opendaylight.tsdr.spi.util.TSDRKeyCache.TSDRCacheEntry;
import org.opendaylight.tsdr.spi.util.TSDRKeyCache.TSDRLogCollectJob;
import org.opendaylight.tsdr.spi.util.TSDRKeyCache.TSDRMetricCollectJob;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrlogrecord.input.TSDRLogRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrlog.RecordAttributes;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class HSQLDBStore {
    private Connection connection = null;
    private static final Logger log = LoggerFactory.getLogger(HSQLDBStore.class);
    private TSDRKeyCache cache = new TSDRKeyCache();
    private static final String METRIC_TABLE = "METRICS";
    private static final String LOG_TABLE = "LOGS";

    public HSQLDBStore(){
        log.info("Connecting to HSQLDB...");
        this.connection = getConnection();
        try{
            createTSDRTables();
        }catch(SQLException e){
            log.error("Failed To Create TSDR Tables",e);
        }
    }

    public HSQLDBStore(Connection c){
        log.info("Connecting to HSQLDB...");
        this.connection = c;
    }

    public synchronized Connection getConnection() {
        try {
            new org.hsqldb.jdbcDriver();
            return DriverManager.getConnection("jdbc:hsqldb:./tsdr/tsdr-hsqldb", "sa","");
        } catch (Exception e) {
            log.error("Failed to get connection to database",e);
        }
        return null;
    }

    public void createTSDRTables() throws SQLException {
        DatabaseMetaData dbm = connection.getMetaData();
        //seek the table METRICVAL in the database schema, if it does not
        //exist then create the database schema
        ResultSet rs = dbm.getTables(null, null,METRIC_TABLE, null);
        if (!rs.next()) {
            String sql = "CREATE TABLE "+METRIC_TABLE+" (" +
                    "KeyA bigint, " +
                    "KeyB bigint, " +
                    "Time bigint, " +
                    "value double," +
                    "PRIMARY KEY (KeyA,KeyB,Time))";
            Statement st = this.connection.createStatement();
            st.execute(sql);
            st.close();
            sql = "CREATE TABLE "+ LOG_TABLE +" (" +
                    "KeyA bigint, " +
                    "KeyB bigint, " +
                    "Time bigint, " +
                    "xIndex int," +
                    "value VARCHAR(255)," +
                    "PRIMARY KEY (KeyA,KeyB,Time,xIndex))";
            st = this.connection.createStatement();
            st.execute(sql);
            st.close();
        }
    }

    public void store(TSDRMetricRecord mr) throws SQLException {
        //create metric key
        String tsdrKey = FormatUtil.getTSDRMetricKey(mr);
        TSDRCacheEntry cacheEntry = cache.getCacheEntry(tsdrKey);

        //if it does not exist, create it
        if(cacheEntry==null){
            cacheEntry = cache.addTSDRCacheEntry(tsdrKey);
        }

        StringBuilder sql = new StringBuilder();
        sql.append("insert into "+METRIC_TABLE+" (KeyA,KeyB,Time,value) values(");
        sql.append(cacheEntry.getMd5ID().getMd5Long1()).append(",");
        sql.append(cacheEntry.getMd5ID().getMd5Long2()).append(",");
        sql.append(mr.getTimeStamp()).append(",");
        sql.append(mr.getMetricValue()).append(")");
        Statement st = this.connection.createStatement();
        st.execute(sql.toString());
        st.close();
    }

    public void store(TSDRLogRecord lr) throws SQLException {
        //create log key
        String tsdrKey = FormatUtil.getTSDRLogKey(lr);
        TSDRCacheEntry cacheEntry = cache.getCacheEntry(tsdrKey);
        //if it does not exist, create it
        if(cacheEntry==null){
            cacheEntry = cache.addTSDRCacheEntry(tsdrKey);
        }

        StringBuilder sql = new StringBuilder();
        sql.append("insert into "+ LOG_TABLE +" (KeyA,KeyB,Time,xIndex,value) values(");
        sql.append(cacheEntry.getMd5ID().getMd5Long1()).append(",");
        sql.append(cacheEntry.getMd5ID().getMd5Long2()).append(",");
        sql.append(lr.getTimeStamp()).append(",");
        sql.append(lr.getIndex()).append(",'");
        sql.append(lr.getRecordFullText()).append("')");
        Statement st = this.connection.createStatement();
        st.execute(sql.toString());
        st.close();
    }

    public List<TSDRMetricRecord> getTSDRMetricRecords(String tsdrMetricKey, long startDateTime, long endDateTime,int recordLimit) throws SQLException {
        TSDRCacheEntry entry = this.cache.getCacheEntry(tsdrMetricKey);
        //Exact match was found
        if(entry!=null){
            List<TSDRMetricRecord> result = new LinkedList<TSDRMetricRecord>();
            String sql = "select * from "+METRIC_TABLE+" where KeyA=" + entry.getMd5ID().getMd5Long1()
                    + " and KeyB=" + entry.getMd5ID().getMd5Long2() + " and Time>=" + startDateTime + " and Time<=" + endDateTime;
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while(rs.next()){
                result.add(getTSDRMetricRecord(rs.getLong("Time"), rs.getDouble("value"), entry));
                if(result.size()>=recordLimit){
                    break;
                }
            }
            rs.close();
            st.close();
            return result;
        }else{
            TSDRMetricCollectJob job = new TSDRMetricCollectJob() {
                @Override
                public void collectMetricRecords(TSDRCacheEntry entry, long startDateTime, long endDateTime, int recordLimit, List<TSDRMetricRecord> globalResult) {
                    String sql = "select * from "+METRIC_TABLE+" where KeyA=" + entry.getMd5ID().getMd5Long1()
                            + " and KeyB=" + entry.getMd5ID().getMd5Long2() + " and Time>=" + startDateTime + " and Time<=" + endDateTime;
                    try {
                        Statement st = connection.createStatement();
                        ResultSet rs = st.executeQuery(sql);
                        while (rs.next()) {
                            globalResult.add(getTSDRMetricRecord(rs.getLong("Time"), rs.getDouble("value"), entry));
                            if (globalResult.size() >= recordLimit) {
                                break;
                            }
                        }
                        rs.close();
                        st.close();
                    }catch(SQLException e){
                        log.error("SQL Error while retrieving records",e);
                    }
                }
            };
            return this.cache.getTSDRMetricRecords(tsdrMetricKey,startDateTime,endDateTime,recordLimit,job);
        }
    }

    public List<TSDRLogRecord> getTSDRLogRecords(String tsdrLogKey, long startDateTime, long endDateTime, int recordLimit) throws SQLException {
        TSDRCacheEntry entry = this.cache.getCacheEntry(tsdrLogKey);
        //Exact match was found
        if(entry!=null){
            List<TSDRLogRecord> result = new LinkedList<TSDRLogRecord>();
            String sql = "select * from "+ LOG_TABLE +" where KeyA=" + entry.getMd5ID().getMd5Long1()
                    + " and KeyB=" + entry.getMd5ID().getMd5Long2() + " and Time>=" + startDateTime + " and Time<=" + endDateTime;
            Statement st = connection.createStatement();
            ResultSet rs = st.executeQuery(sql);
            while(rs.next()){
                result.add(getTSDRLogRecord(rs.getLong("Time"), rs.getString("value"), rs.getInt("xIndex"), entry));
                if(result.size()>=recordLimit){
                    break;
                }
            }
            rs.close();
            st.close();
            return result;
        }else{
            TSDRLogCollectJob job = new TSDRLogCollectJob() {
                @Override
                public void collectLogRecords(TSDRCacheEntry entry, long startDateTime, long endDateTime, int recordLimit, List<TSDRLogRecord> globalResult) {
                    String sql = "select * from "+ LOG_TABLE +" where KeyA=" + entry.getMd5ID().getMd5Long1()
                            + " and KeyB=" + entry.getMd5ID().getMd5Long2() + " and Time>=" + startDateTime + " and Time<=" + endDateTime;
                    try {
                        Statement st = connection.createStatement();
                        ResultSet rs = st.executeQuery(sql);
                        while (rs.next()) {
                            globalResult.add(getTSDRLogRecord(rs.getLong("Time"), rs.getString("value"), rs.getInt("xIndex"), entry));
                            if (globalResult.size() >= recordLimit) {
                                break;
                            }
                        }
                        rs.close();
                        st.close();
                    }catch(SQLException e){
                        log.error("SQL Error while retrieving records",e);
                    }
                }
            };
            return this.cache.getTSDRLogRecords(tsdrLogKey,startDateTime,endDateTime,recordLimit,job);
        }
    }

    private static final List<RecordKeys> EMPTY_RECORD_KEYS = new ArrayList<>();
    private static final List<RecordAttributes> EMPTY_RECORD_ATTRIBUTES = new ArrayList<>();

    private static final TSDRMetricRecord getTSDRMetricRecord(long time, double value, TSDRCacheEntry entry){
        TSDRMetricRecordBuilder rb = new TSDRMetricRecordBuilder();
        rb.setMetricName(entry.getMetricName());
        rb.setMetricValue(new BigDecimal(value));
        rb.setNodeID(entry.getNodeID());
        rb.setRecordKeys(FormatUtil.getRecordKeysFromTSDRKey(entry.getTsdrKey()));
        rb.setTimeStamp(time);
        rb.setTSDRDataCategory(entry.getDataCategory());
        return rb.build();
    }

    private static final TSDRLogRecord getTSDRLogRecord(long time,String value,int index,TSDRCacheEntry entry){
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

    public void shutdown(){
        if(this.connection!=null){
            try{
                this.connection.close();
            }catch(Exception err){
                log.error("Failed to close the DB Connection",err);
            }
        }
        this.cache.shutdown();
    }

    private void purgeMetrics(DataCategory category, long retentionTime) throws SQLException {
        String sql1 = "Delete from "+METRIC_TABLE+" where keyA = ";
        String sql2 = " and keyB = ";
        String sql3 = " and time < " + retentionTime;
        for (TSDRCacheEntry entry : this.cache.getAll()) {
            if (entry.getDataCategory() == category) {
                String sql = sql1 + entry.getMd5ID().getMd5Long1() + sql2 + entry.getMd5ID().getMd5Long2() + sql3;
                Statement st = this.connection.createStatement();
                st.execute(sql);
                st.close();
            }
        }
    }

    private void purgeLogs(DataCategory category, long retentionTime) throws SQLException {
        String sql1 = "Delete from "+ LOG_TABLE +" where keyA = ";
        String sql2 = " and keyB = ";
        String sql3 = " and time < " + retentionTime;
        for (TSDRCacheEntry entry : this.cache.getAll()) {
            if (entry.getDataCategory() == category) {
                String sql = sql1 + entry.getMd5ID().getMd5Long1() + sql2 + entry.getMd5ID().getMd5Long2() + sql3;
                Statement st = this.connection.createStatement();
                st.execute(sql);
                st.close();
            }
        }
    }

    public void purge(DataCategory category, long retentionTime) throws SQLException {
        purgeMetrics(category,retentionTime);
        purgeLogs(category,retentionTime);
    }

}
