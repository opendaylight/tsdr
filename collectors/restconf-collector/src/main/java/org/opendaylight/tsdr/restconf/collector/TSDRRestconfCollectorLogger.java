/*
 * Copyright (c) 2016 Saugo360.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.restconf.collector;

import com.google.common.annotations.VisibleForTesting;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Supplier;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.tsdr.collector.spi.RPCFutures;
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
 *         Created: Dec 16th, 2016
 *
 */
public class TSDRRestconfCollectorLogger extends TimerTask implements AutoCloseable {

    /**
     * a reference to the collector SPI service.
     */
    private TsdrCollectorSpiService tsdrCollectorSpiService;

    /**
     * The instance of this class (for singleton pattern).
     */
    private static TSDRRestconfCollectorLogger INSTANCE = null;

    /**
     * The interval after which the data is persisted, specified in milli-seconds.
     */
    private static final long PERSIST_CHECK_INTERVAL_IN_MILLISECONDS = 5000;

    /**
     * The queue in which the data is cached before persisting.
     */
    @GuardedBy("queueMutex")
    private final LinkedList<TSDRLogRecord> queue = new LinkedList<>();

    /**
     * A mutex used for locking the queue to prevent race conditions between multiple threads.
     */
    private final Object queueMutex = new Object();

    /**
     * The index of the current record, useful to distinguish records are received in the same milli-second
     * it is incremented for each record received, and then zeroed again after the data is persisted.
     */
    @GuardedBy("queueMutex")
    private int currentIndex = 0;

    /**
     * the timer instance.
     */
    private Timer timer;

    private final Supplier<Timer> timerSupplier;

    /**
     * the logger of the class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(TSDRRestconfCollectorLogger.class);

    /**
     * the constructor is private because we are following the singleton pattern.
     * it is called once an instance is created.
     */
    private TSDRRestconfCollectorLogger(Supplier<Timer> timerSupplier) {
        this.timerSupplier = timerSupplier;
    }

    /**
     * retrieves the instance of the class, and creates a new one if no instance exists.
     * @return the instance of the singleton
     */
    public static TSDRRestconfCollectorLogger getInstance() {
        return getInstance(Timer::new);
    }

    /**
     * retrieves the instance of the class, and creates a new one if no instance exists.
     * Only call this version of the function when testing. It is useful if you want to mock the timer
     * @param timerSupplier the supplier of timer instances
     * @return the instance of the singleton
     */
    @VisibleForTesting
    public static synchronized TSDRRestconfCollectorLogger getInstance(Supplier<Timer> timerSupplier) {
        if (INSTANCE == null) {
            INSTANCE = new TSDRRestconfCollectorLogger(timerSupplier);
        }
        return INSTANCE;
    }

    /**
     * only call this method in testing to do mocking.
     * @param instance the instance of the class
     */
    @VisibleForTesting
    static void setInstance(TSDRRestconfCollectorLogger instance) {
        INSTANCE = instance;
    }

    public void init() {
        this.timer = timerSupplier.get();

        this.timer.schedule(this, PERSIST_CHECK_INTERVAL_IN_MILLISECONDS, PERSIST_CHECK_INTERVAL_IN_MILLISECONDS);

        LOG.info("Restconf collector logger initialized");
    }

    @Override
    public void close() {
        run();

        if (this.timer != null) {
            this.cancel();
            this.timer.cancel();
            this.timer.purge();
        }

        LOG.info("Restconf collector logger closed");
    }

    /**
     * sets the collector SPI service.
     * @param tsdrCollectorSpiService the collector SPI service instance
     */
    public void setTsdrCollectorSpiService(TsdrCollectorSpiService tsdrCollectorSpiService) {
        this.tsdrCollectorSpiService = tsdrCollectorSpiService;
    }

    /**
     * returns the collector SPI service.
     * @return the collector SPI service instance
     */
    public TsdrCollectorSpiService getTsdrCollectorSpiService() {
        return this.tsdrCollectorSpiService;
    }

    /**
     * builds a log from the provided parameters, and inserts it into the cache queue.
     * @param method the http method
     * @param pathInfo the relative url of the request
     * @param remoteAddress the address from which the request generated
     * @param body the content of the request
     */
    public void insertLog(String method, String pathInfo, String remoteAddress, String body) {

        TSDRLogRecordBuilder recordBuilder = new TSDRLogRecordBuilder();

        recordBuilder.setNodeID(pathInfo);
        recordBuilder.setTimeStamp(System.currentTimeMillis());
        recordBuilder.setRecordFullText("METHOD=" + method + ",REMOTE_ADDRESS=" + remoteAddress + ",BODY=" + body);
        recordBuilder.setTSDRDataCategory(DataCategory.RESTCONF);

        synchronized (queueMutex) {
            recordBuilder.setIndex(currentIndex);
            currentIndex++;
            queue.add(recordBuilder.build());
        }
    }

    /**
     * persists the cache queue.
     * @param records the queue to persist
     */
    private void store(List<TSDRLogRecord> records) {
        InsertTSDRLogRecordInputBuilder input = new InsertTSDRLogRecordInputBuilder();
        input.setTSDRLogRecord(records);
        input.setCollectorCodeName("TSDRRestconfCollector");

        RPCFutures.logResult(tsdrCollectorSpiService.insertTSDRLogRecord(input.build()), "insertTSDRLogRecord", LOG);
    }

    /**
     * called automatically by the timer each time a period of PERSIST_CHECK_INTERVAL_IN_MILLISECONDS has passed.
     * It copies the cached queue into another queue, then clears the queue and the currentIndex.
     * Then, it checks if that new queue has data, if it has data, it gets persisted.
     * Finally, it checks if the module has shut down. If it has, it cancels the timer task.
     * The reason behind using a temporary queue is to free the original queue as quickly as possible, so that if
     * insertLog is waiting, it could resume its execution as soon as possible
     */
    @Override
    public void run() {
        LinkedList<TSDRLogRecord> tempQueue = new LinkedList<>();
        synchronized (queueMutex) {
            tempQueue.addAll(queue);
            queue.clear();
            currentIndex = 0;
        }
        if (tempQueue.size() != 0) {
            store(tempQueue);
        }
    }
}
