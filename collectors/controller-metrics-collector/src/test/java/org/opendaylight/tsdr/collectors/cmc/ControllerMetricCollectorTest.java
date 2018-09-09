/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.collectors.cmc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.opendaylight.tsdr.collectors.cmc.ControllerMetricCollector.CONTROLLER_ID;
import static org.opendaylight.tsdr.collectors.cmc.ControllerMetricCollector.CPU_USAGE_NAME;
import static org.opendaylight.tsdr.collectors.cmc.ControllerMetricCollector.MACHINE_ID;
import static org.opendaylight.tsdr.collectors.cmc.ControllerMetricCollector.MEMORY_USAGE_NAME;

import com.google.common.util.concurrent.ListenableScheduledFuture;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opendaylight.tsdr.spi.scheduler.SchedulerService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRMetricRecordInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRMetricRecordOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

/**
 * Unit tests for ControllerMetricCollector.
 *
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class ControllerMetricCollectorTest {
    private static final long POLL_INTERVAL = 100;
    private final TsdrCollectorSpiService mockSpiService = mock(TsdrCollectorSpiService.class);
    private final SchedulerService mockSchedulerService = mock(SchedulerService.class);
    private final SigarCollectorMock mockCPUCollector = new SigarCollectorMock();
    private final Map<String, TSDRMetricRecord> storedRecords = new HashMap<>();

    @Before
    public void setup() {
        doAnswer(invocation -> {
            ((InsertTSDRMetricRecordInput) invocation.getArguments()[0]).getTSDRMetricRecord().forEach(
                rec -> storedRecords.put(key(rec.getMetricName(), rec.getNodeID()), rec));
            return RpcResultBuilder.success(new InsertTSDRMetricRecordOutputBuilder().build()).buildFuture();
        }).when(mockSpiService).insertTSDRMetricRecord(any());
    }

    @Test
    public void testCollectMetrics() {
        ListenableScheduledFuture<?> mockFuture = mock(ListenableScheduledFuture.class);
        doReturn(mockFuture).when(mockSchedulerService).scheduleTaskAtFixedRate(any(), anyLong(), anyLong());

        try (ControllerMetricCollector collector = new ControllerMetricCollector(mockSpiService,
                mockSchedulerService, Optional.of(mockCPUCollector), POLL_INTERVAL)) {
            collector.init();

            runScheduledTask();

            verifyMetric(storedRecords, CPU_USAGE_NAME, CONTROLLER_ID, mockCPUCollector.getControllerCpu());
            verifyMetric(storedRecords, CPU_USAGE_NAME, MACHINE_ID, mockCPUCollector.getMachineCpu());
            verifyMetric(storedRecords, MEMORY_USAGE_NAME, CONTROLLER_ID, Optional.empty());

            assertEquals(3, storedRecords.size());
        }

        verify(mockFuture).cancel(anyBoolean());
    }

    @Test
    public void testCollectMetricsSansCPUCollector() {
        try (ControllerMetricCollector collector = new ControllerMetricCollector(mockSpiService,
                mockSchedulerService, Optional.empty(), POLL_INTERVAL)) {
            collector.init();

            runScheduledTask();

            verifyMetric(storedRecords, MEMORY_USAGE_NAME, CONTROLLER_ID, Optional.empty());

            assertEquals(1, storedRecords.size());
        }
    }

    private void runScheduledTask() {
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(mockSchedulerService).scheduleTaskAtFixedRate(runnableCaptor.capture(), eq(POLL_INTERVAL),
                eq(POLL_INTERVAL));

        runnableCaptor.getValue().run();
    }

    private static void verifyMetric(Map<String, TSDRMetricRecord> records, String name,
            String nodeID, Optional<Double> expValue) {
        String key = key(name, nodeID);
        TSDRMetricRecord record = records.get(key);
        assertNotNull("Missing record for " + key, record);
        assertEquals(key + " DataCategory", DataCategory.EXTERNAL, record.getTSDRDataCategory());

        if (expValue.isPresent()) {
            assertEquals(key + " value", new BigDecimal(expValue.get()), record.getMetricValue());
        }
    }

    private static String key(String name, String id) {
        return name + "/" + id;
    }

    private static class SigarCollectorMock extends CpuDataCollector {
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
