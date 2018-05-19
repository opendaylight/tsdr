/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.dataquery;

import static org.mockito.Matchers.any;

import java.math.BigDecimal;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.StoreTSDRLogRecordInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.StoreTSDRLogRecordOutputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.TsdrLogDataService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.StoreTSDRMetricRecordInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.StoreTSDRMetricRecordOutputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.TsdrMetricDataService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.dataquery.impl.rev150219.AddLogInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.dataquery.impl.rev150219.AddMetricInputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

/**
 * Unit tests for TSDRNbiServiceImpl.
 *
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class TSDRNbiServiceImplTest {
    private final TsdrMetricDataService metricDataService = Mockito.mock(TsdrMetricDataService.class);
    private final TsdrLogDataService logDataService = Mockito.mock(TsdrLogDataService.class);
    private final RpcProviderRegistry rpcRegistry = Mockito.mock(RpcProviderRegistry.class);

    private final TSDRNbiServiceImpl impl = new TSDRNbiServiceImpl(metricDataService, logDataService);

    @Before
    public void before() {
        Mockito.when(rpcRegistry.getRpcService(TsdrMetricDataService.class)).thenReturn(metricDataService);
        Mockito.when(metricDataService.storeTSDRMetricRecord(any()))
                .thenReturn(RpcResultBuilder.success(new StoreTSDRMetricRecordOutputBuilder().build()).buildFuture());
        Mockito.when(logDataService.storeTSDRLogRecord(any()))
                .thenReturn(RpcResultBuilder.success(new StoreTSDRLogRecordOutputBuilder().build()).buildFuture());
    }

    @Test
    public void testAddMetricsWithServiceInInput() {
        AddMetricInputBuilder input = new AddMetricInputBuilder();
        input.setMetricName("Test");
        input.setMetricValue(new BigDecimal(10));
        input.setNodeID("Test");
        input.setTimeStamp(System.currentTimeMillis());
        input.setTSDRDataCategory(DataCategory.EXTERNAL);
        input.setRecordKeys(new ArrayList<RecordKeys>());
        impl.addMetric(input.build());
        Mockito.verify(metricDataService, Mockito.atLeastOnce())
                .storeTSDRMetricRecord(Mockito.any(StoreTSDRMetricRecordInput.class));
    }

    @Test
    public void testAddMetricsWithoutServiceInInput() {
        AddMetricInputBuilder input = new AddMetricInputBuilder();
        input.setMetricName("Test");
        input.setMetricValue(new BigDecimal(10));
        input.setNodeID("Test");
        input.setTimeStamp(System.currentTimeMillis());
        input.setTSDRDataCategory(DataCategory.EXTERNAL);
        input.setRecordKeys(new ArrayList<RecordKeys>());
        impl.addMetric(input.build());
        Mockito.verify(metricDataService, Mockito.atLeastOnce())
                .storeTSDRMetricRecord(Mockito.any(StoreTSDRMetricRecordInput.class));
    }

    @Test
    public void testAddLogsWithServiceInInput() {
        AddLogInputBuilder input = new AddLogInputBuilder();
        input.setRecordFullText("Test");
        input.setNodeID("Test");
        input.setTimeStamp(System.currentTimeMillis());
        input.setTSDRDataCategory(DataCategory.EXTERNAL);
        input.setRecordKeys(new ArrayList<RecordKeys>());
        impl.addLog(input.build());
        Mockito.verify(logDataService, Mockito.atLeastOnce())
                .storeTSDRLogRecord(Mockito.any(StoreTSDRLogRecordInput.class));
    }

    @Test
    public void testAddLogsWithoutServiceInInput() {
        AddLogInputBuilder input = new AddLogInputBuilder();
        input.setRecordFullText("Test");
        input.setNodeID("Test");
        input.setTimeStamp(System.currentTimeMillis());
        input.setTSDRDataCategory(DataCategory.EXTERNAL);
        input.setRecordKeys(new ArrayList<RecordKeys>());
        impl.addLog(input.build());
        Mockito.verify(logDataService, Mockito.atLeastOnce())
                .storeTSDRLogRecord(Mockito.any(StoreTSDRLogRecordInput.class));
    }
}
