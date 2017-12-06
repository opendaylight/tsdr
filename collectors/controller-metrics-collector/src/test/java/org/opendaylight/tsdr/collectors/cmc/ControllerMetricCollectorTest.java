/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.collectors.cmc;

import static org.mockito.Matchers.any;

import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

/**
 * Unit tests for ControllerMetricCollector.
 *
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class ControllerMetricCollectorTest {
    private final TsdrCollectorSpiService tsdrService = Mockito.mock(TsdrCollectorSpiService.class);
    private final ControllerMetricCollector collector = new ControllerMetricCollector(tsdrService,
            Optional.of(new SigarCollectorMock()));

    @Before
    public void setup() {
        Mockito.when(tsdrService.insertTSDRLogRecord(any()))
                .thenReturn(RpcResultBuilder.<Void>success().buildFuture());
        Mockito.when(tsdrService.insertTSDRMetricRecord(any()))
                .thenReturn(RpcResultBuilder.<Void>success().buildFuture());
    }

    @Test
    public void testGetControllerCPU() {
        Object object = collector.getControllerCpu();
        Assert.assertNotNull(object);
    }

    @Test
    public void testGetMachineCPU() {
        Object object = collector.getMachineCpu();
        Assert.assertNotNull(object);
    }

    @Test
    public void testGetControllerCPUNull() {
        Object object = collector.getControllerCpu();
        Assert.assertNotNull(object);
    }

    @Test
    public void testGetMachineCPUNull() {
        Object object = collector.getMachineCpu();
        Assert.assertNotNull(object);
    }

    @Test
    public void testInsertMemorySample() {
        collector.insertMemorySample();
    }

    @Test
    public void testInsertControllerCPUSample() {
        collector.insertControllerCPUSample();
    }

    @Test
    public void testInsertMachineCPUSample() {
        collector.insertMachineCPUSample();
    }

    @Test
    public void testWaitForCollectionInterval() throws InterruptedException {
        // Wait for @ least one interval
        Thread.sleep(6000);
    }

    public static class SigarCollectorMock extends CpuDataCollector {
        @Override
        public Optional<Double> getControllerCpu() {
            return Optional.of(0.123d);
        }

        @Override
        public Optional<Double> getMachineCpu() {
            return Optional.of(0.456d);
        }
    }
}
