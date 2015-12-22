/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.spi.util;

import java.text.Normalizer.Form;
import java.util.Collection;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.tsdr.spi.util.TSDRKeyCache.TSDRCacheEntry;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;

/**
 * @author saichler@gmail.com
 **/
public class TSDRKeyCacheTest {

    private static final String TSDR_TEST_KEY = "[NID=127.0.0.1][DC=EXTERNAL][MN=Memory][RK=hello:world,Testing:test]";
    private TSDRKeyCache keyCache = new TSDRKeyCache();

    @Test
    public void testAddTsdrCacheEntry(){
        TSDRCacheEntry entry = keyCache.addTSDRCacheEntry(TSDR_TEST_KEY);
        Assert.assertEquals("127.0.0.1",entry.getNodeID());
        Assert.assertEquals(DataCategory.EXTERNAL,entry.getDataCategory());
        Assert.assertEquals("Memory",entry.getMetricName());
        Assert.assertEquals("world",entry.getRecordKeys().get(0).getKeyValue());
    }

    @Test
    public void testGetTsdrCacheEntry(){
        keyCache.addTSDRCacheEntry(TSDR_TEST_KEY);
        TSDRCacheEntry entry = keyCache.getCacheEntry(TSDR_TEST_KEY);
        Assert.assertEquals("127.0.0.1",entry.getNodeID());
        Assert.assertEquals(DataCategory.EXTERNAL,entry.getDataCategory());
        Assert.assertEquals("Memory",entry.getMetricName());
        Assert.assertEquals("Testing",entry.getRecordKeys().get(1).getKeyName());
    }

    @Test
    public void testGetTsdrCacheEntryByMD5(){
        keyCache.addTSDRCacheEntry(TSDR_TEST_KEY);
        MD5ID md5 = MD5ID.createTSDRID(TSDR_TEST_KEY);
        TSDRCacheEntry entry = keyCache.getCacheEntry(md5);
        Assert.assertEquals("127.0.0.1",entry.getNodeID());
        Assert.assertEquals(DataCategory.EXTERNAL,entry.getDataCategory());
        Assert.assertEquals("Memory",entry.getMetricName());
        Assert.assertEquals("Testing",entry.getRecordKeys().get(1).getKeyName());
    }

    @Test
    public void testGetallKeys(){
        keyCache.addTSDRCacheEntry(TSDR_TEST_KEY);
        Collection<TSDRCacheEntry> all = keyCache.getAll();
        Assert.assertEquals(1,all.size());
        TSDRCacheEntry entry = all.iterator().next();
        Assert.assertEquals("127.0.0.1",entry.getNodeID());
        Assert.assertEquals(DataCategory.EXTERNAL,entry.getDataCategory());
        Assert.assertEquals("Memory",entry.getMetricName());
        Assert.assertEquals("Testing",entry.getRecordKeys().get(1).getKeyName());
    }

    @Test
    public void testFindKeysByNode(){
        keyCache.addTSDRCacheEntry(TSDR_TEST_KEY);
        List<TSDRCacheEntry> list = keyCache.findMatchingTSDRCacheEntries(FormatUtil.KEY_NODEID+FormatUtil.getNodeIdFromTSDRKey(TSDR_TEST_KEY)+"]");
        Assert.assertEquals(1,list.size());
        TSDRCacheEntry entry = list.get(0);
        Assert.assertEquals("127.0.0.1",entry.getNodeID());
        Assert.assertEquals(DataCategory.EXTERNAL,entry.getDataCategory());
        Assert.assertEquals("Memory",entry.getMetricName());
        Assert.assertEquals("Testing",entry.getRecordKeys().get(1).getKeyName());
    }

    @Test
    public void testFindKeysByDataCategory(){
        keyCache.addTSDRCacheEntry(TSDR_TEST_KEY);
        List<TSDRCacheEntry> list = keyCache.findMatchingTSDRCacheEntries(FormatUtil.KEY_CATEGORY+FormatUtil.getDataCategoryFromTSDRKey(TSDR_TEST_KEY)+"]");
        Assert.assertEquals(1,list.size());
        TSDRCacheEntry entry = list.get(0);
        Assert.assertEquals("127.0.0.1",entry.getNodeID());
        Assert.assertEquals(DataCategory.EXTERNAL,entry.getDataCategory());
        Assert.assertEquals("Memory",entry.getMetricName());
        Assert.assertEquals("Testing",entry.getRecordKeys().get(1).getKeyName());
    }

    @Test
    public void testFindKeysByMetricName(){
        keyCache.addTSDRCacheEntry(TSDR_TEST_KEY);
        List<TSDRCacheEntry> list = keyCache.findMatchingTSDRCacheEntries(FormatUtil.KEY_METRICNAME+FormatUtil.getMetriNameFromTSDRKey(TSDR_TEST_KEY)+']');
        Assert.assertEquals(1,list.size());
        TSDRCacheEntry entry = list.get(0);
        Assert.assertEquals("127.0.0.1",entry.getNodeID());
        Assert.assertEquals(DataCategory.EXTERNAL,entry.getDataCategory());
        Assert.assertEquals("Memory",entry.getMetricName());
        Assert.assertEquals("Testing",entry.getRecordKeys().get(1).getKeyName());
    }

    @Test
    public void testFindKeysByRecordKey(){
        keyCache.addTSDRCacheEntry(TSDR_TEST_KEY);
        List<TSDRCacheEntry> list = keyCache.findMatchingTSDRCacheEntries(FormatUtil.KEY_RECORDKEYS+"hello:world"+']');
        Assert.assertEquals(1,list.size());
        TSDRCacheEntry entry = list.get(0);
        Assert.assertEquals("127.0.0.1",entry.getNodeID());
        Assert.assertEquals(DataCategory.EXTERNAL,entry.getDataCategory());
        Assert.assertEquals("Memory",entry.getMetricName());
        Assert.assertEquals("Testing",entry.getRecordKeys().get(1).getKeyName());
    }
}
