/*
 * Copyright (c) 2015 Shoaib Rao All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.datapurge;

import java.util.concurrent.ScheduledFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.tsdr.spi.scheduler.Task;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.PurgeAllTSDRRecordInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRService;


/**
 * This class creates a task to purge data. It extends the TSDR Task, which is
 * schedulable by TSDR Scheduler.
 *
 * @author <a href="mailto:rao.shoaib@gmail.com">Shoaib Rao</a>
 *
 *         Created: Oct 19th, 2015
 *
 */
public class PurgeDataTask extends Task {
    private static final Logger log = LoggerFactory
            .getLogger(PurgeDataTask.class);
    private ScheduledFuture future = null;
    private TSDRService storageService = null;
    private RpcProviderRegistry rpcRegistry = null;
    private int retentionTimeinHours = 0;
    private static final  long Hour2MilliSecs= 60* 60 * 1000;

    public PurgeDataTask(RpcProviderRegistry _rpcRegistry) {
        super();
        this.rpcRegistry = _rpcRegistry;
    }

    @Override
    public void runTask() {
        Thread.currentThread().setName(
                "PurgeData Task-thread-" + Thread.currentThread().getId());
        purgeData();
    }
    /**
     * Send the purging request to storage service to purge the data.
     */
    public void purgeData() {
        log.debug("Entering PurgeData");
        PurgeAllTSDRRecordInputBuilder input = new PurgeAllTSDRRecordInputBuilder();
        if (storageService == null) {
            storageService = this.rpcRegistry.getRpcService(TSDRService.class);
        }
        input.setRetentionTime(System.currentTimeMillis()
                - this.retentionTimeinHours * Hour2MilliSecs);
        storageService.purgeAllTSDRRecord(input.build());
        log.debug("Exiting PurgeData");
    }

    public void setScheduledFuture(ScheduledFuture scheduledFuture) {
        future = scheduledFuture;
    }
    public int getRetentionTimeinHours() {
        return retentionTimeinHours;
    }

    public void setRetentionTimeinHours(int retentionTimeinHours) {
        this.retentionTimeinHours = retentionTimeinHours;
    }

}
