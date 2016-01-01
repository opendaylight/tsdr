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
    private static PurgingScheduler instance = null;
    private static final Logger log = LoggerFactory
            .getLogger(PurgingScheduler.class);
    private static RpcProviderRegistry rpcRegistry;
    private ScheduledFuture future = null;
    private PurgeDataTask purgedatatask;
    private static final int DEFULT_INTERVAL = 24 * 60; /* Default one day */
    private static final String DEFAULT_PURGE_TIME = "23:59:59";
    private static final int DEFAULT_RETENTION_TIME_IN_HOURS = 7 * 24 ;//Default 7 Days
    private boolean isEnabled = false;
    private int purgingInterval = DEFULT_INTERVAL;
    private String purgingTime = DEFAULT_PURGE_TIME;
    private int retentionTime = DEFAULT_RETENTION_TIME_IN_HOURS;

    public static PurgingScheduler getInstance(){
        if(instance == null){
            instance = new PurgingScheduler();
        }
        return instance;
    }

    private PurgingScheduler(){
       super();
    }

    /**
     * Initialize, load properties from configuration, and schedule the purging task
     * This method is meant for the first time to schedule the purging task
     * when purging service is started.
     * @param _rpcRegistry
     */
    public void initAndScheduleTask(RpcProviderRegistry _rpcRegistry){
        rpcRegistry = _rpcRegistry;
        //load properties from configuration
        loadProperties();
        //schedule the purging task
        this.future = schedulePurgingTask();
    }
    private String getPropertyVal(String property, String default_val) {
        String property_val = TSDRDataPurgeConfig.getInstance().getProperty(
                property);
        return ((property_val == null) ? default_val : property_val);
    }

    /**
     * Schedule the purging task according the properties.
     * @return
     */
    private ScheduledFuture schedulePurgingTask(){
        if (!isEnabled) {
            return null;
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
        return(SchedulerService.getInstance().scheduleTaskAtFixedRate(
                purgedatatask, TimeUnit.MILLISECONDS.toSeconds(first_time),
                TimeUnit.MINUTES.toSeconds(purgingInterval)));

    }
    /**
     * Set the instance variables with properties being loaded from configuration.
     */
    private void loadProperties(){
        this.isEnabled = Boolean.parseBoolean(getPropertyVal("data_purge_enabled", "false"));
        this.purgingTime = getPropertyVal("data_purge_time", DEFAULT_PURGE_TIME);
        this.purgingInterval = Integer.valueOf(getPropertyVal(
                "data_purge_interval_in_minutes", Integer.toString(DEFULT_INTERVAL)));
        this.retentionTime = Integer.valueOf(getPropertyVal(
                "retention_time_in_hours", Integer.toString(DEFAULT_RETENTION_TIME_IN_HOURS)));
    }
    /**
     * Reschedule the purging task when the properties are changed from the configuration file.
     */
    public void reSchedule(){
        loadProperties();
        if (this.future != null){
            future.cancel(true);
        }
        this.future = schedulePurgingTask();
    }
    /**
     * Cancel the scheduled purging task
     */
    public void cancelScheduledTask(){
        if (this.future != null){
            future.cancel(true);
        }
    }
    /**Return if the Purging task is currently running
     *
     * @return
     */
    public boolean isRunning(){
        return(this.future != null);
    }

}
