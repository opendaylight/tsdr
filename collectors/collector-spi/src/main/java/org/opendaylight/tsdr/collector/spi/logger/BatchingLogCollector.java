/*
 * Copyright © 2018 Kontron Canada Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.collector.spi.logger;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.opendaylight.tsdr.spi.scheduler.SchedulerService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecordBuilder;


/**
 * This class is responsible for interfacing with the TSDR collector SPI in order to persist the collected logs.
 * It maintains a cached queue of the logs, that is persisted every pre-set amount of time, and then emptied
 *
 * @author <a href="mailto:a.alhamali93@gmail.com">AbdulRahman AlHamali</a>
 *
 * @author Matthieu Cauffiez
 */

public class BatchingLogCollector extends AbstractBatchingLogCollector<TSDRLogRecord> {
    /*
     * The default interval after which the data is persisted, specified in milli-seconds.
     */
    private static final long DEFAULT_FLUSH_INTERVAL_IN_MILLISECONDS = 5000;

    /**
     * The index of the current record, useful to distinguish records that are received in the same millisecond.
     * It is incremented for each record received, and then zeroed again after the data is persisted.
     */
    private final AtomicInteger currentIndex = new AtomicInteger();


    public BatchingLogCollector(TsdrCollectorSpiService tsdrCollectorSpiService, SchedulerService schedulerService,
                                String collectorName, long storeFlushInterval) {
        super(tsdrCollectorSpiService, schedulerService, collectorName, storeFlushInterval);
    }

    public BatchingLogCollector(TsdrCollectorSpiService tsdrCollectorSpiService, SchedulerService schedulerService,
                                String collectorName) {
        this(tsdrCollectorSpiService, schedulerService, collectorName, DEFAULT_FLUSH_INTERVAL_IN_MILLISECONDS);
    }

    /**
     * Builds a log from the provided parameters, and inserts it into the cache queue.
     *
     * @param elementID The ID of the element.
     * @param timeStamp time when the log was produced.
     * @param data Content of the log.
     * @param dataCategory type of data.
     */
    public void insertLog(String elementID, long timeStamp, String data, DataCategory dataCategory) {
        enqueue(new TSDRLogRecordBuilder().setNodeID(elementID).setTimeStamp(timeStamp).setRecordFullText(data)
                .setTSDRDataCategory(dataCategory).setIndex(currentIndex.getAndIncrement()).build());
    }

    @Override
    protected List<TSDRLogRecord> transform(List<TSDRLogRecord> from) {
        currentIndex.set(0);
        return from;
    }
}
