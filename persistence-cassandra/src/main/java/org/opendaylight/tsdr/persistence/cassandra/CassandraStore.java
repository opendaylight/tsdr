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
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.opendaylight.tsdr.spi.util.FormatUtil;
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
    private boolean isMaster = true;
    private String host = null;
    private int replication_factor = 1;
    private Logger log = LoggerFactory.getLogger(CassandraStore.class);
    private Map<String,MetricPathCacheEntry> pathCache = new HashMap<String,MetricPathCacheEntry>();

    public CassandraStore(){
        log.info("Connecting to Cassandra...");
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

    private void createTSDRTables(){
        String cql = "CREATE TABLE MetricPath ("+
                        "TSDRDataCategory text,"+
                        "NodeID text,"+
                        "MetricName text,"+
                        "KeyA bigint,"+
                        "KeyB bigint,"+
                        "KeyPath text,"+
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
        String tsdrMetricKey = FormatUtil.getTSDRMetricKey(mr);
        MD5Identifier ID = null;
        MetricPathCacheEntry entry = pathCache.get(tsdrMetricKey);

        //if it does not exist, create it
        if(entry==null){
            ID = new MD5Identifier(tsdrMetricKey.getBytes());
            StringBuilder cql = new StringBuilder();
            cql.append("insert into MetricPath (TSDRDataCategory,NodeID,MetricName,KeyPath,KeyA,KeyB) values(");
            cql.append("'").append(mr.getTSDRDataCategory().toString()).append("',");
            cql.append("'").append(mr.getNodeID()).append("',");
            cql.append("'").append(mr.getMetricName()).append("',");
            cql.append("'").append(tsdrMetricKey).append("',");
            cql.append(ID.getA()).append(",");
            cql.append(ID.getB());
            cql.append(")");
            session.execute(cql.toString());
            this.pathCache.put(tsdrMetricKey, new MetricPathCacheEntry(ID.getA(),ID.getB(), mr.getTSDRDataCategory(), mr.getNodeID(), mr.getMetricName()));
        }else{
            ID = new MD5Identifier(entry.keyA,entry.keyB);
        }

        StringBuilder cql = new StringBuilder();
        cql.append("insert into MetricVal (KeyA,KeyB,Time,value) values(");
        cql.append(ID.getA()).append(",");
        cql.append(ID.getB()).append(",");
        cql.append(mr.getTimeStamp()).append(",");
        cql.append(mr.getMetricValue()).append(")");
        session.execute(cql.toString());
    }

    public void store(TSDRLogRecord lr){
        //create log key
        String logID = FormatUtil.getTSDRLogKey(lr);
        MD5Identifier ID = null;
        MetricPathCacheEntry entry = pathCache.get(logID);

        //if it does not exist, create it
        if(entry==null){
            ID = new MD5Identifier(logID.getBytes());
            StringBuilder cql = new StringBuilder();
            cql.append("insert into MetricPath (TSDRDataCategory,NodeID,KeyPath,KeyA,KeyB) values(");
            cql.append("'").append(lr.getTSDRDataCategory().toString()).append("',");
            cql.append("'").append(lr.getNodeID()).append("',");
            cql.append("'").append(logID).append("',");
            cql.append(ID.getA()).append(",");
            cql.append(ID.getB());
            cql.append(")");
            session.execute(cql.toString());
            this.pathCache.put(logID, new MetricPathCacheEntry(ID.getA(),ID.getB(), lr.getTSDRDataCategory(), lr.getNodeID(), null));
        }else{
            ID = new MD5Identifier(entry.keyA,entry.keyB);
        }

        StringBuilder cql = new StringBuilder();
        cql.append("insert into MetricLog (KeyA,KeyB,Time,xIndex,value) values(");
        cql.append(ID.getA()).append(",");
        cql.append(ID.getB()).append(",");
        cql.append(lr.getTimeStamp()).append(",");
        cql.append(lr.getIndex()).append(",'");
        cql.append(lr.getRecordFullText()).append("')");
        session.execute(cql.toString());
    }

    public List<?> get100Records(){
        ResultSet rs = session.execute("select * from MetricVal limit 100");
        List<String> arrayList = new ArrayList<>();
        for(Row r:rs.all()){
            long key = r.getLong("Key");
            long time = r.getLong("Time");
            double value = r.getDouble("value");
            MetricPathCacheEntry entry = pathCache.get(key);
            arrayList.add(entry.toString(time, value));
        }
        return arrayList;
    }

    public List<TSDRMetricRecord> getMetrics(String tsdrMetricKey, long startDateTime, long endDateTime) {
        MetricPathCacheEntry entry = this.pathCache.get(tsdrMetricKey);
        List<TSDRMetricRecord> result = new LinkedList<TSDRMetricRecord>();
        if(entry!=null){
            String cql = "select * from MetricVal where KeyA="+entry.keyA+" and KeyB="+entry.keyB+" and Time>="+startDateTime+" and Time<="+endDateTime;
            ResultSet rs = session.execute(cql);
            for(Row r:rs.all()){
                result.add(getTSDRMetricRecord(r.getLong("Time"), r.getDouble("value"),entry,tsdrMetricKey));
            }
        }
        return result;
    }

    public List<TSDRLogRecord> getLogs(String tsdrLogKey, long startDateTime, long endDateTime) {
        MetricPathCacheEntry entry = this.pathCache.get(tsdrLogKey);
        List<TSDRLogRecord> result = new LinkedList<TSDRLogRecord>();
        if(entry!=null){
            String cql = "select * from MetricLog where KeyA="+entry.keyA+" and KeyB="+entry.keyB+" and Time>="+startDateTime+" and Time<="+endDateTime;
            ResultSet rs = session.execute(cql);
            for(Row r:rs.all()){
                result.add(getTSDRLogRecord(r.getLong("Time"), r.getString("value"),r.getInt("xIndex"),entry,tsdrLogKey));
            }
        }
        return result;
    }

    private static final List<RecordKeys> EMPTY_RECORD_KEYS = new ArrayList<>();
    private static final List<RecordAttributes> EMPTY_RECORD_ATTRIBUTES = new ArrayList<>();

    private static final TSDRMetricRecord getTSDRMetricRecord(long time, double value, MetricPathCacheEntry entry,String tsdrMetricKey){
        TSDRMetricRecordBuilder rb = new TSDRMetricRecordBuilder();
        rb.setMetricName(entry.MetricName);
        rb.setMetricValue(new BigDecimal(value));
        rb.setNodeID(entry.NodeID);
        rb.setRecordKeys(FormatUtil.getRecordKeysFromTSDRKey(tsdrMetricKey));
        rb.setTimeStamp(time);
        rb.setTSDRDataCategory(entry.TSDRDataCategory);
        return rb.build();
    }

    private static final TSDRLogRecord getTSDRLogRecord(long time,String value,int index,MetricPathCacheEntry entry,String tdsrLogKey){
        TSDRLogRecordBuilder lb = new TSDRLogRecordBuilder();
        lb.setTSDRDataCategory(entry.TSDRDataCategory);
        lb.setTimeStamp(time);
        lb.setRecordKeys(FormatUtil.getRecordKeysFromTSDRKey(tdsrLogKey));
        lb.setNodeID(entry.NodeID);
        lb.setIndex(index);
        lb.setRecordAttributes(null);
        lb.setRecordFullText(value);
        return lb.build();
    }

    private void loadPathCache(){
        ResultSet rs = session.execute("select * from MetricPath limit 100000");
        for(Row r:rs.all()){
            this.pathCache.put(r.getString("KeyPath"), new MetricPathCacheEntry(r.getLong("KeyA"),r.getLong("KeyB"), DataCategory.valueOf(r.getString("TSDRDataCategory")),
                                 r.getString("NodeID"),r.getString("MetricName")));
        }
    }

    private class MetricPathCacheEntry {
        private long keyA = -1;
        private long keyB = -1;
        private DataCategory TSDRDataCategory = null;
        private String NodeID = null;
        private String MetricName = null;

        public MetricPathCacheEntry(long _keyA,long _keyB, DataCategory _category, String _nodeid, String _metricName){
            this.keyA = _keyA;
            this.keyB = _keyB;
            this.TSDRDataCategory = _category;
            this.NodeID = _nodeid;
            this.MetricName = _metricName;
        }

        public String toString(long time,double value){
            StringBuilder sb = new StringBuilder("|");
            sb.append(TSDRDataCategory.toString()).append("|");
            sb.append(NodeID).append("|");
            sb.append(MetricName).append("|");
            sb.append(new Date(time)).append("|");
            sb.append(value).append("|");
            return sb.toString();
        }
    }

    public void shutdown(){
        if(this.session!=null){
            try{this.session.close();}catch(Exception err){log.error("Failed to close the cassandra session",err);}
        }
    }
}
