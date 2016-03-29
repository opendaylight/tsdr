/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.spi.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.binary.data.rev160325.storetsdrbinaryrecord.input.TSDRBinaryRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
   The TSDRKeyCache is build to mediate between a full String TSDR Key String to a MD5 hash so
   a metric/log id string identifies isn't persisted on each sample, but only the md5 hash is.
   The TSDRCacheEntry contains all the static data for a single metric so when a metric is being queried
   most of the TSDRMetricRecord or TSDRLogRecord is taken from the same cache instance and only the timestamp and
   the value is taken from the row record.
 * @author - Sharon Aicler (saichler@gmail.com)
 */
public class TSDRKeyCache {

    private static final Logger LOG = LoggerFactory.getLogger(TSDRKeyCache.class);

    public static final String TSDR_KEY_CACHE_FILENAME = "tsdr/tsdrKeyCache.txt";
    //The main cache mapping between the TSDRKey to the TSDRCacheEntry
    private final Map<String,TSDRCacheEntry> cache = new ConcurrentHashMap<>();
    //The mapping between the MD5 and the TSDRCacheEntry
    private final Map<MD5ID,TSDRCacheEntry> md52CacheEntry = new ConcurrentHashMap<>();
    //File that serves as the Key Store.
    private FileOutputStream cacheStore = null;

    public TSDRKeyCache(){
        File dir = new File("tsdr");
        if(!dir.exists()){
            dir.mkdirs();
        }
        try {
            loadTSDRCacheKey();
            File file = new File(TSDR_KEY_CACHE_FILENAME);
            cacheStore = new FileOutputStream(file,true);
        } catch (IOException e) {
            LOG.error("Failed to load key cache",e);
        }
    }

    private void loadTSDRCacheKey() throws IOException {
        synchronized(cache) {
            File file = new File(TSDR_KEY_CACHE_FILENAME);
            if(file.exists() && file.length()>0){
                BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                String tsdrKey = in.readLine();
                while (tsdrKey!=null){
                    addTSDRCacheEntry(tsdrKey.substring(0,tsdrKey.indexOf("|")),false);
                    tsdrKey = in.readLine();
                }
                in.close();
            }
        }
    }

    public final TSDRCacheEntry getCacheEntry(final String tsdrKey){
        return this.cache.get(tsdrKey);
    }

    public final TSDRCacheEntry getCacheEntry(final MD5ID md5ID){
        return this.md52CacheEntry.get(md5ID);
    }

    public final TSDRCacheEntry addTSDRCacheEntry(final String tsdrKey){
        return addTSDRCacheEntry(tsdrKey,true);
    }

    private final TSDRCacheEntry addTSDRCacheEntry(final String tsdrKey,boolean save){
        final TSDRCacheEntry entry = new TSDRCacheEntry(tsdrKey);
        if(this.cache.get(entry.getTsdrKey())==null) {
            this.cache.put(entry.getTsdrKey(), entry);
            this.md52CacheEntry.put(entry.getMd5ID(), entry);
            if(save && cacheStore!=null){
                try {
                    synchronized(cache) {
                        cacheStore.write(tsdrKey.getBytes());
                        cacheStore.write('|');
                        cacheStore.write((""+entry.getMd5ID().getMd5Long1()).getBytes());
                        cacheStore.write('|');
                        cacheStore.write((""+entry.getMd5ID().getMd5Long2()).getBytes());
                        cacheStore.write("\n".getBytes());
                        cacheStore.flush();
                    }
                }catch(IOException e){
                    LOG.error("Failed to save to key cache store",e);
                    if(cacheStore!=null) {
                        try {
                            cacheStore.close();
                        } catch (IOException err) {
                            LOG.error("Failed to close the cache store", e);
                        }
                    }
                    cacheStore = null;
                }
            }
            return entry;
        }
        return this.cache.get(entry.getTsdrKey());
    }

