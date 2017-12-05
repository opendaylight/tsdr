/*
 * Copyright (c) 2016 Frinx s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.persistence.elasticsearch;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.opendaylight.tsdr.spi.persistence.TSDRBinaryPersistenceService;
import org.opendaylight.tsdr.spi.persistence.TSDRLogPersistenceService;
import org.opendaylight.tsdr.spi.persistence.TSDRMetricPersistenceService;
import org.opendaylight.tsdr.spi.util.ConfigFileUtil;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Activator initialize/destroy elasticsearch data store for TSDR.
 *
 * @author Lukas Beles(lbeles@frinx.io)
 */
public class Activator extends DependencyActivatorBase {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private ElasticSearchStore store;

    /**
     * Initialize elasticsearch data store service.
     */
    @Override
    public void init(BundleContext context, DependencyManager manager) throws Exception {
        LOG.info("Initializing TSDR Elasticsearch data store bundle ...");
        Map<String, String> properties = loadElasticsearchStore();
        TsdrElasticSearchPersistenceServiceImpl impl = TsdrElasticSearchPersistenceServiceImpl.create(store);
        if (ConfigFileUtil.isMetricPersistenceEnabled(properties)) {
            manager.add(createComponent()
                    .setInterface(new String[]{TSDRMetricPersistenceService.class.getName()}, null)
                    .setImplementation(impl));
        }
        if (ConfigFileUtil.isLogPersistenceEnabled(properties)) {
            manager.add(createComponent()
                    .setInterface(new String[]{TSDRLogPersistenceService.class.getName()}, null)
                    .setImplementation(impl));
        }
        if (ConfigFileUtil.isBinaryPersistenceEnabled(properties)) {
            manager.add(createComponent()
                    .setInterface(new String[]{TSDRBinaryPersistenceService.class.getName()}, null)
                    .setImplementation(impl));
        }
        LOG.info("TSDR Elasticsearch data store bundle was initialized successfully");
    }

    @VisibleForTesting
    Map<String, String> loadElasticsearchStore() throws IOException {
        Map<String, String> properties = ConfigFileUtil.loadConfig(ConfigFileUtil.ELASTICSEARCH_STORE_CONFIG_FILE);
        store = ElasticSearchStore.create(properties, null);
        store.startAsync();
        return properties;
    }

    /**
     * Destroy elasticsearch data store service.
     */
    @Override
    public void destroy(BundleContext context, DependencyManager manager) throws Exception {
        LOG.info("Destroying TSDR Elasticsearch data store bundle ...");
        try {
            if (store != null) {
                store.stopAsync().awaitTerminated(3L, TimeUnit.SECONDS);
                LOG.info("TSDR Elasticsearch data store bundle was destroyed successfully");
            }
        } catch (TimeoutException e) {
            LOG.error("Could not destroyed TSDR Elasticsearch data store bundle: {}", e);
        }
    }

    @VisibleForTesting
    void setStore(ElasticSearchStore store) {
        this.store = store;
    }
}
