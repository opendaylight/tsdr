/*
 * Copyright (c) 2016 Saugo360.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.tsdr.restconf.collector;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Dictionary;
import java.util.Hashtable;
import org.opendaylight.tsdr.restconf.collector.TSDRRestconfCollectorConfig;
import org.opendaylight.tsdr.restconf.collector.TSDRRestconfCollectorFilter;
import org.opendaylight.tsdr.restconf.collector.TSDRRestconfCollectorLogger;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for setting up the TSDRRestconfCollector.
 * It provides TSDRRestconfCollectorLogger with its TsdrCollectorSpiService dependency
 * it registers the TSDRRestconfCollectorFilter in the aaa filterchain
 * it registers the TSDRRestconfCollectorConfig class to listen for changes in configuration
 * The class also cleans up after the module
 * it shuts down the logger, and unregisters the filter from the aaa filterchain
 *
 * @author <a href="mailto:a.alhamali93@gmail.com">AbdulRahman AlHamali</a>
 *
 *         Created: Dec 16th, 2016
 *
 */
public class TSDRRestconfCollectorModule extends
    org.opendaylight.controller.config.yang.config.tsdr.restconf.collector.AbstractTSDRRestconfCollectorModule
    implements AutoCloseable {

    /**
     * The logger of the class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


    /**
     * The bundle context used for registering the filter and the configuration class.
     */
    private BundleContext bundleContext;

    /**
     * A reference to the configuration of the filter chain, where we add/remove our filter.
     */
    Configuration filterChainConfiguration;

    /**
     * The PID used to access the filter chain configuration.
     */
    private static final String FILTER_CHAIN_PID = "org.opendaylight.aaa.filterchain";

    /**
     * The property in the filterchain configuration that holds the registered filters.
     */
    private static final String FILTER_LIST_PROPERTY = "customFilterList";

    /**
     * The PID used to access the configuration of our module.
     */
    private static final String TSDR_RESTCONF_COLLECTOR_PID = "tsdr.restconf.collector";

    /**
     * The full name of the filter class, used when registering and unregistering the filter in the filterchain.
     */
    private static final String COLLECTOR_FILTER_CLASS_NAME
        = TSDRRestconfCollectorFilter.class.getName();

    /**
     * A reference for the logger object responsible for creating the records and storing them.
     */
    private TSDRRestconfCollectorLogger restconfCollectorLogger;

    public TSDRRestconfCollectorModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
        org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public TSDRRestconfCollectorModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
        org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
        org.opendaylight.controller.config.yang.config.tsdr.restconf.collector.TSDRRestconfCollectorModule oldModule,
        java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    /**
     * sets the bundle context of the module.
     * @param bundleContext bundle context of the module
     */
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    /**
     * returns the bundle context of the module.
     * @return bundle context of the module
     */
    public BundleContext getBundleContext() {
        return this.bundleContext;
    }

    /**
     * registers the TSDRRestconfCollectorFilter in the aaa filterchain.
     */
    private void registerFilter() {
        try {
            ServiceReference configurationAdminReference = bundleContext
                .getServiceReference(ConfigurationAdmin.class.getName());

            if (configurationAdminReference != null) {
                ConfigurationAdmin confAdmin
                    = (ConfigurationAdmin) bundleContext.getService(configurationAdminReference);

                filterChainConfiguration = confAdmin.getConfiguration(FILTER_CHAIN_PID);
                Dictionary properties = filterChainConfiguration.getProperties();

                String customFilterList = (String)properties.get(FILTER_LIST_PROPERTY);

                if (customFilterList != null && !(customFilterList.trim().equals(""))) {

                    if (!customFilterList.contains(COLLECTOR_FILTER_CLASS_NAME)) {
                        customFilterList += "," + COLLECTOR_FILTER_CLASS_NAME;
                    }

                } else {
                    customFilterList = COLLECTOR_FILTER_CLASS_NAME;
                }

                properties.put(FILTER_LIST_PROPERTY, customFilterList);
                filterChainConfiguration.update(properties);
            } else {
                LOG.error("Unable to register TSDRRestconfCollectorFilter: ConfigurationAdmin not found");
            }
        } catch (IOException e) {
            LOG.error(e.toString());
        }
    }

    /**
     * registers the TSDRRestconfCollectorConfig as a service that listens to changes in the configuration.
     */
    private void registerConfiguration() {
        Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put(Constants.SERVICE_PID, TSDR_RESTCONF_COLLECTOR_PID);
        bundleContext.registerService(ManagedService.class.getName(),
            TSDRRestconfCollectorConfig.getInstance() , properties);
    }

    /**
     * removes the TSDRRestconfCollectorFilter from the aaa filterchain. This is not really necessary, because if the
     * filter remained in the list even when the module was removed, filterchain will only complain that the filter
     * does not exist, but no other problems will occur.
     */
    private void unregisterFilter() {
        try {
            Dictionary properties = filterChainConfiguration.getProperties();
            String customFilterList = (String)properties.get(FILTER_LIST_PROPERTY);

            if (customFilterList != null) {
                if (customFilterList.contains(COLLECTOR_FILTER_CLASS_NAME)) {
                    String[] filters = customFilterList.split(",");
                    customFilterList = "";
                    for (int i = 0; i < filters.length; i++) {
                        if (!filters[i].equals(COLLECTOR_FILTER_CLASS_NAME)) {
                            customFilterList += filters[i];
                            customFilterList += ",";
                        }
                    }

                    // Remove the trailing comma
                    if (customFilterList.length() > 0) {
                        customFilterList = customFilterList.substring(0, customFilterList.length() - 1);
                    }

                    properties.put(FILTER_LIST_PROPERTY, customFilterList);
                    filterChainConfiguration.update(properties);
                }
            }
        } catch (IOException e) {
            LOG.error(e.toString());
        }
    }

    /**
     * called when the module is created, it provides the logger with a reference to the collector SPI service,
     * registers the filter in the filterchain, and registers the configuration class as a service for the collector's
     * PID.
     */
    @Override
    public java.lang.AutoCloseable createInstance() {
        restconfCollectorLogger = TSDRRestconfCollectorLogger.getInstance();

        restconfCollectorLogger.setTsdrCollectorSpiService(getRpcRegistryDependency().getRpcService(
            TsdrCollectorSpiService.class));

        registerFilter();
        registerConfiguration();

        return this;
    }

    /**
     * called before the module is destroyed, it shuts down the logger, and unregisters the filter from the filterchain.
     */
    @Override
    public void close() {
        restconfCollectorLogger.shutDown();
        this.unregisterFilter();
        LOG.info("TSDR Restconf Data Collector (instance {}) torn down.", this);
    }
}