    /**
        When the TSDR Pseudo Key is a general one and a few records in the cache fits that pseudo key,
        the persistence layer should create a Job that this method will utilize to collect the amount of records requested while iterating
        over the exact keys.
        @param tsdrMetricKey - The psudo metric key
        @param startDateTime - The start time
        @param endDateTime - The end time
        @param recordLimit - The number of records to collect
        @param job - The Persistence Layer Job implementation
        @return  - A list of TSDR Metric Records.
     **/
    public List<TSDRMetricRecord> getTSDRMetricRecords(String tsdrMetricKey, long startDateTime, long endDateTime, int recordLimit, TSDRMetricCollectJob job) {
        String dataCategory = FormatUtil.getDataCategoryFromTSDRKey(tsdrMetricKey);
        //In case the dataCategory is null, it may be that the source
        //of the call is from the tsdr:list command, hence the tsdrKey
        //is actually a Data Category. in this case, try to see if the TSDRKey
        //is a data category.
        if(dataCategory==null){
            try{
                DataCategory dc = DataCategory.valueOf(tsdrMetricKey);
                dataCategory = dc.name();
            }catch(Exception e){
                LOG.trace("TSDR Metric Key {} is not a DataCategory",tsdrMetricKey);
            }
        }
        String nodeID = FormatUtil.getNodeIdFromTSDRKey(tsdrMetricKey);
        String metricName = FormatUtil.getMetriNameFromTSDRKey(tsdrMetricKey);
        List<RecordKeys> recKeys = FormatUtil.getRecordKeysFromTSDRKey(tsdrMetricKey);

        if(dataCategory!=null){
            dataCategory+="]";
        }
        if(metricName!=null){
            metricName+="]";
        }
        if(nodeID!=null){
            nodeID+="]";
        }

        final List<TSDRMetricRecord> result = new ArrayList<>();

        for(TSDRCacheEntry e:this.cache.values()){
            if(dataCategory!=null && e.getTsdrKey().indexOf(dataCategory)==-1){
                continue;
            }
            if(nodeID!=null && e.getTsdrKey().indexOf(nodeID)==-1){
                continue;
            }
            if(metricName!=null && e.getTsdrKey().indexOf(metricName)==-1){
                continue;
            }
            if(recKeys!=null){
                boolean fitCriteria = true;
                for(RecordKeys r:recKeys){
                    if((e.getTsdrKey().indexOf(","+r.getKeyName()+":")==-1 && e.getTsdrKey().indexOf("[RK="+r.getKeyName()+":")==-1) ||
                            (e.getTsdrKey().indexOf(":"+r.getKeyValue()+"]")==-1 && e.getTsdrKey().indexOf(":"+r.getKeyValue()+",")==-1)){
                        fitCriteria = false;
                        break;
                    }
                }
                if(!fitCriteria){
                    continue;
                }
            }
            job.collectMetricRecords(e,startDateTime,endDateTime,recordLimit,result);
            if(result.size()>=recordLimit){
                break;
            }
        }
        return result;

    }

