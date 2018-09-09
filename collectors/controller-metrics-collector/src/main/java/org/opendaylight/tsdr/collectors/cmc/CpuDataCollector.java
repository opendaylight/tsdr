/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.collectors.cmc;

import java.util.OptionalDouble;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by tylevine on 8/30/16.
 */
public abstract class CpuDataCollector implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(CpuDataCollector.class);

    abstract OptionalDouble getControllerCpu();

    abstract OptionalDouble getMachineCpu();

    public static CpuDataCollector getCpuDataCollector() {
        // attempt to use the sun cpu data collector first (unless FORCE_SIGAR env var is set)
        if (System.getenv("FORCE_SIGAR") == null) {
            try {
                final SunCpuDataCollector sunCollector = new SunCpuDataCollector();
                LOG.info("Sun OperatingSystemMXBean is present, using it for CPU data");
                return sunCollector;
            } catch (final SunOsMBeanNotPresentException e) {
                LOG.info("Sun OS MBean is not present, attempting to fall back to Sigar library for CPU usage data");
            }
        } else {
            LOG.info("FORCE_SIGAR environment variable is set. Not using Sun CPU data collector");
        }

        // otherwise try to use Sigar cpu data collector
        try {
            final SigarCpuDataCollector sigarCollector = new SigarCpuDataCollector();
            LOG.info("Sun OperatingSystemMXBean is not present, but Sigar library is installed. "
                    + "Falling back to Sigar for CPU data");
            return sigarCollector;
        } catch (final SigarNotPresentException e) {
            LOG.warn("Sigar library was not found");
        }

        // no way to get controller CPU usage data!!
        LOG.warn("*** TSDR is unable to provide CPU usage data. If you need CPU usage data, either use an Oracle JVM "
                + "or install the Sigar library (see the User Guide) ***");

        return getNullCpuDataCollector();
    }

    public static CpuDataCollector getNullCpuDataCollector() {
        return new CpuDataCollector() {
            @Override
            OptionalDouble getMachineCpu() {
                return OptionalDouble.empty();
            }

            @Override
            OptionalDouble getControllerCpu() {
                return OptionalDouble.empty();
            }
        };
    }

    // not all implementations will need to close dependencies, but those which do can override
    @Override
    public void close() throws Exception {
    }
}
