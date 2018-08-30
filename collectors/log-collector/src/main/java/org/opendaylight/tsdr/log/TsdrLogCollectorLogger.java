/*
 * Copyright © 2018 Kontron Canada Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.log;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.opendaylight.tsdr.collector.spi.logger.BatchingLogCollector;
import org.opendaylight.tsdr.spi.scheduler.SchedulerService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;

/**
 * This class is responsible for interfacing with the TSDR collector SPI in order to persist the odl collected logs.
 * It maintains a cached queue of the logs, that is persisted every pre-set amount of time, and then emptied.
 *
 */
@Singleton
public class TsdrLogCollectorLogger extends BatchingLogCollector {

    @Inject
    public TsdrLogCollectorLogger(TsdrCollectorSpiService tsdrCollectorSpiService, SchedulerService schedulerService) {
        super(tsdrCollectorSpiService, schedulerService, "TSDRLogCollector");
    }

    /**
     * builds a log from the provided parameters, and inserts it into the cache queue.
     * @param loggerName the emit logger Name
     * @param level the log level message
     * @param message the log message
     * @param timeStamp time when the log was produced.
     */
    public void insertLog(String loggerName, long timeStamp, String level, String message) {
        super.insertLog(loggerName, timeStamp, "LEVEL=" + level + ",MESSAGE=" + message, DataCategory.ODLLOG);
    }

}
