/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.restconf.collector;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.util.Dictionary;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.ops4j.pax.cdi.api.OsgiService;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
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
     * The full name of the filter class, used when registering and unregistering the filter in the filterchain.
     */
    private static final String COLLECTOR_FILTER_CLASS_NAME = TSDRRestconfCollectorFilter.class.getName();

    private final ConfigurationAdmin configAdmin;

    /**
     * A reference to the configuration of the filter chain, where we add/remove our filter.
     */
    private Configuration filterChainConfiguration;

    @Inject
    public TSDRRestconfCollectorConfigurator(@OsgiService ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    @PostConstruct
    public void init() {
        registerFilter();
    }

    @Override
    @PreDestroy
    public void close() {
        unregisterFilter();
    }

    /**
     * Registers the TSDRRestconfCollectorFilter in the aaa filterchain.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void registerFilter() {
        try {
            filterChainConfiguration = configAdmin.getConfiguration(FILTER_CHAIN_PID);
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
        } catch (IOException e) {
            LOG.error("Error updating aaa {} property", FILTER_LIST_PROPERTY, e);
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
                    StringBuilder builder = new StringBuilder();
                    for (int i = 0; i < filters.length; i++) {
                        if (!filters[i].equals(COLLECTOR_FILTER_CLASS_NAME)) {
                            builder.append(filters[i]).append(',');
                        }
                    }

                    customFilterList = builder.toString();

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
}
