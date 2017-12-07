/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.spi.persistence;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;
import org.opendaylight.tsdr.spi.util.ConfigFileUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Conditionally registers persistence services via OSGi.
 *
 * @author Thomas Pantelis
 */
public class TSDRPersistenceServiceRegistrar implements AutoCloseable {
    private final ServiceRegistration<TSDRMetricPersistenceService> metricServiceRegistration;
    private final ServiceRegistration<TSDRLogPersistenceService> logServiceRegistration;
    private final ServiceRegistration<TSDRBinaryPersistenceService> binaryServiceRegistration;

    public TSDRPersistenceServiceRegistrar(Object implementation, String configFileName, BundleContext bundleContext)
            throws IOException {
        Map<String, String> props = ConfigFileUtil.loadConfig(configFileName);

        metricServiceRegistration = maybeRegister((TSDRMetricPersistenceService)implementation,
                TSDRMetricPersistenceService.class, bundleContext, ConfigFileUtil.isMetricPersistenceEnabled(props));

        logServiceRegistration = maybeRegister((TSDRLogPersistenceService)implementation,
                TSDRLogPersistenceService.class, bundleContext, ConfigFileUtil.isLogPersistenceEnabled(props));

        binaryServiceRegistration = maybeRegister((TSDRBinaryPersistenceService)implementation,
                TSDRBinaryPersistenceService.class, bundleContext, ConfigFileUtil.isBinaryPersistenceEnabled(props));
    }

    @Override
    public void close() {
        safeUnregister(metricServiceRegistration);
        safeUnregister(logServiceRegistration);
        safeUnregister(binaryServiceRegistration);
    }

    private static <T> ServiceRegistration<T> maybeRegister(T implementation, Class<T> serviceInterface,
            BundleContext bundleContext, boolean enabled) {
        if (enabled) {
            return bundleContext.registerService(serviceInterface, implementation, new Hashtable<>());
        }

        return null;
    }

    private static void safeUnregister(ServiceRegistration<?> reg) {
        if (reg != null) {
            try {
                reg.unregister();
            } catch (IllegalStateException e) {
                // This can be safely ignored
            }
        }
    }
}
