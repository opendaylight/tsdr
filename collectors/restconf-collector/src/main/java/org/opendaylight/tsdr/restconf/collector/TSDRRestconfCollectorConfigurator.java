/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.restconf.collector;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TSDRRestconfCollectorConfigurator implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(TSDRRestconfCollectorConfigurator.class);

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
    private static final String COLLECTOR_FILTER_CLASS_NAME = TSDRRestconfCollectorFilter.class.getName();

    private final BundleContext bundleContext;

    private ServiceRegistration<ManagedService> serviceRegistration;

    /**
     * A reference to the configuration of the filter chain, where we add/remove our filter.
     */
    private Configuration filterChainConfiguration;

    public TSDRRestconfCollectorConfigurator(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void init() {
        registerFilter();
        registerConfiguration();
    }

    @Override
    public void close() {
        unregisterFilter();

        if (serviceRegistration != null) {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }
    }

    /**
     * Registers the TSDRRestconfCollectorFilter in the aaa filterchain.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void registerFilter() {
        ServiceReference<ConfigurationAdmin> configurationAdminReference = null;
        try {
            configurationAdminReference = bundleContext.getServiceReference(ConfigurationAdmin.class);

            if (configurationAdminReference != null) {
                ConfigurationAdmin confAdmin = bundleContext.getService(configurationAdminReference);

                filterChainConfiguration = confAdmin.getConfiguration(FILTER_CHAIN_PID);
                Dictionary properties = filterChainConfiguration.getProperties();

                String customFilterList = (String) properties.get(FILTER_LIST_PROPERTY);

                if (customFilterList != null && !customFilterList.trim().isEmpty()) {
                    if (!customFilterList.contains(COLLECTOR_FILTER_CLASS_NAME)) {
                        customFilterList += "," + COLLECTOR_FILTER_CLASS_NAME;
                    }

                } else {
                    customFilterList = COLLECTOR_FILTER_CLASS_NAME;
                }

                properties.put(FILTER_LIST_PROPERTY, customFilterList);
                filterChainConfiguration.update(properties);

                LOG.info("Updated aaa {} property to {}", FILTER_LIST_PROPERTY, customFilterList);
            } else {
                LOG.error("Unable to register TSDRRestconfCollectorFilter: ConfigurationAdmin not found");
            }
        } catch (IOException e) {
            LOG.error("Error updating aaa {} property", FILTER_LIST_PROPERTY, e);
        } finally {
            if (configurationAdminReference != null) {
                bundleContext.ungetService(configurationAdminReference);
            }
        }
    }

    /**
     * Removes the TSDRRestconfCollectorFilter from the aaa filterchain. This is not really necessary, because if the
     * filter remained in the list even when the module was removed, filterchain will only complain that the filter
     * does not exist, but no other problems will occur.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void unregisterFilter() {
        if (filterChainConfiguration == null) {
            return;
        }

        try {
            Dictionary properties = filterChainConfiguration.getProperties();
            String customFilterList = (String) properties.get(FILTER_LIST_PROPERTY);

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
            LOG.error("Error updating aaa {} property", FILTER_LIST_PROPERTY, e);
        }
    }

    /**
     * Registers the TSDRRestconfCollectorConfig as a service that listens to changes in the configuration.
     */
    private void registerConfiguration() {
        Hashtable<String, String> properties = new Hashtable<>();
        properties.put(Constants.SERVICE_PID, TSDR_RESTCONF_COLLECTOR_PID);
        serviceRegistration = bundleContext.registerService(ManagedService.class,
                TSDRRestconfCollectorConfig.getInstance(), properties);
    }
}
