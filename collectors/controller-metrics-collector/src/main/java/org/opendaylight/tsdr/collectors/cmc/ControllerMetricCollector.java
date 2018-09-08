/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.collectors.cmc;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.OptionalDouble;
import java.util.concurrent.ScheduledFuture;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.tsdr.collector.spi.RPCFutures;
import org.opendaylight.tsdr.spi.scheduler.SchedulerService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRMetricRecordInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collects various metrics for the controller process.
 *
 * @author Sharon Aicler(saichler@gmail.com)
 * @author Thomas Pantelis
 */
@Singleton
public class ControllerMetricCollector implements AutoCloseable {

    static final String MACHINE_ID = "Machine";
    static final String CONTROLLER_ID = "Controller";
    static final String CPU_USAGE_NAME = "CPU:Usage";
    static final String MEMORY_USAGE_NAME = "Heap:Memory:Usage";

    private static final Logger LOG = LoggerFactory.getLogger(ControllerMetricCollector.class);

    private static final String COLLECTOR_CODE_NAME = ControllerMetricCollector.class.getSimpleName();
    private static final long DEFAULT_POLL_INTERVAL = 5000;

    private final CpuDataCollector cpuDataCollector;
    private final TsdrCollectorSpiService collectorSPIService;
    private final SchedulerService schedulerService;
    private final long pollInterval;

    private ScheduledFuture<?> scheduledFuture;

    @Inject
    public ControllerMetricCollector(final TsdrCollectorSpiService collectorSPIService,
            final SchedulerService schedulerService) {
        this(collectorSPIService, schedulerService, CpuDataCollector.getCpuDataCollector(), DEFAULT_POLL_INTERVAL);
    }

    @VisibleForTesting
    ControllerMetricCollector(final TsdrCollectorSpiService collectorSPIService,
            final SchedulerService schedulerService, final CpuDataCollector cpuDataCollector, final long pollInterval) {
        this.collectorSPIService = requireNonNull(collectorSPIService);
        this.schedulerService = requireNonNull(schedulerService);
        this.cpuDataCollector = requireNonNull(cpuDataCollector);
        this.pollInterval = pollInterval;
    }

    @PostConstruct
    public void init() {
        scheduledFuture = schedulerService.scheduleTaskAtFixedRate(this::poll, pollInterval, pollInterval);
        LOG.info("Controller Metrics Collector started");
    }

    @Override
    @PreDestroy
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void close() {
        try {
            cpuDataCollector.close();
        } catch (Exception e) {
            LOG.warn("Error closing CpuDataCollector", e);
        }

        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }

        LOG.info("Controller Metrics Collector closed");
    }

    private void poll() {
        LOG.debug("Inserting new set of TSDR records");

        insertMemorySample();
        insertControllerCPUSample();
        insertMachineCPUSample();
    }

    private void insertMemorySample() {
        storeRecord(MEMORY_USAGE_NAME, CONTROLLER_ID, OptionalDouble.of(
                Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
    }

    private void insertControllerCPUSample() {
        storeRecord(CPU_USAGE_NAME, CONTROLLER_ID, cpuDataCollector.getControllerCpu());
    }

    private void insertMachineCPUSample() {
        storeRecord(CPU_USAGE_NAME, MACHINE_ID, cpuDataCollector.getMachineCpu());
    }

    private void storeRecord(String metricName, String nodeID, OptionalDouble value) {
        if (!value.isPresent()) {
            LOG.debug("{} / {} data not present", metricName, nodeID);
            return;
        }

        LOG.debug("Storing {} / {} record with value {}", metricName, nodeID, value.getAsDouble());

        TSDRMetricRecordBuilder builder = new TSDRMetricRecordBuilder().setMetricName(metricName)
            .setTSDRDataCategory(DataCategory.EXTERNAL).setNodeID(nodeID).setRecordKeys(Collections.emptyList())
            .setTimeStamp(System.currentTimeMillis()).setMetricValue(new BigDecimal(value.getAsDouble()));

        InsertTSDRMetricRecordInputBuilder input = new InsertTSDRMetricRecordInputBuilder()
            .setTSDRMetricRecord(Collections.singletonList(builder.build())).setCollectorCodeName(COLLECTOR_CODE_NAME);

        RPCFutures.logResult(collectorSPIService.insertTSDRMetricRecord(input.build()), "insertTSDRMetricRecord", LOG);
    }
}
