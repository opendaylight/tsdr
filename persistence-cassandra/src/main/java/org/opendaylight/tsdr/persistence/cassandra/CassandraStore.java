/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.InvalidQueryException;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.opendaylight.tsdr.spi.util.FormatUtil;
import org.opendaylight.tsdr.spi.util.TSDRKeyCache;
import org.opendaylight.tsdr.spi.util.TSDRKeyCache.TSDRCacheEntry;
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
public class CassandraStore {
    private static final String confFile = "./etc/tsdr-persistence-cassandra.properties";
    private Session session = null;
    private Cluster cluster = null;
    private boolean isMaster = true;
    private String host = null;
    private int replication_factor = 1;
    private Logger log = LoggerFactory.getLogger(CassandraStore.class);
    private TSDRKeyCache cache = new TSDRKeyCache();

    public CassandraStore(){
        log.info("Connecting to Cassandra...");
        try {
            getSession();
            loadPathCache();
        } catch (Exception e) {
            log.error("Failed to connect to Cassandra",e);
        }
    }

    public CassandraStore(Session s,Cluster c){
        log.info("Connecting to Cassandra...");
        this.session = s;
        this.cluster = c;
        try {
            getSession();
            loadPathCache();
        } catch (Exception e) {
            log.error("Failed to connect to Cassandra",e);
        }
    }

    private Map<String,String> loadConfig() throws IOException{
        HashMap<String, String> result = new HashMap<String,String>();
        BufferedReader in = null;
        in = new BufferedReader(new InputStreamReader(new FileInputStream(confFile)));
        String line = in.readLine();
        while(line!=null){
            int index = line.indexOf("=");
            String key = line.substring(0,index).trim();
            String value = line.substring(index+1).trim();
            result.put(key, value);
            line = in.readLine();
        }
        in.close();
        return result;
    }

