/*
 * Copyright (c) 2018 Kontron Canada Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.collector.spi.logger;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.PreDestroy;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.tsdr.collector.spi.RPCFutures;
import org.opendaylight.tsdr.spi.scheduler.SchedulerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRLogRecordInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class that batches generic messages and periodically transforms them <code>TSDRLogRecord</code>a and
 * flushes them to the persistence back-end.
 *
 * @author Matthieu Cauffiez
 * @author Thomas Pantelis
 *
 * @param T the incoming message type
 */
public abstract class AbstractBatchingLogCollector<T> implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractBatchingLogCollector.class);

    /*
     * The reference to the collector SPI service.
     */
    private final TsdrCollectorSpiService tsdrCollectorSpiService;

    private final ScheduledFuture<?> future;

    /*
     * The queue in which the data is cached before persisting.
     */
    @GuardedBy("queue")
    private final Deque<T> queue = new ArrayDeque<>();

    private final String collectorName;

    /*
     * The index of the current record, useful to distinguish records are received in the same milli-second
     * it is incremented for each record received, and then zeroed again after the data is persisted.
     */
    @GuardedBy("queue")
    private final int currentIndex = 0;

    protected AbstractBatchingLogCollector(TsdrCollectorSpiService tsdrCollectorSpiService,
            SchedulerService schedulerService, String collectorName, long storeFlushInterval) {
        this.tsdrCollectorSpiService = tsdrCollectorSpiService;
        this.collectorName = collectorName;
        this.future = schedulerService.scheduleTaskAtFixedRate(this::processQueue, storeFlushInterval,
                storeFlushInterval);
    }

    @Override
    @PreDestroy
    public void close() {
        this.future.cancel(false);
    }

    @Nonnull
    protected abstract List<TSDRLogRecord> transform(List<T> from);

    public void enqueue(T message) {
        synchronized (queue) {
            queue.offer(message);
        }
    }

    /**
     * Persists the cache queue.
     *
     * @param records the queue to persist
     */
    private void store(@Nonnull List<TSDRLogRecord> records) {
        if (records.isEmpty()) {
            return;
        }

        InsertTSDRLogRecordInputBuilder input = new InsertTSDRLogRecordInputBuilder().setTSDRLogRecord(records)
                .setCollectorCodeName(collectorName);

        RPCFutures.logResult(tsdrCollectorSpiService.insertTSDRLogRecord(input.build()), "insertTSDRLogRecord", LOG);
    }

    /**
     * Copies the cached queue to a temporary list, then clears the queue. If the temp list has data, it is
     * transformed then persisted. The reason behind using a temporary list is to free the original queue as quickly
     * as possible, so that if insertLog is waiting, it could resume its execution as soon as possible
     */
    private void processQueue() {
        final List<T> tempQueue;
        synchronized (queue) {
            if (!queue.isEmpty()) {
                tempQueue = queue.stream().collect(Collectors.toList());
                queue.clear();
            } else {
                tempQueue = Collections.emptyList();
            }
        }

        if (tempQueue.isEmpty()) {
            return;
        }

        store(transform(tempQueue));
    }
}
