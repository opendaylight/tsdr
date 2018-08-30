/*
 * Copyright © 2018 Kontron Canada Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.collector.spi.logger;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;
import javax.annotation.PreDestroy;
import javax.annotation.concurrent.GuardedBy;

import org.opendaylight.tsdr.collector.spi.RPCFutures;
import org.opendaylight.tsdr.spi.scheduler.SchedulerService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRLogRecordInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecordBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class is responsible for interfacing with the TSDR collector SPI in order to persist the collected logs.
 * It maintains a cached queue of the logs, that is persisted every pre-set amount of time, and then emptied
 *
 * @author <a href="mailto:a.alhamali93@gmail.com">AbdulRahman AlHamali</a>
 *
 * @author Matthieu Cauffiez
 */

public class BatchingLogCollector implements AutoCloseable {
    /**
     * the logger of the class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(BatchingLogCollector.class);
    /**
     * default interval after which the data is persisted, specified in milli-seconds.
     */
    private static final long DEFAULT_CHECK_INTERVAL_IN_MILLISECONDS = 5000;

    /**
     * a reference to the collector SPI service.
     */
    private final TsdrCollectorSpiService tsdrCollectorSpiService;

    private final ScheduledFuture<?> future;

    /**
     * The queue in which the data is cached before persisting.
     */
    @GuardedBy("queueMutex")
    private final ArrayDeque<TSDRLogRecord> queue = new ArrayDeque<>();

    /**
     * A mutex used for locking the queue to prevent race conditions between multiple threads.
     */
    private final Object queueMutex = new Object();

    private final String collectorName;

    /**
     * The index of the current record, useful to distinguish records are received in the same milli-second
     * it is incremented for each record received, and then zeroed again after the data is persisted.
     */
    @GuardedBy("queueMutex")
    private int currentIndex = 0;


    public BatchingLogCollector(TsdrCollectorSpiService tsdrCollectorSpiService, SchedulerService schedulerService,
                                String collectorName, long initialDelay, long retryInterval) {
        this.tsdrCollectorSpiService = tsdrCollectorSpiService;
        this.collectorName = collectorName;
        this.future = schedulerService.scheduleTaskAtFixedRate(this::processQueue, initialDelay, retryInterval);
        LOG.info("{} logger initialized", collectorName);
    }

    public BatchingLogCollector(TsdrCollectorSpiService tsdrCollectorSpiService, SchedulerService schedulerService,
                                String collectorName) {
        this(tsdrCollectorSpiService, schedulerService, collectorName,
                DEFAULT_CHECK_INTERVAL_IN_MILLISECONDS, DEFAULT_CHECK_INTERVAL_IN_MILLISECONDS);
    }

    /**
     * persists the cache queue.
     * @param records the queue to persist
     */
    private void store(List<TSDRLogRecord> records) {
        InsertTSDRLogRecordInputBuilder input = new InsertTSDRLogRecordInputBuilder();
        input.setTSDRLogRecord(records);
        input.setCollectorCodeName(collectorName);

        RPCFutures.logResult(tsdrCollectorSpiService.insertTSDRLogRecord(input.build()), "insertTSDRLogRecord", LOG);
    }

    /**
     * It copies the cached queue into another queue, then clears the queue and the currentIndex.
     * Then, it checks if that new queue has data, if it has data, it gets persisted.
     * Finally, it checks if the module has shut down. If it has, it cancels the timer task.
     * The reason behind using a temporary queue is to free the original queue as quickly as possible, so that if
     * insertLog is waiting, it could resume its execution as soon as possible
     */
    private void processQueue() {
        final List<TSDRLogRecord> tempQueue;
        synchronized (queueMutex) {
            if (!queue.isEmpty()) {
                tempQueue = queue.stream().collect(Collectors.toList());
                queue.clear();
                currentIndex = 0;
            } else {
                tempQueue = Collections.emptyList();
            }
        }
        if (!tempQueue.isEmpty()) {
            store(tempQueue);
        }
    }

    /**
     * builds a log from the provided parameters, and inserts it into the cache queue.
     * @param elementID The ID of the element.
     * @param timeStamp time when the log was produced.
     * @param data Content of the log.
     * @param dataCategory type of data.
     */
    public void insertLog(String elementID, long timeStamp, String data, DataCategory dataCategory) {

        TSDRLogRecordBuilder recordBuilder = new TSDRLogRecordBuilder();

        recordBuilder.setNodeID(elementID);
        recordBuilder.setTimeStamp(timeStamp);
        recordBuilder.setRecordFullText(data);
        recordBuilder.setTSDRDataCategory(dataCategory);

        synchronized (queueMutex) {
            recordBuilder.setIndex(currentIndex);
            currentIndex++;
            queue.add(recordBuilder.build());
        }
    }

    @Override
    @PreDestroy
    public void close() {
        this.future.cancel(false);
    }

}
