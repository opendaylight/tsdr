/*
 * Copyright (c) 2015 Shoaib Rao All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.datapurge;

import java.util.concurrent.ScheduledFuture;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRService;
import org.opendaylight.tsdr.spi.scheduler.SchedulerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TSDR Purge service implementation class.
 *
 * <p>
 * Schedules a recurring purge task.
 * </p>
 *
 * @author <a href="mailto:rao.shoaib@gmail.com">Shoaib Rao</a>
 *
 *    Created: Oct 19, 2015
 */
public class TSDRPurgeServiceImpl {
    private TSDRService tsdrService = null;
    private RpcProviderRegistry rpcRegistry = null;
    private DataBroker dataBroker = null;
    private ScheduledFuture future = null;
    private PurgeDataTask purgedatatask;
    private Long interval = 0L;

    private static final Logger log = LoggerFactory
        .getLogger(TSDRPurgeServiceImpl.class);


    public TSDRPurgeServiceImpl(DataBroker _dataBroker,
            RpcProviderRegistry _rpcRegistry) {

        // Now
        Date now = Calendar.getInstance().getTime();
        // Next
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.HOUR, 23);
        cal.set(Calendar.MINUTE, 59);
        long first_time = cal.getTime().getTime() - now.getTime();

        this.dataBroker = _dataBroker;
        this.rpcRegistry = _rpcRegistry;
        if(tsdrService == null){
            tsdrService = this.rpcRegistry.getRpcService(TSDRService.class);
        }

        purgedatatask = new PurgeDataTask(this.rpcRegistry);
        this.future = SchedulerService.getInstance().scheduleTaskAtFixedRate(purgedatatask, TimeUnit.MILLISECONDS.toSeconds(first_time),
            TimeUnit.DAYS.toSeconds(1));
        log.debug("Starting TSDRPurgeServiceImpl");
    }


    public void shutdown() {
        log.debug("shutting Down TSDRPurgeServiceImpl");
        if (this.future != null)
            future.cancel(true);
    }
}
