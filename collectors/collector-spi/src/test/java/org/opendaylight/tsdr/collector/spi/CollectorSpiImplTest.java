/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.collector.spi;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.StoreTSDRLogRecordOutputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.TsdrLogDataService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.StoreTSDRMetricRecordOutputBuilder;
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
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

/**
 * Unit tests for CollectorSpiImpl.
 *
 * @author <a href="mailto:saichler@gmail.com">Sharon Aicler</a>
 */
public class CollectorSpiImplTest {
    private final TsdrMetricDataService metricDataService = Mockito.mock(TsdrMetricDataService.class);
    private final TsdrLogDataService logDataService = Mockito.mock(TsdrLogDataService.class);
    private final CollectorSPIImpl impl = new CollectorSPIImpl(metricDataService,logDataService);

    @Before
    public void setup(){

    }

    @Before
    public void before() throws InterruptedException, ExecutionException {
        doReturn(RpcResultBuilder.success(new StoreTSDRMetricRecordOutputBuilder().build()).buildFuture())
                .when(metricDataService).storeTSDRMetricRecord(any());
        doReturn(RpcResultBuilder.success(new StoreTSDRLogRecordOutputBuilder().build()).buildFuture())
                .when(logDataService).storeTSDRLogRecord(any());
    }

    private static List<TSDRMetricRecord> createTSDRMetricRecordList() {
        TSDRMetricRecordBuilder recBuilder = new TSDRMetricRecordBuilder();
        recBuilder.setTSDRDataCategory(DataCategory.EXTERNAL);
        recBuilder.setTimeStamp(System.currentTimeMillis());
        RecordKeysBuilder keysBuilder = new RecordKeysBuilder();
        keysBuilder.setKeyName("Test");
        keysBuilder.setKeyValue("Test");
        List<RecordKeys> recList = new ArrayList<>();
        recList.add(keysBuilder.build());
        recBuilder.setRecordKeys(recList);
        recBuilder.setNodeID("TestNode");
        recBuilder.setMetricName("Test Metric");
        recBuilder.setMetricValue(new BigDecimal(123));
        return Collections.singletonList(recBuilder.build());
    }

    private static List<TSDRLogRecord> createTSDRLogRecordList() {
        TSDRLogRecordBuilder recBuilder = new TSDRLogRecordBuilder();
        recBuilder.setTSDRDataCategory(DataCategory.EXTERNAL);
        recBuilder.setTimeStamp(System.currentTimeMillis());
        RecordKeysBuilder keysBuilder = new RecordKeysBuilder();
        keysBuilder.setKeyName("Test");
        keysBuilder.setKeyValue("Test");
        List<RecordKeys> recList = new ArrayList<>();
        recList.add(keysBuilder.build());
        recBuilder.setRecordKeys(recList);
        recBuilder.setNodeID("TestNode");
        recBuilder.setRecordFullText("Syslog test");
        return Collections.singletonList(recBuilder.build());
    }

    @Test
    public void testInsertTSDRMetricRecord() {
        InsertTSDRMetricRecordInputBuilder builder = new InsertTSDRMetricRecordInputBuilder();
        builder.setCollectorCodeName("Test");
        builder.setTSDRMetricRecord(createTSDRMetricRecordList());
        impl.insertTSDRMetricRecord(builder.build());
    }

    @Test
    public void testInsertTSDRLogRecord() {
        InsertTSDRLogRecordInputBuilder builder = new InsertTSDRLogRecordInputBuilder();
        builder.setCollectorCodeName("Test");
        builder.setTSDRLogRecord(createTSDRLogRecordList());
        impl.insertTSDRLogRecord(builder.build());
    }
}
