/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.cassandra;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.opendaylight.tsdr.spi.util.FormatUtil;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.InvalidQueryException;
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
        String metricID = FormatUtil.getMetricID(mr);
        MD5Identifier ID = null;
        MetricPathCacheEntry entry = pathCache.get(metricID);

        //if it does not exist, create it
        if(entry==null){
            ID = new MD5Identifier(metricID.getBytes());
            StringBuilder cql = new StringBuilder();
            cql.append("insert into MetricPath (TSDRDataCategory,NodeID,MetricName,KeyPath,KeyA,KeyB) values(");
            cql.append("'").append(mr.getTSDRDataCategory().toString()).append("',");
            cql.append("'").append(mr.getNodeID()).append("',");
            cql.append("'").append(mr.getMetricName()).append("',");
            cql.append("'").append(metricID).append("',");
            cql.append(ID.getA()).append(",");
            cql.append(ID.getB());
            cql.append(")");
            session.execute(cql.toString());
            this.pathCache.put(metricID, new MetricPathCacheEntry(ID.getA(),ID.getB(), mr.getTSDRDataCategory(), mr.getNodeID(), mr.getMetricName()));
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
        String logID = FormatUtil.getLogID(lr);
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

    public List<?> getMetrics(String category, Date startDateTime,Date endDateTime) {
        category = category.replaceAll("%3A", ":");
        MetricPathCacheEntry entry = this.pathCache.get(category);
        List<CassandraEntry> result = new LinkedList<CassandraEntry>();
        if(entry!=null){
            String cql = "select * from MetricVal where KeyA="+entry.keyA+" and KeyB="+entry.keyB+" and Time>="+startDateTime.getTime()+" and Time<="+endDateTime.getTime();
            ResultSet rs = session.execute(cql);
            for(Row r:rs.all()){
                result.add(new CassandraEntry(r.getLong("Time"), r.getDouble("value")));
            }
            return result;
        }
        return result;
    }

    public static class CassandraEntry{
        private long time = -1;
        private double value = -1;
        public CassandraEntry(long _time,double _value){
            this.time = _time;
            this.value = _value;
        }
        public Date getTime(){
            return new Date(this.time);
        }
        public double getValue(){
            return this.value;
        }
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
