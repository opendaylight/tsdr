/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.cassandra;

import java.util.Map;
import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.opendaylight.tsdr.spi.persistence.TSDRBinaryPersistenceService;
import org.opendaylight.tsdr.spi.persistence.TSDRLogPersistenceService;
import org.opendaylight.tsdr.spi.persistence.TSDRMetricPersistenceService;
import org.opendaylight.tsdr.spi.util.ConfigFileUtil;
import org.osgi.framework.BundleContext;

/**
 * Bundle activator.
 *
 * @author Sharon Aicler(saichler@gmail.com)
 */
public class Activator extends DependencyActivatorBase {

    @Override
    public void init(BundleContext context, DependencyManager manager) throws Exception {
        final TSDRCassandraPersistenceServiceImpl impl = new TSDRCassandraPersistenceServiceImpl();
        Map<String, String> props = ConfigFileUtil.loadConfig(ConfigFileUtil.CASSANDRA_STORE_CONFIG_FILE);
        if (ConfigFileUtil.isMetricPersistenceEnabled(props)) {
            manager.add(
                    createComponent().setInterface(new String[] { TSDRMetricPersistenceService.class.getName() }, null)
                            .setImplementation(impl));
        }
        if (ConfigFileUtil.isLogPersistenceEnabled(props)) {
            manager.add(createComponent().setInterface(new String[] { TSDRLogPersistenceService.class.getName() }, null)
                    .setImplementation(impl));
        }
        if (ConfigFileUtil.isBinaryPersistenceEnabled(props)) {
            manager.add(
                    createComponent().setInterface(new String[] { TSDRBinaryPersistenceService.class.getName() }, null)
                            .setImplementation(impl));
        }
    }

    @Override
    public void destroy(BundleContext context, DependencyManager manager) throws Exception {
    }
}
