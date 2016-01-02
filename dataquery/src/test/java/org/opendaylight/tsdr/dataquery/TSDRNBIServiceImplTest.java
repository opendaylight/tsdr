/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.dataquery;

import java.math.BigDecimal;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.StoreTSDRLogRecordInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.StoreTSDRMetricRecordInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.dataquery.impl.rev150219.AddLogInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.dataquery.impl.rev150219.AddMetricInputBuilder;

/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class TSDRNBIServiceImplTest {
    private TSDRService tsdrService = Mockito.mock(TSDRService.class);
    private RpcProviderRegistry rpcRegistry = Mockito.mock(RpcProviderRegistry.class);

    @Before
    public void before(){
       Mockito.when(rpcRegistry.getRpcService(TSDRService.class)).thenReturn(tsdrService);
    }

    @Test
    public void testAddMetricsWithServiceInInput(){
        TSDRNBIServiceImpl impl = new TSDRNBIServiceImpl(tsdrService,rpcRegistry);
        AddMetricInputBuilder input = new AddMetricInputBuilder();
        input.setMetricName("Test");
        input.setMetricValue(new BigDecimal(10));
        input.setNodeID("Test");
        input.setTimeStamp(System.currentTimeMillis());
        input.setTSDRDataCategory(DataCategory.EXTERNAL);
        input.setRecordKeys(new ArrayList<RecordKeys>());
        impl.addMetric(input.build());
        Mockito.verify(tsdrService,Mockito.atLeastOnce()).storeTSDRMetricRecord(Mockito.any(StoreTSDRMetricRecordInput.class));
    }

    @Test
    public void testAddMetricsWithoutServiceInInput(){
        TSDRNBIServiceImpl impl = new TSDRNBIServiceImpl(null,rpcRegistry);
        AddMetricInputBuilder input = new AddMetricInputBuilder();
        input.setMetricName("Test");
        input.setMetricValue(new BigDecimal(10));
        input.setNodeID("Test");
        input.setTimeStamp(System.currentTimeMillis());
        input.setTSDRDataCategory(DataCategory.EXTERNAL);
        input.setRecordKeys(new ArrayList<RecordKeys>());
        impl.addMetric(input.build());
        Mockito.verify(tsdrService,Mockito.atLeastOnce()).storeTSDRMetricRecord(Mockito.any(StoreTSDRMetricRecordInput.class));
    }

    @Test
    public void testAddLogsWithServiceInInput(){
        TSDRNBIServiceImpl impl = new TSDRNBIServiceImpl(tsdrService,rpcRegistry);
        AddLogInputBuilder input = new AddLogInputBuilder();
        input.setRecordFullText("Test");
        input.setNodeID("Test");
        input.setTimeStamp(System.currentTimeMillis());
        input.setTSDRDataCategory(DataCategory.EXTERNAL);
        input.setRecordKeys(new ArrayList<RecordKeys>());
        impl.addLog(input.build());
        Mockito.verify(tsdrService,Mockito.atLeastOnce()).storeTSDRLogRecord(Mockito.any(StoreTSDRLogRecordInput.class));
    }

    @Test
    public void testAddLogsWithoutServiceInInput(){
        TSDRNBIServiceImpl impl = new TSDRNBIServiceImpl(null,rpcRegistry);
        AddLogInputBuilder input = new AddLogInputBuilder();
        input.setRecordFullText("Test");
        input.setNodeID("Test");
        input.setTimeStamp(System.currentTimeMillis());
        input.setTSDRDataCategory(DataCategory.EXTERNAL);
        input.setRecordKeys(new ArrayList<RecordKeys>());
        impl.addLog(input.build());
        Mockito.verify(tsdrService,Mockito.atLeastOnce()).storeTSDRLogRecord(Mockito.any(StoreTSDRLogRecordInput.class));
    }
}
