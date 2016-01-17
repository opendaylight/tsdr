/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.spi.util;

import java.io.File;
import java.util.Collection;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.tsdr.spi.util.TSDRKeyCache.TSDRCacheEntry;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrlogrecord.input.TSDRLogRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;

/**
 * @author saichler@gmail.com
 **/
public class TSDRKeyCacheTest {

    private static final String TSDR_TEST_KEY = "[NID=127.0.0.1][DC=EXTERNAL][MN=Memory][RK=hello:world,Testing:test]";
    private TSDRKeyCache keyCache = null;

    @Before
    public void before(){
        keyCache = new TSDRKeyCache();
    }

    @After
    public void after(){
        keyCache.shutdown();
        File dir = new File("./tsdr");
        File[] files = dir.listFiles();
        for(File f:files){
            f.delete();
        }
        dir.delete();
    }

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

    private class TestMetricJob implements TSDRKeyCache.TSDRMetricCollectJob {
        @Override
        public void collectMetricRecords(TSDRCacheEntry entry, long startDateTime, long endDateTime, int recordLimit, List<TSDRMetricRecord> globalResult) {
            TSDRMetricRecordBuilder b = new TSDRMetricRecordBuilder();
            b.setNodeID(entry.getNodeID());
            b.setTSDRDataCategory(entry.getDataCategory());
            b.setMetricName(entry.getMetricName());
            b.setRecordKeys(entry.getRecordKeys());
            b.setTimeStamp(System.currentTimeMillis());
            globalResult.add(b.build());
        }
    }

    private class TestLogJob implements TSDRKeyCache.TSDRLogCollectJob {
        @Override
        public void collectLogRecords(TSDRCacheEntry entry, long startDateTime, long endDateTime, int recordLimit, List<TSDRLogRecord> globalResult) {
            TSDRLogRecordBuilder b = new TSDRLogRecordBuilder();
            b.setNodeID(entry.getNodeID());
            b.setTSDRDataCategory(entry.getDataCategory());
            b.setRecordKeys(entry.getRecordKeys());
            b.setTimeStamp(System.currentTimeMillis());
            globalResult.add(b.build());
        }
    }

    @Test
    public void testMetricByNode(){
        keyCache.addTSDRCacheEntry(TSDR_TEST_KEY);
        List<TSDRMetricRecord> list = keyCache.getTSDRMetricRecords(FormatUtil.KEY_NODEID+FormatUtil.getNodeIdFromTSDRKey(TSDR_TEST_KEY)+"]",0,Long.MAX_VALUE,1000,new TestMetricJob());
        Assert.assertEquals(1,list.size());
        TSDRMetricRecord entry = list.get(0);
        Assert.assertEquals("127.0.0.1",entry.getNodeID());
        Assert.assertEquals(DataCategory.EXTERNAL,entry.getTSDRDataCategory());
        Assert.assertEquals("Memory",entry.getMetricName());
        Assert.assertEquals("Testing",entry.getRecordKeys().get(1).getKeyName());
    }


    @Test
    public void testMetricByDataCategory(){
        keyCache.addTSDRCacheEntry(TSDR_TEST_KEY);
        List<TSDRMetricRecord> list = keyCache.getTSDRMetricRecords(FormatUtil.KEY_CATEGORY+FormatUtil.getDataCategoryFromTSDRKey(TSDR_TEST_KEY)+"]",0,Long.MAX_VALUE,1000,new TestMetricJob());
        Assert.assertEquals(1,list.size());
        TSDRMetricRecord entry = list.get(0);
        Assert.assertEquals("127.0.0.1",entry.getNodeID());
        Assert.assertEquals(DataCategory.EXTERNAL,entry.getTSDRDataCategory());
        Assert.assertEquals("Memory",entry.getMetricName());
        Assert.assertEquals("Testing",entry.getRecordKeys().get(1).getKeyName());
    }

