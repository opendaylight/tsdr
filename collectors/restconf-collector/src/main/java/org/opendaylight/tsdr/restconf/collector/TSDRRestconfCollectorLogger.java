/*
 * Copyright (c) 2016 Saugo360.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.restconf.collector;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.opendaylight.tsdr.collector.spi.logger.BatchingLogCollector;
import org.opendaylight.tsdr.spi.scheduler.SchedulerService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;

/**
 * This class is responsible for interfacing with the TSDR collector SPI in order to persist the collected logs.
 * It maintains a cached queue of the logs, that is persisted every pre-set amount of time, and then emptied
 *
 * @author <a href="mailto:a.alhamali93@gmail.com">AbdulRahman AlHamali</a>
 *
 *         Created: Dec 16th, 2016
 *
 */
@Singleton
public class TSDRRestconfCollectorLogger extends BatchingLogCollector {

    @Inject
    public TSDRRestconfCollectorLogger(TsdrCollectorSpiService tsdrCollectorSpiService,
                                       SchedulerService schedulerService) {
        super(tsdrCollectorSpiService, schedulerService, "TSDRRestconfCollector");
    }

    @PostConstruct
    public void init() {
        TSDRRestconfCollectorFilter.setTSDRRestconfCollectorLogger(this);
    }

    /**
     * builds a log from the provided parameters, and inserts it into the cache queue.
     * @param method the http method
     * @param pathInfo the relative url of the request
     * @param remoteAddress the address from which the request generated
     * @param body the content of the request
     */
    public void insertLog(String method, String pathInfo, String remoteAddress, String body) {
        super.insertLog(pathInfo, System.currentTimeMillis(),
                "METHOD=" + method + ",REMOTE_ADDRESS=" + remoteAddress + ",BODY=" + body,
                DataCategory.RESTCONF);
    }
}
