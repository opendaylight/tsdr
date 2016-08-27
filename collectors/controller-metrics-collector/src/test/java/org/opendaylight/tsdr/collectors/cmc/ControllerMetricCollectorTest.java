/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.collectors.cmc;

import java.util.Optional;

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
    private TsdrCollectorSpiService tsdrService = Mockito.mock(TsdrCollectorSpiService.class);

    @Before
    public void setup(){
        if(collector==null){
            collector = new ControllerMetricCollector(module, Optional.of(new SigarCollectorMock()));
        }
        Mockito.when(module.getTSDRCollectorSPIService()).thenReturn(tsdrService);
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
        Object object = collector.getControllerCPU();
        Assert.assertNotNull(object);
    }

    @Test
    public void testGetMachineCPUNull(){
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

    public static class SigarCollectorMock extends CpuDataCollector {
        public Optional<Double> getControllerCpu(){ return Optional.of(0.123d); }

        public Optional<Double> getMachineCpu(){
            return Optional.of(0.456d);
        }
    }
}
