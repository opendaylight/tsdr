/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.spi.command;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.opendaylight.tsdr.spi.persistence.TSDRLogPersistenceService;
import org.opendaylight.tsdr.spi.persistence.TSDRMetricPersistenceService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class Activator extends DependencyActivatorBase {

    private BundleContext bundleContext = null;
    private static final Logger log = LoggerFactory.getLogger(Activator.class);

    @Override
    public void init(BundleContext context, DependencyManager manager) throws Exception {
        this.bundleContext = context;
        new ServiceLocator();
    }

    @Override
    public void destroy(BundleContext context, DependencyManager manager) throws Exception {
    }

    private class ServiceLocator extends Thread {
        public boolean metricServiceFound = false;
        public boolean logServiceFound = false;


        public ServiceLocator(){
            this.setDaemon(true);
            this.start();
        }

        public void run(){
            int count = 0;
            while(!metricServiceFound || !logServiceFound) {
                count++;
                log.info("Attempt #{} to find persistence services",count);
                if(!metricServiceFound) {
                    final ServiceReference<TSDRMetricPersistenceService> serviceReference = bundleContext.getServiceReference(TSDRMetricPersistenceService.class);
                    if (serviceReference != null) {
                        ListMetricsCommand.metricService = bundleContext.getService(serviceReference);
                        metricServiceFound = true;
                        log.info("TSDR List Metric Persistence Service Was Found.");
                    }else{
                        log.info("TSDR List Metric Persistence Service Was not Found, will attempt in 2 seconds");
                    }
                }
                if(!logServiceFound) {
                    final ServiceReference<TSDRLogPersistenceService> serviceReference = bundleContext.getServiceReference(TSDRLogPersistenceService.class);
                    if (serviceReference != null) {
                        ListMetricsCommand.logService = bundleContext.getService(serviceReference);
                        logServiceFound = true;
                        log.info("TSDR List Log Persistence Service Was Found.");
                    }else{
                        log.info("TSDR List Log Persistence Service Was not Found, will attempt in 2 seconds");
                    }
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    log.error("Interrupted",e);
                    break;
                }
            }
            if(metricServiceFound && logServiceFound){
                log.info("All TSDR List Persistence Services were found.");
            }
        }
    }
}