    /**
     When the TSDR Pseudo Key is a general one and a few records in the cache fits that pseudo key,
     the persistence layer should create a Job that this method will utilize to collect the amount of records requested while iterating
     over the exact keys.
     @param tsdrLogKey - The psudo log key
     @param startDateTime - The start time
     @param endDateTime - The end time
     @param recordLimit - The number of records to collect
     @param job - The Persistence Layer Job implementation
     @return  - A list of TSDR Log Records.
     **/
    public List<TSDRLogRecord> getTSDRLogRecords(String tsdrLogKey, long startDateTime, long endDateTime, int recordLimit, TSDRLogCollectJob job) {
        String dataCategory = FormatUtil.getDataCategoryFromTSDRKey(tsdrLogKey);
        //In case the dataCategory is null, it may be that the source
        //of the call is from the tsdr:list command, hence the tsdrKey
        //is actually a Data Category. in this case, try to see if the TSDRKey
        //is a data category.
        if(dataCategory==null){
            try{
                DataCategory dc = DataCategory.valueOf(tsdrLogKey);
                dataCategory = dc.name();
            }catch(Exception e){
                LOG.trace("TSDR Log Key {} is not a Data Category.",tsdrLogKey);
            }
        }
        String nodeID = FormatUtil.getNodeIdFromTSDRKey(tsdrLogKey);
        String metricName = FormatUtil.getMetriNameFromTSDRKey(tsdrLogKey);
        List<RecordKeys> recKeys = FormatUtil.getRecordKeysFromTSDRKey(tsdrLogKey);

        if(dataCategory!=null){
            dataCategory+="]";
        }
        if(metricName!=null){
            metricName+="]";
        }
        if(nodeID!=null){
            nodeID+="]";
        }

        final List<TSDRLogRecord> result = new ArrayList<>();

        for(TSDRCacheEntry e:this.cache.values()){
            if(dataCategory!=null && e.getTsdrKey().indexOf(dataCategory)==-1){
                continue;
            }
            if(nodeID!=null && e.getTsdrKey().indexOf(nodeID)==-1){
                continue;
            }
            if(metricName!=null && e.getTsdrKey().indexOf(metricName)==-1){
                continue;
            }
            if(recKeys!=null){
                boolean fitCriteria = true;
                for(RecordKeys r:recKeys){
                    int keyFit = 0;
                    for(RecordKeys er:e.getRecordKeys()){
                        if(er.getKeyName().equals(r.getKeyName()) && er.getKeyValue().equals(r.getKeyValue())){
                            keyFit++;
                        }
                    }
                    if(keyFit==0){
                        fitCriteria = false;
                        break;
                    }
                    /*
                    if((e.getTsdrKey().indexOf(","+r.getKeyName()+":")==-1 && e.getTsdrKey().indexOf("[RK="+r.getKeyName()+":")==-1) ||
                            (e.getTsdrKey().indexOf(":"+r.getKeyValue()+"]")==-1 && e.getTsdrKey().indexOf(":"+r.getKeyValue()+",")==-1)){
                        fitCriteria = false;
                        break;
                    }*/
                }
                if(!fitCriteria){
                    continue;
                }
            }
            job.collectLogRecords(e,startDateTime,endDateTime,recordLimit,result);
            if(result.size()>=recordLimit){
                break;
            }
        }
        return result;

    }

