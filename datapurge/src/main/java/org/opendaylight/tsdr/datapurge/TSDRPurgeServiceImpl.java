/*
 * Copyright (c) 2015 Shoaib Rao All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.datapurge;

import java.text.SimpleDateFormat;
import java.util.Properties;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
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
    private int interval = 24*60; /* Default one day */
    private String purge_def_time = "23:59:59";
    private static String tsdrConfigFile = "tsdr.cfg";

    private static final Logger log = LoggerFactory
        .getLogger(TSDRPurgeServiceImpl.class);


    public TSDRPurgeServiceImpl(DataBroker _dataBroker,
            RpcProviderRegistry _rpcRegistry) {
        String date_string;
        // Now
        Date now = Calendar.getInstance().getTime();
        // Next
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf =  new SimpleDateFormat("HH:mm:ss");
        Properties properties = new Properties();
        InputStream inputStream = null;

        try {
            String  fileFullPath = System.getProperty("karaf.etc") + "/" + tsdrConfigFile;
            File f = new File(fileFullPath);
            if(f.exists()){
                log.info("Loading properties from " + fileFullPath);
                inputStream = new FileInputStream(f);
                properties.load(inputStream);
            } else {
                log.error("Property file " + fileFullPath + " missing");
            }
        } catch(Exception e){
                log.error("Exception while loading the datapurge.properties stream", e);
        }
        try {
            if(inputStream == null || !properties.propertyNames().hasMoreElements()){
                log.error("Properties stream is null or properties failed to load, check the file=" + tsdrConfigFile +" exists in classpath");
                log.warn("Initializing datapurge with default values");
                date_string = this.purge_def_time;
            } else {
                String enabled = getPropertyVal(properties, "data_purge_enabled", "false");
                Boolean is_enabled = Boolean.parseBoolean(enabled);
                date_string = getPropertyVal(properties, "data_purge_time", this.purge_def_time);
                interval = Integer.valueOf(getPropertyVal(properties, "data_purge_interval_in_minutes", Integer.toString(interval)));
                if (!is_enabled) {
                    return;
                }
            }
            try {
                cal.setTime(sdf.parse("date_string"));
            } catch (Exception e) {
                log.error("Exception while parsing purge time", e);
            }
        } finally{
            if(inputStream != null){
                try{
                    inputStream.close();
                }catch(Exception e){
                    log.error("Exception while closing the stream", e);
                }
            }
        }

        long first_time = cal.getTime().getTime() - now.getTime();

        this.dataBroker = _dataBroker;
        this.rpcRegistry = _rpcRegistry;
        if(tsdrService == null){
            tsdrService = this.rpcRegistry.getRpcService(TSDRService.class);
        }

        purgedatatask = new PurgeDataTask(this.rpcRegistry);
        this.future = SchedulerService.getInstance().scheduleTaskAtFixedRate(purgedatatask, TimeUnit.MILLISECONDS.toSeconds(first_time),
            TimeUnit.MINUTES.toSeconds(interval));
            log.debug("Starting TSDRPurgeServiceImpl");
        }
    private String getPropertyVal(Properties properties, String property, String default_val) {
        String property_val = properties.getProperty(property);
        return ((property_val == null) ? default_val:property_val);
    }
    public void shutdown() {
        log.debug("shutting Down TSDRPurgeServiceImpl");
        if (this.future != null)
            future.cancel(true);
    }
    public boolean isRunning() {
        return(this.future != null);
    }
}