    public Session getSession() throws Exception {
        if (session == null) {
            synchronized (this) {
                Map<String,String> config = loadConfig();
                this.host = config.get("host");
                this.isMaster = Boolean.parseBoolean(config.get("master"));
                this.replication_factor = Integer.parseInt(config.get("replication_factor"));
                log.info("Trying to work with " + this.host+ ", Which cassandra master is set to=" + this.isMaster);
                Cluster cluster = Cluster.builder().addContactPoint(host).build();

                // Try 5 times to connect to cassandra with a 5 seconds delay
                // between each try
                for (int index = 0; index < 5; index++) {
                    try {
                        session = cluster.connect("tsdr");
                        return session;
                    } catch (InvalidQueryException err) {
                        try {
                            log.info("Failed to get tsdr keyspace...");
                            if (this.isMaster) {
                                log.info("This is the main node, trying to create keyspace and tables...");
                                session = cluster.connect();
                                session.execute("CREATE KEYSPACE tsdr WITH replication "
                                + "= {'class':'SimpleStrategy', 'replication_factor':"+replication_factor+"};");
                                session = cluster.connect("tsdr");
                                createTSDRTables();
                                return session;
                            }
                        } catch (Exception err2) {
                            log.error("Failed to create keyspace & tables, will retry in 5 seconds...",err2);
                        }
                    }
                    log.info("Sleeping for 5 seconds...");
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        log.error("Interrupted",e);
                    }
                }
            }
        }
        return session;
    }

    public void createTSDRTables(){
        String cql = "CREATE TABLE MetricPath (keyPath text,"+
                "PRIMARY KEY (KeyPath))";
        this.session.execute(cql);
        cql = "CREATE TABLE MetricVal ("+
                  "KeyA bigint, "+
                  "KeyB bigint, "+
                  "Time bigint, "+
                  "value double,"+
                  "PRIMARY KEY (KeyA,KeyB,Time))";
        this.session.execute(cql);
        cql = "CREATE TABLE MetricLog ("+
                "KeyA bigint, "+
                "KeyB bigint, "+
                "Time bigint, "+
                "xIndex int,"+
                "value text,"+
                "PRIMARY KEY (KeyA,KeyB,Time,xIndex))";
        this.session.execute(cql);
    }

    public void store(TSDRMetricRecord mr){
        //create metric key
        String tsdrKey = FormatUtil.getTSDRMetricKey(mr);
        TSDRCacheEntry cacheEntry = cache.getCacheEntry(tsdrKey);

        //if it does not exist, create it
        if(cacheEntry==null){
            cacheEntry = cache.addTSDRCacheEntry(tsdrKey);
            StringBuilder cql = new StringBuilder();
            cql.append("insert into MetricPath (KeyPath) values(");
            cql.append("'").append(cacheEntry.getTsdrKey()).append("'");
            cql.append(")");
            session.execute(cql.toString());
        }

        StringBuilder cql = new StringBuilder();
        cql.append("insert into MetricVal (KeyA,KeyB,Time,value) values(");
        cql.append(cacheEntry.getMd5ID().getMd5Long1()).append(",");
        cql.append(cacheEntry.getMd5ID().getMd5Long2()).append(",");
        cql.append(mr.getTimeStamp()).append(",");
        cql.append(mr.getMetricValue()).append(")");
        session.execute(cql.toString());
    }

    public void store(TSDRLogRecord lr){
        //create log key
        String tsdrKey = FormatUtil.getTSDRLogKey(lr);
        TSDRCacheEntry cacheEntry = cache.getCacheEntry(tsdrKey);
        //if it does not exist, create it
        if(cacheEntry==null){
            cacheEntry = cache.addTSDRCacheEntry(tsdrKey);
            StringBuilder cql = new StringBuilder();
            cql.append("insert into MetricPath (KeyPath) values(");
            cql.append("'").append(cacheEntry.getTsdrKey()).append("'");
            cql.append(")");
            session.execute(cql.toString());
        }

        StringBuilder cql = new StringBuilder();
        cql.append("insert into MetricLog (KeyA,KeyB,Time,xIndex,value) values(");
        cql.append(cacheEntry.getMd5ID().getMd5Long1()).append(",");
        cql.append(cacheEntry.getMd5ID().getMd5Long2()).append(",");
        cql.append(lr.getTimeStamp()).append(",");
        cql.append(lr.getIndex()).append(",'");
        cql.append(lr.getRecordFullText()).append("')");
        session.execute(cql.toString());
    }

    public List<TSDRMetricRecord> getTSDRMetricRecords(String tsdrMetricKey, long startDateTime, long endDateTime,int recordLimit) {
        TSDRCacheEntry entry = this.cache.getCacheEntry(tsdrMetricKey);
        //Exact match was found
        if(entry!=null){
            final List<TSDRMetricRecord> result = new LinkedList<TSDRMetricRecord>();
            String cql = "select * from MetricVal where KeyA=" + entry.getMd5ID().getMd5Long1()
                    + " and KeyB=" + entry.getMd5ID().getMd5Long2() + " and Time>=" + startDateTime
                    + " and Time<=" + endDateTime + " limit "+recordLimit;
            ResultSet rs = session.execute(cql);
            for (Row r : rs.all()) {
                result.add(getTSDRMetricRecord(r.getLong("Time"), r.getDouble("value"), entry));
            }
            return result;
        }else{
            final TSDRKeyCache.TSDRMetricCollectJob job = new TSDRKeyCache.TSDRMetricCollectJob() {
                @Override
                public void collectMetricRecords(TSDRCacheEntry entry, long startDateTime, long endDateTime, int recordLimit, List<TSDRMetricRecord> globalResult) {
                    String cql = "select * from MetricVal where KeyA=" + entry.getMd5ID().getMd5Long1()
                            + " and KeyB=" + entry.getMd5ID().getMd5Long2() + " and Time>=" + startDateTime
                            + " and Time<=" + endDateTime + " limit "+(recordLimit-globalResult.size());
                    ResultSet rs = session.execute(cql);
                    for (Row r : rs.all()) {
                        globalResult.add(getTSDRMetricRecord(r.getLong("Time"), r.getDouble("value"), entry));
                    }
                }
            };
            return this.cache.getTSDRMetricRecords(tsdrMetricKey,startDateTime,endDateTime,recordLimit,job);
        }
    }

    public List<TSDRLogRecord> getTSDRLogRecords(String tsdrLogKey, long startDateTime, long endDateTime, int recordLimit) {
        TSDRCacheEntry entry = this.cache.getCacheEntry(tsdrLogKey);
        //Exact match was found
        if(entry!=null){
            final List<TSDRLogRecord> result = new LinkedList<TSDRLogRecord>();
            String cql = "select * from MetricLog where KeyA=" + entry.getMd5ID().getMd5Long1()
                    + " and KeyB=" + entry.getMd5ID().getMd5Long2() + " and Time>="
                    + startDateTime + " and Time<=" + endDateTime + " limit "+recordLimit;
            ResultSet rs = session.execute(cql);
            for (Row r : rs.all()) {
                result.add(getTSDRLogRecord(r.getLong("Time"), r.getString("value"), r.getInt("xIndex"), entry));
            }
            return result;
        }else{
            TSDRKeyCache.TSDRLogCollectJob job = new TSDRKeyCache.TSDRLogCollectJob() {
                @Override
                public void collectLogRecords(TSDRCacheEntry entry, long startDateTime, long endDateTime, int recordLimit, List<TSDRLogRecord> globalResult) {
                    String cql = "select * from MetricLog where KeyA=" + entry.getMd5ID().getMd5Long1()
                            + " and KeyB=" + entry.getMd5ID().getMd5Long2() + " and Time>="
                            + startDateTime + " and Time<=" + endDateTime + " limit "+(recordLimit-globalResult.size());
                    ResultSet rs = session.execute(cql);
                    for (Row r : rs.all()) {
                        globalResult.add(getTSDRLogRecord(r.getLong("Time"), r.getString("value"), r.getInt("xIndex"), entry));
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

    public void loadPathCache(){
        ResultSet rs = session.execute("select * from MetricPath limit 100000");
        for(Row r:rs.all()){
            String tsdrKey = r.getString("KeyPath");
            this.cache.addTSDRCacheEntry(tsdrKey);
        }
    }

    public void shutdown(){
        if(this.session!=null){
            try{this.session.close();}catch(Exception err){log.error("Failed to close the cassandra session",err);}
        }
    }

    public void purge(DataCategory category, long retentionTime){
        String cql1 = "Delete from MetricVal where keyA = ";
        String cql2 = " and keyB = ";
        String cql3 = " and time < "+retentionTime;
        for(TSDRCacheEntry entry:this.cache.getAll()){
            if(entry.getDataCategory()==category){
                String cql = cql1 + entry.getMd5ID().getMd5Long1()+cql2+entry.getMd5ID().getMd5Long2()+cql3;
                session.execute(cql);
            }
        }
    }

}
