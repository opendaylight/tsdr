/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.spi.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.tsdrlog.RecordAttributes;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.tsdrlog.RecordAttributesBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;

public class FormatUtilTest {
    public static TSDRLogRecord createLogRecord() {
        TSDRLogRecordBuilder builder = new TSDRLogRecordBuilder();
        builder.setNodeID("Test");
        builder.setTimeStamp(System.currentTimeMillis());
        builder.setTSDRDataCategory(DataCategory.EXTERNAL);
        builder.setRecordFullText("Some syslog text");
        List<RecordKeys> recs = new ArrayList<>();
        RecordKeysBuilder rb = new RecordKeysBuilder();
        rb.setKeyValue("Test1");
        rb.setKeyName("Test2");
        recs.add(rb.build());
        builder.setRecordKeys(recs);
        List<RecordAttributes> attributes = new LinkedList<>();
        RecordAttributesBuilder rab = new RecordAttributesBuilder();
        rab.setName("RATest");
        rab.setValue("RAValue");
        attributes.add(rab.build());
        builder.setRecordAttributes(attributes);
        return builder.build();
    }

    public static TSDRMetricRecord createMetricRecord() {
        TSDRMetricRecordBuilder builder = new TSDRMetricRecordBuilder();
        builder.setNodeID("Test");
        builder.setTimeStamp(System.currentTimeMillis());
        builder.setMetricName("Test");
        builder.setMetricValue(new BigDecimal(11D));
        builder.setTSDRDataCategory(DataCategory.EXTERNAL);
        List<RecordKeys> recs = new ArrayList<>();
        RecordKeysBuilder rb = new RecordKeysBuilder();
        rb.setKeyValue("Test1");
        rb.setKeyName("Test2");
        recs.add(rb.build());
        builder.setRecordKeys(recs);
        return builder.build();
    }

    public static TSDRLogRecord createDataCategoryLogRecord() {
        TSDRLogRecordBuilder builder = new TSDRLogRecordBuilder();
        builder.setTimeStamp(System.currentTimeMillis());
        builder.setTSDRDataCategory(DataCategory.NETFLOW);
        return builder.build();
    }

    public static TSDRMetricRecord createDataCategoryMetricRecord() {
        TSDRMetricRecordBuilder builder = new TSDRMetricRecordBuilder();
        builder.setTimeStamp(System.currentTimeMillis());
        builder.setTSDRDataCategory(DataCategory.EXTERNAL);
        return builder.build();
    }

    public static TSDRLogRecord createLogRecordWithDCAndNID() {
        TSDRLogRecordBuilder builder = new TSDRLogRecordBuilder();
        builder.setTimeStamp(System.currentTimeMillis());
        builder.setTSDRDataCategory(DataCategory.NETFLOW);
        builder.setNodeID("Test");
        builder.setRecordFullText("Some syslog text");
        List<RecordAttributes> attributes = new LinkedList<>();
        RecordAttributesBuilder rab = new RecordAttributesBuilder();
        rab.setName("RATest");
        rab.setValue("RAValue");
        attributes.add(rab.build());
        builder.setRecordAttributes(attributes);
        return builder.build();
    }

    public static TSDRLogRecord createLogRecordWithDCAndRK() {
        TSDRLogRecordBuilder builder = new TSDRLogRecordBuilder();
        builder.setTimeStamp(System.currentTimeMillis());
        builder.setTSDRDataCategory(DataCategory.NETFLOW);
        List<RecordKeys> recs = new ArrayList<>();
        RecordKeysBuilder rb = new RecordKeysBuilder();
        rb.setKeyValue("Test1");
        rb.setKeyName("Test2");
        recs.add(rb.build());
        builder.setRecordKeys(recs);
        builder.setRecordFullText("Some syslog text");
        List<RecordAttributes> attributes = new LinkedList<>();
        RecordAttributesBuilder rab = new RecordAttributesBuilder();
        rab.setName("RATest");
        rab.setValue("RAValue");
        attributes.add(rab.build());
        builder.setRecordAttributes(attributes);
        return builder.build();
    }

    public static TSDRMetricRecord createMetricRecordWithDCAndNID() {
        TSDRMetricRecordBuilder builder = new TSDRMetricRecordBuilder();
        builder.setTimeStamp(System.currentTimeMillis());
        builder.setTSDRDataCategory(DataCategory.SNMPINTERFACES);
        builder.setNodeID("Test");
        return builder.build();
    }

    public static TSDRMetricRecord createMetricRecordWithDCAndMN() {
        TSDRMetricRecordBuilder builder = new TSDRMetricRecordBuilder();
        builder.setTimeStamp(System.currentTimeMillis());
        builder.setTSDRDataCategory(DataCategory.SNMPINTERFACES);
        builder.setMetricName("Test");
        builder.setMetricValue(new BigDecimal(11D));
        return builder.build();
    }

    public static TSDRMetricRecord createMetricRecordWithDCAndRK() {
        TSDRMetricRecordBuilder builder = new TSDRMetricRecordBuilder();
        builder.setTimeStamp(System.currentTimeMillis());
        builder.setTSDRDataCategory(DataCategory.SNMPINTERFACES);
        List<RecordKeys> recs = new ArrayList<>();
        RecordKeysBuilder rb = new RecordKeysBuilder();
        rb.setKeyValue("Test1");
        rb.setKeyName("Test2");
        recs.add(rb.build());
        builder.setRecordKeys(recs);
        return builder.build();
    }

