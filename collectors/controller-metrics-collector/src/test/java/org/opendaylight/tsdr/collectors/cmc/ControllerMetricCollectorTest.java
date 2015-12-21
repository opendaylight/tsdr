/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.collectors.cmc;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.config.yang.config.tsdr.controller.metrics.collector.TSDRCMCModule;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;

/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class ControllerMetricCollectorTest {
    private ControllerMetricCollector collector = null;
    private TSDRCMCModule module = Mockito.mock(TSDRCMCModule.class);
    private RpcProviderRegistry registry = Mockito.mock(RpcProviderRegistry.class);
    private TsdrCollectorSpiService tsdrService = Mockito.mock(TsdrCollectorSpiService.class);

    @Before
    public void setup(){
        if(collector==null){
            collector = new ControllerMetricCollector(module,registry);
        }
        collector.setSigar(new SigarMock());
        Mockito.when(module.getTSDRCollectorSPIService()).thenReturn(tsdrService);
        Mockito.when(module.isRunning()).thenReturn(true);
    }

    @Test
    public void testGetControllerCPU(){
        Object object = collector.getControllerCPU();
        Assert.assertNotNull(object);
    }

    @Test
    public void testGetMachineCPU(){
        Object object = collector.getMachineCPU();
        Assert.assertNotNull(object);
    }

    @Test
    public void testGetControllerCPUNull(){
        collector.setSigar(null);
        Object object = collector.getControllerCPU();
        Assert.assertNotNull(object);
    }

    @Test
    public void testGetMachineCPUNull(){
        collector.setSigar(null);
        Object object = collector.getMachineCPU();
        Assert.assertNotNull(object);
    }

    @Test
    public void testInsertMemorySample(){
        collector.insertMemorySample();
    }

    @Test
    public void testInsertControllerCPUSample(){
        collector.insertControllerCPUSample();
    }

    @Test
    public void testInsertMachineCPUSample(){
        collector.insertMachineCPUSample();
    }

    @Test
    public void testWaitForCollectionInterval() throws InterruptedException {
        //Wait for @ least one interval
        Thread.sleep(6000);
    }

    public static class SigarMock {
        public long getPid(){
            return 123L;
        }

        public SigarCpuMock getProcCpu(long l){
            return new SigarCpuMock();
        }

        public SigarCpuMock getCpuPerc(){
            return new SigarCpuMock();
        }
    }

    public static class SigarCpuMock {
        public double getPercent(){
            return 50.0;
        }

        public double getCombined(){
            return 0.6;
        }
    }
}
