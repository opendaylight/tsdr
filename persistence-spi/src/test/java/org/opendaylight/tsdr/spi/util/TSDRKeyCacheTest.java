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

    private static final String TSDR_TEST_KEY = "[NID=openflow:1][DC=EXTERNAL][MN=Memory][RK=hello:world,Testing:test]";
    private static final String TSDR_TEST_KEY2 = "[NID=openflow:11][DC=EXTERNAL][MN=Memory][RK=hello:world,Testing:test]";
    private static final String TSDR_TEST_KEY3 = "[NID=openflow:11][DC=EXTERNAL][MN=Memory][RK=he:world,Testing:test]";
    private static final String TSDR_TEST_KEY4 = "[NID=openflow:11][DC=EXTERNAL][MN=Memory][RK=hello:worl,Testing:test]";
    private static final String TSDR_TEST_5080_KEY1 = "[NID=openflow:2][DC=QUEUESTATS][MN=TransmittedPackets][RK=Node:openflow:2,NodeConnector:openflow:2:2,Queue:1]";
    private static final String TSDR_TEST_5080_KEY2 = "[NID=openflow:2][DC=QUEUESTATS][MN=TransmittedPackets][RK=Node:openflow:2,NodeConnector:openflow:2:1,Queue:1]";
    private static final String KEY_5080 = "[NID=][DC=QUEUESTATS][MN=][RK=Queue:2]";
    private static final String KEY_5052 = "[NID=openflow:1][DC=][MN=FLOWTABLESTATS][RK=Table:0]";
    private static final String TSDR_TEST_5052_KEY1 = "[NID=openflow:1][DC=FLOWTABLESTATS][MN=PacketLookup][RK=Node:openflow:1,Table:150]";
    private static final String TSDR_TEST_5052_KEY2 = "[NID=openflow:1][DC=FLOWTABLESTATS][MN=PacketLookup][RK=Node:openflow:1,Table:160]";

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
        Assert.assertEquals("openflow:1",entry.getNodeID());
        Assert.assertEquals(DataCategory.EXTERNAL,entry.getDataCategory());
        Assert.assertEquals("Memory",entry.getMetricName());
        Assert.assertEquals("world",entry.getRecordKeys().get(0).getKeyValue());
    }

    @Test
    public void testGetTsdrCacheEntry(){
        keyCache.addTSDRCacheEntry(TSDR_TEST_KEY);
        TSDRCacheEntry entry = keyCache.getCacheEntry(TSDR_TEST_KEY);
        Assert.assertEquals("openflow:1",entry.getNodeID());
        Assert.assertEquals(DataCategory.EXTERNAL,entry.getDataCategory());
        Assert.assertEquals("Memory",entry.getMetricName());
        Assert.assertEquals("Testing",entry.getRecordKeys().get(1).getKeyName());
    }

    @Test
    public void testGetTsdrCacheEntryByMD5(){
        keyCache.addTSDRCacheEntry(TSDR_TEST_KEY);
        MD5ID md5 = MD5ID.createTSDRID(TSDR_TEST_KEY);
        TSDRCacheEntry entry = keyCache.getCacheEntry(md5);
        Assert.assertEquals("openflow:1",entry.getNodeID());
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
        Assert.assertEquals("openflow:1",entry.getNodeID());
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
        Assert.assertEquals("openflow:1",entry.getNodeID());
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
        Assert.assertEquals("openflow:1",entry.getNodeID());
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
        Assert.assertEquals("openflow:1",entry.getNodeID());
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
        Assert.assertEquals("openflow:1",entry.getNodeID());
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
        Assert.assertEquals("openflow:1",entry.getNodeID());
        Assert.assertEquals(DataCategory.EXTERNAL,entry.getTSDRDataCategory());
        Assert.assertEquals("Testing",entry.getRecordKeys().get(1).getKeyName());
    }


    @Test
    public void testLogByDataCategory(){
        keyCache.addTSDRCacheEntry(TSDR_TEST_KEY);
        List<TSDRLogRecord> list = keyCache.getTSDRLogRecords(FormatUtil.KEY_CATEGORY+FormatUtil.getDataCategoryFromTSDRKey(TSDR_TEST_KEY)+"]",0,Long.MAX_VALUE,1000,new TestLogJob());
        Assert.assertEquals(1,list.size());
        TSDRLogRecord entry = list.get(0);
        Assert.assertEquals("openflow:1",entry.getNodeID());
        Assert.assertEquals(DataCategory.EXTERNAL,entry.getTSDRDataCategory());
        Assert.assertEquals("Testing",entry.getRecordKeys().get(1).getKeyName());
    }

    @Test
    public void testLogByMetricName(){
        keyCache.addTSDRCacheEntry(TSDR_TEST_KEY);
        List<TSDRLogRecord> list = keyCache.getTSDRLogRecords(FormatUtil.KEY_METRICNAME+FormatUtil.getMetriNameFromTSDRKey(TSDR_TEST_KEY)+']',0,Long.MAX_VALUE,1000,new TestLogJob());
        Assert.assertEquals(1,list.size());
        TSDRLogRecord entry = list.get(0);
        Assert.assertEquals("openflow:1",entry.getNodeID());
        Assert.assertEquals(DataCategory.EXTERNAL,entry.getTSDRDataCategory());
        Assert.assertEquals("Testing",entry.getRecordKeys().get(1).getKeyName());
    }

    @Test
    public void testLogByRecordKey(){
        keyCache.addTSDRCacheEntry(TSDR_TEST_KEY);
        List<TSDRLogRecord> list = keyCache.getTSDRLogRecords(FormatUtil.KEY_RECORDKEYS+"hello:world"+']',0,Long.MAX_VALUE,1000,new TestLogJob());
        Assert.assertEquals(1,list.size());
        TSDRLogRecord entry = list.get(0);
        Assert.assertEquals("openflow:1",entry.getNodeID());
        Assert.assertEquals(DataCategory.EXTERNAL,entry.getTSDRDataCategory());
        Assert.assertEquals("Testing",entry.getRecordKeys().get(1).getKeyName());
    }

    @Test
    public void tesBugFixPrefix(){
        keyCache.addTSDRCacheEntry(TSDR_TEST_KEY);
        keyCache.addTSDRCacheEntry(TSDR_TEST_KEY2);
        List<TSDRLogRecord> list = keyCache.getTSDRLogRecords(FormatUtil.KEY_NODEID+"openflow:1]",0,Long.MAX_VALUE,1000,new TestLogJob());
        Assert.assertEquals(1,list.size());
        TSDRLogRecord entry = list.get(0);
        Assert.assertEquals("openflow:1",entry.getNodeID());
        Assert.assertEquals(DataCategory.EXTERNAL,entry.getTSDRDataCategory());
        Assert.assertEquals("Testing",entry.getRecordKeys().get(1).getKeyName());
    }

    @Test
    public void tesBugFixRKKey(){
        keyCache.addTSDRCacheEntry(TSDR_TEST_KEY);
        keyCache.addTSDRCacheEntry(TSDR_TEST_KEY2);
        keyCache.addTSDRCacheEntry(TSDR_TEST_KEY3);
        keyCache.addTSDRCacheEntry(TSDR_TEST_KEY4);
        List<TSDRLogRecord> list = keyCache.getTSDRLogRecords(FormatUtil.KEY_RECORDKEYS+"he:world]",0,Long.MAX_VALUE,1000,new TestLogJob());
        Assert.assertEquals(1,list.size());
        TSDRLogRecord entry = list.get(0);
        Assert.assertEquals("openflow:11",entry.getNodeID());
        Assert.assertEquals(DataCategory.EXTERNAL,entry.getTSDRDataCategory());
        Assert.assertEquals("Testing",entry.getRecordKeys().get(1).getKeyName());
    }

    @Test
    public void tesBugFixRKValue(){
        keyCache.addTSDRCacheEntry(TSDR_TEST_KEY);
        keyCache.addTSDRCacheEntry(TSDR_TEST_KEY2);
        keyCache.addTSDRCacheEntry(TSDR_TEST_KEY3);
        keyCache.addTSDRCacheEntry(TSDR_TEST_KEY4);
        List<TSDRLogRecord> list = keyCache.getTSDRLogRecords(FormatUtil.KEY_RECORDKEYS+"hello:worl]",0,Long.MAX_VALUE,1000,new TestLogJob());
        Assert.assertEquals(1,list.size());
    }

    @Test
    public void tesBug5080(){
        keyCache.addTSDRCacheEntry(TSDR_TEST_5080_KEY1);
        keyCache.addTSDRCacheEntry(TSDR_TEST_5080_KEY2);
        List<TSDRLogRecord> list = keyCache.getTSDRLogRecords(KEY_5080,0,Long.MAX_VALUE,1000,new TestLogJob());
        Assert.assertEquals(0,list.size());
    }

    @Test
    public void tesBug5052(){
        keyCache.addTSDRCacheEntry(TSDR_TEST_5052_KEY1);
        keyCache.addTSDRCacheEntry(TSDR_TEST_5052_KEY2);
        List<TSDRLogRecord> list = keyCache.getTSDRLogRecords(KEY_5052,0,Long.MAX_VALUE,1000,new TestLogJob());
        Assert.assertEquals(0,list.size());
    }
}