    @Test
    public void testGetTSDRMetricKey() {
        String key = FormatUtil.getTSDRMetricKey(createMetricRecord());
        Assert.assertEquals("[NID=Test][DC=EXTERNAL][MN=Test][RK=Test2:Test1]", key);
    }

    @Test
    public void testGetTSDRLogKey() {
        String key = FormatUtil.getTSDRLogKey(createLogRecord());
        Assert.assertEquals("[NID=Test][DC=EXTERNAL][RK=Test2:Test1]", key);
    }

    @Test
    public void testGetTSDRLogKeyWithAttributes() {
        String key = FormatUtil.getTSDRLogKeyWithRecordAttributes(createLogRecord());
        Assert.assertEquals("[NID=Test][DC=EXTERNAL][RK=Test2:Test1][RA=RATest:RAValue]", key);
    }

    @Test
    public void testGetTSDRLogAttributesFromKey() {
        String key = FormatUtil.getTSDRLogKeyWithRecordAttributes(createLogRecord());
        List<RecordAttributes> list = FormatUtil.getRecordAttributesFromTSDRKey(key);
        Assert.assertEquals(list.size(), 1);
        Assert.assertEquals("RATest", list.get(0).getName());
    }

    @Test
    public void testGetTSDRLogKeyWithTimeStamp() {
        String key = FormatUtil.getTSDRLogKeyWithTimeStamp(createLogRecord());
        List<RecordKeys> list = FormatUtil.getRecordKeysFromTSDRKey(key);
        Assert.assertEquals(list.size(), 1);
        Assert.assertEquals("Test2", list.get(0).getKeyName());
    }

    @Test
    public void testGetTSDRMetricKeyWithTimeStamp() {
        String key = FormatUtil.getTSDRMetricKeyWithTimeStamp(createMetricRecord());
        List<RecordKeys> list = FormatUtil.getRecordKeysFromTSDRKey(key);
        Assert.assertEquals(list.size(), 1);
        Assert.assertEquals("Test2", list.get(0).getKeyName());
    }

    @Test
    public void testIsValidKey() {
        String key = FormatUtil.getTSDRMetricKey(createMetricRecord());
        Assert.assertTrue(FormatUtil.isValidTSDRKey(key));
    }

    @Test
    public void testIsValidTSDRLogKey() {
        String key = FormatUtil.getTSDRLogKey(createLogRecord());
        Assert.assertTrue(FormatUtil.isValidTSDRLogKey(key));
    }

    @Test
    public void testIsDataCategoryKey() {
        // test invalid tsdrMetricKey
        String key = "[NID=][MN=][DC=][RK=]";
        Assert.assertFalse(FormatUtil.isDataCategoryKey(key));
        key = "[NID=][MN=]DC=[RK=]";
        Assert.assertFalse(FormatUtil.isDataCategoryKey(key));
        key = "[DC=PORTSTATS][MN=][RK=]";
        Assert.assertFalse(FormatUtil.isDataCategoryKey(key));
        key = "[NID=][DC=PORTSTATS][MN=]";
        Assert.assertFalse(FormatUtil.isDataCategoryKey(key));
        // test invalid tsdrlogkey
        key = "[NID=][DC=][RK=]";
        Assert.assertFalse(FormatUtil.isDataCategoryKey(key));
        key = "[NID=]DC=[RK=]";
        Assert.assertFalse(FormatUtil.isDataCategoryKey(key));
        key = "[DC=][RK=]";
        Assert.assertFalse(FormatUtil.isDataCategoryKey(key));
        key = "[NID=][DC=PORTSTATS]";
        Assert.assertFalse(FormatUtil.isDataCategoryKey(key));
        // test when the key is a valid tsdrKey
        // test metrickey
        key = FormatUtil.getTSDRMetricKey(createMetricRecordWithDCAndNID());
        Assert.assertFalse(FormatUtil.isDataCategoryKey(key));
        key = FormatUtil.getTSDRMetricKey(createMetricRecordWithDCAndMN());
        Assert.assertFalse(FormatUtil.isDataCategoryKey(key));
        key = FormatUtil.getTSDRMetricKey(createMetricRecordWithDCAndRK());
        Assert.assertFalse(FormatUtil.isDataCategoryKey(key));
        key = FormatUtil.getTSDRMetricKey(createDataCategoryMetricRecord());
        Assert.assertTrue(FormatUtil.isDataCategoryKey(key));
        // test logkey
        key = FormatUtil.getTSDRLogKey(createLogRecordWithDCAndNID());
        Assert.assertFalse(FormatUtil.isDataCategory(key));
        key = FormatUtil.getTSDRLogKey(createLogRecordWithDCAndRK());
        Assert.assertFalse(FormatUtil.isDataCategoryKey(key));
        Assert.assertFalse(FormatUtil.isDataCategory(key));
        key = FormatUtil.getTSDRLogKey(createDataCategoryLogRecord());
        Assert.assertTrue(FormatUtil.isDataCategoryKey(key));
    }

    @Test
    public void testGetTimeStampFromTSDRKey() {
        long time = System.currentTimeMillis();
        String key = FormatUtil.getTSDRMetricKeyWithTimeStamp(createMetricRecord());
        long timeStamp = FormatUtil.getTimeStampFromTSDRKey(key);
        Assert.assertTrue(timeStamp >= time && timeStamp <= time + 1000);
    }

}
