/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.collectors.cmc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRMetricRecordInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class ControllerMetricCollector extends Thread implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(ControllerMetricCollector.class);

    private static final String COLLECTOR_CODE_NAME = ControllerMetricCollector.class.getSimpleName();

    private final Optional<CpuDataCollector> cpuDataCollector;
    private final TsdrCollectorSpiService collectorSPIService;

    public ControllerMetricCollector(final TsdrCollectorSpiService collectorSPIService) {
        this(collectorSPIService, CpuDataCollector.getCpuDataCollector());
    }

    public ControllerMetricCollector(final TsdrCollectorSpiService collectorSPIService,
            final Optional<CpuDataCollector> cpuDataCollector) {
        this.collectorSPIService = collectorSPIService;
        this.cpuDataCollector = cpuDataCollector;

        this.setDaemon(true);
    }

    public void init() {
        this.start();
        logger.info("Controller Metrics Collector started");
    }

    @Override
    public void close() {
        interrupt();
        logger.info("Controller Metrics Collector closed");
    }

    @Override
    public void run() {
        try {
            while (!interrupted()) {
                logger.debug("inserting new set of TSDR records");
                insertMemorySample();
                insertControllerCPUSample();
                insertMachineCPUSample();

                Thread.sleep(5000);
            }
        } catch (final InterruptedException err) {
            logger.info("ControllerMetricCollector thread has been interrupted and is now exiting");
            Thread.currentThread().interrupt();
        }
    }


    public Optional<Double> getControllerCPU() {
        logger.debug("Getting controller CPU data");
        if (cpuDataCollector.isPresent()) {
            return cpuDataCollector.get().getControllerCpu();
        } else {
            // unable to get controller CPU data, user has already been warned
            logger.info("Unable to get controller CPU data, data collector is not present");
            return Optional.empty();
        }
    }

    public Optional<Double> getMachineCPU() {
        logger.debug("Getting machine CPU data");
        if (cpuDataCollector.isPresent()) {
            return cpuDataCollector.get().getMachineCpu();
        } else {
            // unable to get machine CPU data, user has already been warned
            logger.info("Unable to get machine CPU data, data collector is not present");
            return Optional.empty();
        }
    }

    protected void insertMemorySample() {
        TSDRMetricRecordBuilder b = new TSDRMetricRecordBuilder();
        b.setMetricName("Heap:Memory:Usage");
        b.setTSDRDataCategory(DataCategory.EXTERNAL);
        b.setNodeID("Controller");
        b.setRecordKeys(new ArrayList<>());
        b.setTimeStamp(System.currentTimeMillis());
        b.setMetricValue(new BigDecimal(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
        InsertTSDRMetricRecordInputBuilder input = new InsertTSDRMetricRecordInputBuilder();
        List<TSDRMetricRecord> list = new LinkedList<>();
        list.add(b.build());
        input.setTSDRMetricRecord(list);
        input.setCollectorCodeName(COLLECTOR_CODE_NAME);
        collectorSPIService.insertTSDRMetricRecord(input.build());
    }

    protected void insertControllerCPUSample() {
        final Optional<Double> cpuValue = getControllerCPU();

        if (!cpuValue.isPresent()) {
            return;
        }

        TSDRMetricRecordBuilder b = new TSDRMetricRecordBuilder();
        b.setMetricName("CPU:Usage");
        b.setTSDRDataCategory(DataCategory.EXTERNAL);
        b.setNodeID("Controller");
        b.setRecordKeys(new ArrayList<>());
        b.setTimeStamp(System.currentTimeMillis());
        b.setMetricValue(new BigDecimal(cpuValue.get()));
        InsertTSDRMetricRecordInputBuilder input = new InsertTSDRMetricRecordInputBuilder();
        List<TSDRMetricRecord> list = new LinkedList<>();
        list.add(b.build());
        input.setTSDRMetricRecord(list);
        input.setCollectorCodeName(COLLECTOR_CODE_NAME);
        collectorSPIService.insertTSDRMetricRecord(input.build());
    }

    protected void insertMachineCPUSample() {
        final Optional<Double> cpuValue = getMachineCPU();

        if (!cpuValue.isPresent()) {
            return;
        }

        TSDRMetricRecordBuilder b = new TSDRMetricRecordBuilder();
        b.setMetricName("CPU:Usage");
        b.setTSDRDataCategory(DataCategory.EXTERNAL);
        b.setNodeID("Machine");
        b.setRecordKeys(new ArrayList<>());
        b.setTimeStamp(System.currentTimeMillis());
        b.setMetricValue(new BigDecimal(cpuValue.get()));
        InsertTSDRMetricRecordInputBuilder input = new InsertTSDRMetricRecordInputBuilder();
        List<TSDRMetricRecord> list = new LinkedList<>();
        list.add(b.build());
        input.setTSDRMetricRecord(list);
        input.setCollectorCodeName(COLLECTOR_CODE_NAME);
        collectorSPIService.insertTSDRMetricRecord(input.build());
    }
}
