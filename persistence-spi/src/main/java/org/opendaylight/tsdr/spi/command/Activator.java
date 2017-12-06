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
 * Bundle Activator.
 *
 * @author Sharon Aicler(saichler@gmail.com)
 */
public class Activator extends DependencyActivatorBase {
    private static final Logger LOG = LoggerFactory.getLogger(Activator.class);

    @Override
    public void init(BundleContext context, DependencyManager manager) throws Exception {
        new ServiceLocator(context).start();
    }

    @Override
    public void destroy(BundleContext context, DependencyManager manager) throws Exception {
    }

    private static class ServiceLocator extends Thread {
        private final BundleContext bundleContext;
        public boolean metricServiceFound;
        public boolean logServiceFound;

        ServiceLocator(BundleContext bundleContext) {
            this.bundleContext = bundleContext;
            this.setDaemon(true);
        }

        @Override
        public void run() {
            int count = 0;
            while (!metricServiceFound || !logServiceFound) {
                count++;
                LOG.info("Attempt #{} to find persistence services", count);
                if (!metricServiceFound) {
                    final ServiceReference<TSDRMetricPersistenceService> serviceReference = bundleContext
                            .getServiceReference(TSDRMetricPersistenceService.class);
                    if (serviceReference != null) {
                        ListMetricsCommand.setMetricService(bundleContext.getService(serviceReference));
                        metricServiceFound = true;
                        LOG.info("TSDR List Metric Persistence Service Was Found.");
                    } else {
                        LOG.info("TSDR List Metric Persistence Service Was not Found, will attempt in 2 seconds");
                    }
                }
                if (!logServiceFound) {
                    final ServiceReference<TSDRLogPersistenceService> serviceReference = bundleContext
                            .getServiceReference(TSDRLogPersistenceService.class);
                    if (serviceReference != null) {
                        ListMetricsCommand.setLogService(bundleContext.getService(serviceReference));
                        logServiceFound = true;
                        LOG.info("TSDR List Log Persistence Service Was Found.");
                    } else {
                        LOG.info("TSDR List Log Persistence Service Was not Found, will attempt in 2 seconds");
                    }
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    LOG.error("Interrupted", e);
                    break;
                }
            }
            if (metricServiceFound && logServiceFound) {
                LOG.info("All TSDR List Persistence Services were found.");
            }
        }
    }
}
