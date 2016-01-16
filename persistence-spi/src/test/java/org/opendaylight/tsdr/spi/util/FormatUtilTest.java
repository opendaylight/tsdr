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
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrlogrecord.input.TSDRLogRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrlog.RecordAttributes;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrlog.RecordAttributesBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;

/**
 * @author saichler@gmail.com
 **/
public class FormatUtilTest {
    public static TSDRLogRecord createLogRecord(){
        TSDRLogRecordBuilder b = new TSDRLogRecordBuilder();
        b.setNodeID("Test");
        b.setTimeStamp(System.currentTimeMillis());
        b.setTSDRDataCategory(DataCategory.EXTERNAL);
        b.setRecordFullText("Some syslog text");
        List<RecordKeys> recs = new ArrayList<>();
        RecordKeysBuilder rb = new RecordKeysBuilder();
        rb.setKeyValue("Test1");
        rb.setKeyName("Test2");
        recs.add(rb.build());
        b.setRecordKeys(recs);
        List<RecordAttributes> attributes = new LinkedList<>();
        RecordAttributesBuilder rab = new RecordAttributesBuilder();
        rab.setName("RATest");
        rab.setValue("RAValue");
        attributes.add(rab.build());
        b.setRecordAttributes(attributes);
        return b.build();
    }

    public static TSDRMetricRecord createMetricRecord(){
        TSDRMetricRecordBuilder b = new TSDRMetricRecordBuilder();
        b.setNodeID("Test");
        b.setTimeStamp(System.currentTimeMillis());
        b.setMetricName("Test");
        b.setMetricValue(new BigDecimal(11D));
        b.setTSDRDataCategory(DataCategory.EXTERNAL);
        List<RecordKeys> recs = new ArrayList<>();
        RecordKeysBuilder rb = new RecordKeysBuilder();
        rb.setKeyValue("Test1");
        rb.setKeyName("Test2");
        recs.add(rb.build());
        b.setRecordKeys(recs);
        return b.build();
    }

    @Test
    public void testGetTSDRMetricKey(){
        String key = FormatUtil.getTSDRMetricKey(createMetricRecord());
        Assert.assertEquals("[NID=Test][DC=EXTERNAL][MN=Test][RK=Test2:Test1]",key);
    }

    @Test
    public void testGetTSDRLogKey(){
        String key = FormatUtil.getTSDRLogKey(createLogRecord());
        Assert.assertEquals("[NID=Test][DC=EXTERNAL][RK=Test2:Test1]",key);
    }

    @Test
    public void testGetTSDRLogKeyWithAttributes(){
        String key = FormatUtil.getTSDRLogKeyWithRecordAttributes(createLogRecord());
        Assert.assertEquals("[NID=Test][DC=EXTERNAL][RK=Test2:Test1][RA=RATest:RAValue]",key);
    }

    @Test
    public void testGetTSDRLogAttributesFromKey(){
        String key = FormatUtil.getTSDRLogKeyWithRecordAttributes(createLogRecord());
        List<RecordAttributes> list = FormatUtil.getRecordAttributesFromTSDRKey(key);
        Assert.assertEquals(list.size(),1);
        Assert.assertEquals("RATest",list.get(0).getName());
    }

    @Test
    public void testGetTSDRLogKeyWithTimeStamp(){
        String key = FormatUtil.getTSDRLogKeyWithTimeStamp(createLogRecord());
        List<RecordKeys> list = FormatUtil.getRecordKeysFromTSDRKey(key);
        Assert.assertEquals(list.size(),1);
        Assert.assertEquals("Test2",list.get(0).getKeyName());
    }

    @Test
    public void testGetTSDRMetricKeyWithTimeStamp(){
        String key = FormatUtil.getTSDRMetricKeyWithTimeStamp(createMetricRecord());
        List<RecordKeys> list = FormatUtil.getRecordKeysFromTSDRKey(key);
        Assert.assertEquals(list.size(),1);
        Assert.assertEquals("Test2",list.get(0).getKeyName());
    }

    @Test
    public void testIsValidKey(){
        String key = FormatUtil.getTSDRMetricKey(createMetricRecord());
        Assert.assertTrue(FormatUtil.isValidTSDRKey(key));
    }

    @Test
    public void testGetTimeStampFromTSDRKey(){
        long time = System.currentTimeMillis();
        String key = FormatUtil.getTSDRMetricKeyWithTimeStamp(createMetricRecord());
        long timeStamp = FormatUtil.getTimeStampFromTSDRKey(key);
        Assert.assertTrue(timeStamp>=time && timeStamp<=time+1000);
    }

}