    @Test
    public void testMetricByMetricName(){
        keyCache.addTSDRCacheEntry(TSDR_TEST_KEY);
        List<TSDRMetricRecord> list = keyCache.getTSDRMetricRecords(FormatUtil.KEY_METRICNAME+FormatUtil.getMetriNameFromTSDRKey(TSDR_TEST_KEY)+']',0,Long.MAX_VALUE,1000,new TestMetricJob());
        Assert.assertEquals(1,list.size());
        TSDRMetricRecord entry = list.get(0);
        Assert.assertEquals("127.0.0.1",entry.getNodeID());
        Assert.assertEquals(DataCategory.EXTERNAL,entry.getTSDRDataCategory());
        Assert.assertEquals("Memory",entry.getMetricName());
        Assert.assertEquals("Testing",entry.getRecordKeys().get(1).getKeyName());
    }

    @Test
    public void testMetricByRecordKey(){
        keyCache.addTSDRCacheEntry(TSDR_TEST_KEY);
        List<TSDRMetricRecord> list = keyCache.getTSDRMetricRecords(FormatUtil.KEY_RECORDKEYS+"hello:world"+']',0,Long.MAX_VALUE,1000,new TestMetricJob());
        Assert.assertEquals(1,list.size());
        TSDRMetricRecord entry = list.get(0);
        Assert.assertEquals("127.0.0.1",entry.getNodeID());
        Assert.assertEquals(DataCategory.EXTERNAL,entry.getTSDRDataCategory());
        Assert.assertEquals("Memory",entry.getMetricName());
        Assert.assertEquals("Testing",entry.getRecordKeys().get(1).getKeyName());
    }


    @Test
    public void testLogByNode(){
        keyCache.addTSDRCacheEntry(TSDR_TEST_KEY);
        List<TSDRLogRecord> list = keyCache.getTSDRLogRecords(FormatUtil.KEY_NODEID+FormatUtil.getNodeIdFromTSDRKey(TSDR_TEST_KEY)+"]",0,Long.MAX_VALUE,1000,new TestLogJob());
        Assert.assertEquals(1,list.size());
        TSDRLogRecord entry = list.get(0);
        Assert.assertEquals("127.0.0.1",entry.getNodeID());
        Assert.assertEquals(DataCategory.EXTERNAL,entry.getTSDRDataCategory());
        Assert.assertEquals("Testing",entry.getRecordKeys().get(1).getKeyName());
    }


    @Test
    public void testLogByDataCategory(){
        keyCache.addTSDRCacheEntry(TSDR_TEST_KEY);
        List<TSDRLogRecord> list = keyCache.getTSDRLogRecords(FormatUtil.KEY_CATEGORY+FormatUtil.getDataCategoryFromTSDRKey(TSDR_TEST_KEY)+"]",0,Long.MAX_VALUE,1000,new TestLogJob());
        Assert.assertEquals(1,list.size());
        TSDRLogRecord entry = list.get(0);
        Assert.assertEquals("127.0.0.1",entry.getNodeID());
        Assert.assertEquals(DataCategory.EXTERNAL,entry.getTSDRDataCategory());
        Assert.assertEquals("Testing",entry.getRecordKeys().get(1).getKeyName());
    }

    @Test
    public void testLogByMetricName(){
        keyCache.addTSDRCacheEntry(TSDR_TEST_KEY);
        List<TSDRLogRecord> list = keyCache.getTSDRLogRecords(FormatUtil.KEY_METRICNAME+FormatUtil.getMetriNameFromTSDRKey(TSDR_TEST_KEY)+']',0,Long.MAX_VALUE,1000,new TestLogJob());
        Assert.assertEquals(1,list.size());
        TSDRLogRecord entry = list.get(0);
        Assert.assertEquals("127.0.0.1",entry.getNodeID());
        Assert.assertEquals(DataCategory.EXTERNAL,entry.getTSDRDataCategory());
        Assert.assertEquals("Testing",entry.getRecordKeys().get(1).getKeyName());
    }

    @Test
    public void testLogByRecordKey(){
        keyCache.addTSDRCacheEntry(TSDR_TEST_KEY);
        List<TSDRLogRecord> list = keyCache.getTSDRLogRecords(FormatUtil.KEY_RECORDKEYS+"hello:world"+']',0,Long.MAX_VALUE,1000,new TestLogJob());
        Assert.assertEquals(1,list.size());
        TSDRLogRecord entry = list.get(0);
        Assert.assertEquals("127.0.0.1",entry.getNodeID());
        Assert.assertEquals(DataCategory.EXTERNAL,entry.getTSDRDataCategory());
        Assert.assertEquals("Testing",entry.getRecordKeys().get(1).getKeyName());
    }
}
