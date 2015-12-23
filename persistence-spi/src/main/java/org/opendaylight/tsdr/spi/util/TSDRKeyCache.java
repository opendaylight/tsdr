/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.spi.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;

/**
   The TSDRKeyCache is build to mediate between a full String TSDR Key String to a MD5 hash so
   a metric/log id string identifies isn't persisted on each sample, but only the md5 hash is.
   The TSDRCacheEntry contains all the static data for a single metric so when a metric is being queried
   most of the TSDRMetricRecord or TSDRLogRecord is taken from the same cache instance and only the timestamp and
   the value is taken from the row record.
 * @author - Sharon Aicler (saichler@gmail.com)
 */
public class TSDRKeyCache {

    //The main cache mapping between the TSDRKey to the TSDRCacheEntry
    private final Map<String,TSDRCacheEntry> cache = new ConcurrentHashMap<>();
    //The mapping between the MD5 and the TSDRCacheEntry
    private final Map<MD5ID,TSDRCacheEntry> md52CacheEntry = new ConcurrentHashMap<>();

    public TSDRKeyCache(){
    }

    public final TSDRCacheEntry getCacheEntry(final String tsdrKey){
        return this.cache.get(tsdrKey);
    }

    public final TSDRCacheEntry getCacheEntry(final MD5ID md5ID){
        return this.md52CacheEntry.get(md5ID);
    }

    public final TSDRCacheEntry addTSDRCacheEntry(final String tsdrKey){
        final TSDRCacheEntry entry = new TSDRCacheEntry(tsdrKey);
        this.cache.put(entry.getTsdrKey(),entry);
        this.md52CacheEntry.put(entry.getMd5ID(),entry);
        return entry;
    }

    /**
       This method receive as an input a TSDR Key and scan the cache for those entries
       That match those part of the key that have values. For example if the passed tsdr key is
       something like "[NID=openflow:1][DC=PORTSTATS][MN=][RK=]" this method will retrieve all
       the entries that belong to both openflow:1 node and are of type PORTSTATS.
     * @param tsdrKey - The TSDR Key, can be partial but needs to contain all the tags but not all the values.
     * @return - A list of cache entries.
     */
    public List<TSDRCacheEntry> findMatchingTSDRCacheEntries(String tsdrKey){
        String dataCategory = FormatUtil.getDataCategoryFromTSDRKey(tsdrKey);
        //In case the dataCategory is null, it may be that the source
        //of the call is from the tsdr:list command, hence the tsdrKey
        //is actually a Data Category. in this case, try to see if the TSDRKey
        //is a data category.
        if(dataCategory==null){
            try{
                DataCategory dc = DataCategory.valueOf(tsdrKey);
                dataCategory = dc.name();
            }catch(Exception e){/*Don't care*/}
        }
        String nodeID = FormatUtil.getNodeIdFromTSDRKey(tsdrKey);
        String metricName = FormatUtil.getMetriNameFromTSDRKey(tsdrKey);
        List<RecordKeys> recKeys = FormatUtil.getRecordKeysFromTSDRKey(tsdrKey);
        List<TSDRCacheEntry> result = new ArrayList<>();

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
                    if(e.getTsdrKey().indexOf(r.getKeyName())==-1 || e.getTsdrKey().indexOf(r.getKeyValue())==-1){
                        fitCriteria = false;
                        break;
                    }
                }
                if(!fitCriteria){
                    continue;
                }
            }
            result.add(e);
        }
        return result;
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

}