    /**
     When the TSDR Pseudo Key is a general one and a few records in the cache fits that pseudo key,
     the persistence layer should create a Job that this method will utilize to collect the amount of records requested while iterating
     over the exact keys.
     @param tsdrBinaryKey - The psudo log key
     @param startDateTime - The start time
     @param endDateTime - The end time
     @param recordLimit - The number of records to collect
     @param job - The Persistence Layer Job implementation
     @return  - A list of TSDR Binary Records.
     **/
    public List<TSDRBinaryRecord> getTSDRBinaryRecords(String tsdrBinaryKey, long startDateTime, long endDateTime, int recordLimit, TSDRBinaryCollectJob job) {
        String dataCategory = FormatUtil.getDataCategoryFromTSDRKey(tsdrBinaryKey);
        //In case the dataCategory is null, it may be that the source
        //of the call is from the tsdr:list command, hence the tsdrKey
        //is actually a Data Category. in this case, try to see if the TSDRKey
        //is a data category.
        if(dataCategory==null){
            try{
                DataCategory dc = DataCategory.valueOf(tsdrBinaryKey);
                dataCategory = dc.name();
            }catch(Exception e){
                LOG.trace("TSDR Binary Key {} is not a Data Category.",tsdrBinaryKey);
            }
        }
        String nodeID = FormatUtil.getNodeIdFromTSDRKey(tsdrBinaryKey);
        String metricName = FormatUtil.getMetriNameFromTSDRKey(tsdrBinaryKey);
        List<RecordKeys> recKeys = FormatUtil.getRecordKeysFromTSDRKey(tsdrBinaryKey);

        if(dataCategory!=null){
            dataCategory+="]";
        }
        if(metricName!=null){
            metricName+="]";
        }
        if(nodeID!=null){
            nodeID+="]";
        }

        final List<TSDRBinaryRecord> result = new ArrayList<>();

        for(TSDRCacheEntry e:this.cache.values()){
            if(dataCategory!=null && e.getTsdrKey().indexOf(dataCategory)==-1){
                continue;
            }
            if(nodeID!=null && e.getTsdrKey().indexOf(nodeID)==-1){
                continue;
            }
            if(metricName!=null && e.getTsdrKey().indexOf(metricName)==-1){
                continue;
            }
            if(recKeys!=null){
                boolean fitCriteria = true;
                for(RecordKeys r:recKeys){
                    int keyFit = 0;
                    for(RecordKeys er:e.getRecordKeys()){
                        if(er.getKeyName().equals(r.getKeyName()) && er.getKeyValue().equals(r.getKeyValue())){
                            keyFit++;
                        }
                    }
                    if(keyFit==0){
                        fitCriteria = false;
                        break;
                    }
                    /*
                    if((e.getTsdrKey().indexOf(","+r.getKeyName()+":")==-1 && e.getTsdrKey().indexOf("[RK="+r.getKeyName()+":")==-1) ||
                            (e.getTsdrKey().indexOf(":"+r.getKeyValue()+"]")==-1 && e.getTsdrKey().indexOf(":"+r.getKeyValue()+",")==-1)){
                        fitCriteria = false;
                        break;
                    }*/
                }
                if(!fitCriteria){
                    continue;
                }
            }
            job.collectBinaryRecords(e,startDateTime,endDateTime,recordLimit,result);
            if(result.size()>=recordLimit){
                break;
            }
        }
        return result;

    }

    public static interface TSDRMetricCollectJob {
        public void collectMetricRecords(TSDRCacheEntry entry,long startDateTime, long endDateTime,int recordLimit,List<TSDRMetricRecord> globalResult);
    }

    public static interface TSDRLogCollectJob {
        public void collectLogRecords(TSDRCacheEntry entry,long startDateTime, long endDateTime,int recordLimit,List<TSDRLogRecord> globalResult);
    }

    public static interface TSDRBinaryCollectJob {
        public void collectBinaryRecords(TSDRCacheEntry entry,long startDateTime, long endDateTime,int recordLimit,List<TSDRBinaryRecord> globalResult);
    }

    public Collection<TSDRCacheEntry> getAll(){
        return this.cache.values();
    }

    //Cache entry
    public static class TSDRCacheEntry {
        private final String tsdrKey;
        private final MD5ID md5ID;
        private final DataCategory dataCategory;
        private final String nodeID;
        private final String metricName;
        private final List<RecordKeys> recordKeys;

        public TSDRCacheEntry(String tsdrKey){
            this.tsdrKey = tsdrKey;
            this.md5ID = MD5ID.createTSDRID(this.tsdrKey);
            this.dataCategory = DataCategory.valueOf(FormatUtil.getDataCategoryFromTSDRKey(this.tsdrKey));
            this.nodeID = FormatUtil.getNodeIdFromTSDRKey(this.tsdrKey);
            this.metricName = FormatUtil.getMetriNameFromTSDRKey(this.tsdrKey);
            this.recordKeys = FormatUtil.getRecordKeysFromTSDRKey(this.tsdrKey);
        }

        public String getTsdrKey() {
            return tsdrKey;
        }

        public MD5ID getMd5ID() {
            return md5ID;
        }

        public DataCategory getDataCategory() {
            return dataCategory;
        }

        public String getNodeID() {
            return nodeID;
        }

        public String getMetricName() {
            return metricName;
        }

        public List<RecordKeys> getRecordKeys() {
            return recordKeys;
        }
    }

    public void shutdown(){
        try {
            this.cacheStore.close();
        } catch (IOException e) {
            LOG.error("Failed to close the cache store file.",e);
        }
    }
}
