/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.collector.spi;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.TsdrLogDataService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.TsdrMetricDataService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRLogRecordInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRMetricRecordInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecordBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrmetricrecord.input.TSDRMetricRecordBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:saichler@gmail.com">Sharon Aicler</a>
 * Created by saichler on 12/20/15.
 */
public class CollectorSpiImplTest {
    private TsdrMetricDataService metricDataService = Mockito.mock(TsdrMetricDataService.class);
    private TsdrLogDataService logDataService = Mockito.mock(TsdrLogDataService.class);
    private CollectorSPIImpl impl = new CollectorSPIImpl(metricDataService,logDataService);

    @Before
    public void setup(){

    }

    private static List<TSDRMetricRecord> createTSDRMetricRecordList(){
        List<TSDRMetricRecord> result = new ArrayList<>();
        TSDRMetricRecordBuilder b = new TSDRMetricRecordBuilder();
        b.setTSDRDataCategory(DataCategory.EXTERNAL);
        b.setTimeStamp(System.currentTimeMillis());
        RecordKeysBuilder rb = new RecordKeysBuilder();
        rb.setKeyName("Test");
        rb.setKeyValue("Test");
        List<RecordKeys> rList = new ArrayList<>();
        rList.add(rb.build());
        b.setRecordKeys(rList);
        b.setNodeID("TestNode");
        b.setMetricName("Test Metric");
        b.setMetricValue(new BigDecimal(123));
        result.add(b.build());
        return result;
    }

    private static List<TSDRLogRecord> createTSDRLogRecordList(){
        List<TSDRLogRecord> result = new ArrayList<>();
        TSDRLogRecordBuilder b = new TSDRLogRecordBuilder();
        b.setTSDRDataCategory(DataCategory.EXTERNAL);
        b.setTimeStamp(System.currentTimeMillis());
        RecordKeysBuilder rb = new RecordKeysBuilder();
        rb.setKeyName("Test");
        rb.setKeyValue("Test");
        List<RecordKeys> rList = new ArrayList<>();
        rList.add(rb.build());
        b.setRecordKeys(rList);
        b.setNodeID("TestNode");
        b.setRecordFullText("Syslog test");
        result.add(b.build());
        return result;
    }

    @Test
    public void testInsertTSDRMetricRecord(){
        InsertTSDRMetricRecordInputBuilder b = new InsertTSDRMetricRecordInputBuilder();
        b.setCollectorCodeName("Test");
        b.setTSDRMetricRecord(createTSDRMetricRecordList());
        impl.insertTSDRMetricRecord(b.build());
    }

    @Test
    public void testInsertTSDRLogRecord(){
        InsertTSDRLogRecordInputBuilder b = new InsertTSDRLogRecordInputBuilder();
        b.setCollectorCodeName("Test");
        b.setTSDRLogRecord(createTSDRLogRecordList());
        impl.insertTSDRLogRecord(b.build());
    }
}
