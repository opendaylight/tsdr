/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.spi.util;

import com.google.common.collect.Maps;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

public final class ConfigFileUtil {
    public static final String CONFIG_DIR = "." + File.separator + "etc" + File.separator;

    public static final String METRIC_PERSISTENCE_PROPERTY = "metric-persistency";
    public static final String LOG_PERSISTENCE_PROPERTY = "log-persistency";
    public static final String BINARY_PERSISTENCE_PROPERTY = "binary-persistency";

    private ConfigFileUtil() {
        throw new AssertionError(); // Protection from reflection.
    }

    public static Map<String, String> loadConfig(String config) throws IOException {
        Properties properties = new Properties();
        properties.load(Files.asCharSource(new File(config), StandardCharsets.UTF_8).openStream());
        return Maps.fromProperties(properties);
    }

    public static boolean isMetricPersistenceEnabled(Map<String, String> properties) {
        return Boolean.valueOf(properties.getOrDefault(METRIC_PERSISTENCE_PROPERTY, Boolean.FALSE.toString()));
    }

    public static boolean isLogPersistenceEnabled(Map<String, String> properties) {
        return Boolean.valueOf(properties.getOrDefault(LOG_PERSISTENCE_PROPERTY, Boolean.FALSE.toString()));
    }

    public static boolean isBinaryPersistenceEnabled(Map<String, String> properties) {
        return Boolean.valueOf(properties.getOrDefault(BINARY_PERSISTENCE_PROPERTY, Boolean.FALSE.toString()));
    }
}
