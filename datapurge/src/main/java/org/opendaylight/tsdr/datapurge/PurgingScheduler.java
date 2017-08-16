/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.datapurge;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.tsdr.spi.scheduler.SchedulerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * PurgeScheduler schedules Purging task based on the parameters
 * in the purging configuration file.
 *
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 *
 * Created: Dec 31rst, 2015
 */
public class PurgingScheduler {
    private static final Logger log = LoggerFactory.getLogger(PurgingScheduler.class);

    private ScheduledFuture<?> future = null;
    private PurgeDataTask purgedatatask;
    private boolean isEnabled = false;
    private final int purgingInterval;
    private final String purgingTime;
    private final int retentionTime;
    private final RpcProviderRegistry rpcRegistry;

    public PurgingScheduler(RpcProviderRegistry rpcRegistry, boolean isEnabled, int purgingInterval,
            String purgingTime, int retentionTime) {
        this.rpcRegistry = rpcRegistry;
        this.isEnabled = isEnabled;
        this.purgingInterval = purgingInterval;
        this.purgingTime = purgingTime;
        this.retentionTime = retentionTime;
    }

    /**
     * Schedule the Purging Task according to the properites.
     */
    private void schedulePurgingTask(){
        if (!isEnabled) {
            return;
        }
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        try {
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH);
            int day = cal.get(Calendar.DAY_OF_MONTH);
            cal.setTime(sdf.parse(this.purgingTime));
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.MONTH, month);
            cal.set(Calendar.DAY_OF_MONTH, day);
        } catch (Exception e) {
            log.error("Exception while parsing purge time", e);
        }

        long first_time = cal.getTime().getTime() - System.currentTimeMillis();
        purgedatatask = new PurgeDataTask(rpcRegistry);
        purgedatatask.setRetentionTimeinHours(this.retentionTime);
        this.future = SchedulerService.getInstance().scheduleTaskAtFixedRate(
                purgedatatask, TimeUnit.MILLISECONDS.toSeconds(first_time),
                TimeUnit.MINUTES.toSeconds(purgingInterval));
        return;

    }

    /**
     * Reschedule the purging task when the properties are changed from the configuration file.
     */
    public void schedule(){
        if (this.future != null){
            future.cancel(true);
        }
        schedulePurgingTask();
    }
    /**
     * Cancel the scheduled purging task
     */
    public void cancelScheduledTask(){
        if (this.future != null){
            future.cancel(true);
            future = null;
        }
    }
    /**Return if the Purging task is currently running
     *
     * @return - true if the scheduled task is not null.
     *         - false if the scheduled task is null.
     */
    public boolean isRunning(){
        return this.future != null;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public int getPurgingInterval() {
        return purgingInterval;
    }

    public String getPurgingTime() {
        return purgingTime;
    }

    public int getRetentionTime() {
        return retentionTime;
    }
}
