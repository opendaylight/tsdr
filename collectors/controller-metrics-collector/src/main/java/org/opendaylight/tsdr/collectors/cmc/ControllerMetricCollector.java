/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.collectors.cmc;

import com.google.common.annotations.VisibleForTesting;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collects various metrics for the controller process.
 *
 * @author Sharon Aicler(saichler@gmail.com)
 */
@Singleton
public class ControllerMetricCollector implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(ControllerMetricCollector.class);

    private static final String COLLECTOR_CODE_NAME = ControllerMetricCollector.class.getSimpleName();
    private static final long POLL_INTERVAL = 5000;

    private final Optional<CpuDataCollector> cpuDataCollector;
    private final TsdrCollectorSpiService collectorSPIService;
    private final SchedulerService schedulerService;

    private ScheduledFuture<?> scheduledFuture;

    @Inject
    public ControllerMetricCollector(final TsdrCollectorSpiService collectorSPIService,
            final SchedulerService schedulerService) {
        this(collectorSPIService, schedulerService, CpuDataCollector.getCpuDataCollector());
    }

    @VisibleForTesting
    ControllerMetricCollector(final TsdrCollectorSpiService collectorSPIService,
            final SchedulerService schedulerService, final Optional<CpuDataCollector> cpuDataCollector) {
        this.collectorSPIService = collectorSPIService;
        this.schedulerService = schedulerService;
        this.cpuDataCollector = cpuDataCollector;
    }

    @PostConstruct
    public void init() {
        scheduledFuture = schedulerService.scheduleTaskAtFixedRate(this::poll, POLL_INTERVAL, POLL_INTERVAL);
        LOG.info("Controller Metrics Collector started");
    }

    @Override
    @PreDestroy
    @SuppressWarnings("checkstyle:IllegalCatch")
    public void close() {
        if (cpuDataCollector.isPresent()) {
            try {
                cpuDataCollector.get().close();
            } catch (Exception e) {
                LOG.warn("Error closing CpuDataCollector", e);
            }
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


    public Optional<Double> getControllerCpu() {
        LOG.debug("Getting controller CPU data");
        if (cpuDataCollector.isPresent()) {
            return cpuDataCollector.get().getControllerCpu();
        } else {
            // unable to get controller CPU data, user has already been warned
            LOG.info("Unable to get controller CPU data, data collector is not present");
            return Optional.empty();
        }
    }

    public Optional<Double> getMachineCpu() {
        LOG.debug("Getting machine CPU data");
        if (cpuDataCollector.isPresent()) {
            return cpuDataCollector.get().getMachineCpu();
        } else {
            // unable to get machine CPU data, user has already been warned
            LOG.info("Unable to get machine CPU data, data collector is not present");
            return Optional.empty();
        }
    }

    protected void insertMemorySample() {
        TSDRMetricRecordBuilder builder = new TSDRMetricRecordBuilder();
        builder.setMetricName("Heap:Memory:Usage");
        builder.setTSDRDataCategory(DataCategory.EXTERNAL);
        builder.setNodeID("Controller");
        builder.setRecordKeys(new ArrayList<>());
        builder.setTimeStamp(System.currentTimeMillis());
        builder.setMetricValue(new BigDecimal(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
        InsertTSDRMetricRecordInputBuilder input = new InsertTSDRMetricRecordInputBuilder();
        List<TSDRMetricRecord> list = new LinkedList<>();
        list.add(builder.build());
        input.setTSDRMetricRecord(list);
        input.setCollectorCodeName(COLLECTOR_CODE_NAME);

        RPCFutures.logResult(collectorSPIService.insertTSDRMetricRecord(input.build()), "insertTSDRMetricRecord", LOG);
    }

    protected void insertControllerCPUSample() {
        final Optional<Double> cpuValue = getControllerCpu();

        if (!cpuValue.isPresent()) {
            return;
        }

        TSDRMetricRecordBuilder builder = new TSDRMetricRecordBuilder();
        builder.setMetricName("CPU:Usage");
        builder.setTSDRDataCategory(DataCategory.EXTERNAL);
        builder.setNodeID("Controller");
        builder.setRecordKeys(new ArrayList<>());
        builder.setTimeStamp(System.currentTimeMillis());
        builder.setMetricValue(new BigDecimal(cpuValue.get()));
        InsertTSDRMetricRecordInputBuilder input = new InsertTSDRMetricRecordInputBuilder();
        List<TSDRMetricRecord> list = new LinkedList<>();
        list.add(builder.build());
        input.setTSDRMetricRecord(list);
        input.setCollectorCodeName(COLLECTOR_CODE_NAME);

        RPCFutures.logResult(collectorSPIService.insertTSDRMetricRecord(input.build()), "insertTSDRMetricRecord", LOG);
    }

    protected void insertMachineCPUSample() {
        final Optional<Double> cpuValue = getMachineCpu();

        if (!cpuValue.isPresent()) {
            return;
        }

        TSDRMetricRecordBuilder builder = new TSDRMetricRecordBuilder();
        builder.setMetricName("CPU:Usage");
        builder.setTSDRDataCategory(DataCategory.EXTERNAL);
        builder.setNodeID("Machine");
        builder.setRecordKeys(new ArrayList<>());
        builder.setTimeStamp(System.currentTimeMillis());
        builder.setMetricValue(new BigDecimal(cpuValue.get()));
        InsertTSDRMetricRecordInputBuilder input = new InsertTSDRMetricRecordInputBuilder();
        List<TSDRMetricRecord> list = new LinkedList<>();
        list.add(builder.build());
        input.setTSDRMetricRecord(list);
        input.setCollectorCodeName(COLLECTOR_CODE_NAME);

        RPCFutures.logResult(collectorSPIService.insertTSDRMetricRecord(input.build()), "insertTSDRMetricRecord", LOG);
    }
}
