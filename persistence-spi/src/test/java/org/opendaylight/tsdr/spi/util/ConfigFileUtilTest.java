/*
 * Copyright (c) 2016 Frinx s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.spi.util;

import static com.google.common.truth.Truth.assertThat;
import static org.opendaylight.tsdr.spi.util.ConfigFileUtil.BINARY_PERSISTENCE_PROPERTY;
import static org.opendaylight.tsdr.spi.util.ConfigFileUtil.LOG_PERSISTENCE_PROPERTY;
import static org.opendaylight.tsdr.spi.util.ConfigFileUtil.METRIC_PERSISTENCE_PROPERTY;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.jledit.utils.Files;
import org.junit.Test;

public class ConfigFileUtilTest {

    @Test
    public void loadConfig() throws Exception {
        String content = ""
                + METRIC_PERSISTENCE_PROPERTY + "=" + Boolean.TRUE.toString() + "\n"
                + LOG_PERSISTENCE_PROPERTY + "=" + Boolean.TRUE.toString() + "\n"
                + BINARY_PERSISTENCE_PROPERTY + "=" + Boolean.TRUE.toString() + "\n";
        File config = File.createTempFile("test_config", null);
        Files.writeToFile(config, content, StandardCharsets.UTF_8);
        Map<String, String> properties = ConfigFileUtil.loadConfig(config.getAbsolutePath());

        assertThat(properties).containsEntry(METRIC_PERSISTENCE_PROPERTY, Boolean.TRUE.toString());
        assertThat(properties).containsEntry(LOG_PERSISTENCE_PROPERTY, Boolean.TRUE.toString());
        assertThat(properties).containsEntry(BINARY_PERSISTENCE_PROPERTY, Boolean.TRUE.toString());

        config.delete();
    }

    @Test
    public void isMetricPersistenceEnabled() throws Exception {
        Map<String, String> properties = new HashMap<>(1);
        assertThat(ConfigFileUtil.isMetricPersistenceEnabled(properties)).isFalse();

        properties.put(METRIC_PERSISTENCE_PROPERTY, Boolean.FALSE.toString());
        assertThat(ConfigFileUtil.isMetricPersistenceEnabled(properties)).isFalse();

        properties.put(METRIC_PERSISTENCE_PROPERTY, Boolean.TRUE.toString());
        assertThat(ConfigFileUtil.isMetricPersistenceEnabled(properties)).isTrue();
    }

    @Test
    public void isLogPersistenceEnabled() throws Exception {
        Map<String, String> properties = new HashMap<>(1);
        assertThat(ConfigFileUtil.isLogPersistenceEnabled(properties)).isFalse();

        properties.put(LOG_PERSISTENCE_PROPERTY, Boolean.FALSE.toString());
        assertThat(ConfigFileUtil.isLogPersistenceEnabled(properties)).isFalse();

        properties.put(LOG_PERSISTENCE_PROPERTY, Boolean.TRUE.toString());
        assertThat(ConfigFileUtil.isLogPersistenceEnabled(properties)).isTrue();
    }

    @Test
    public void isBinaryPersistenceEnabled() throws Exception {
        Map<String, String> properties = new HashMap<>(1);
        assertThat(ConfigFileUtil.isBinaryPersistenceEnabled(properties)).isFalse();

        properties.put(BINARY_PERSISTENCE_PROPERTY, Boolean.FALSE.toString());
        assertThat(ConfigFileUtil.isBinaryPersistenceEnabled(properties)).isFalse();

        properties.put(BINARY_PERSISTENCE_PROPERTY, Boolean.TRUE.toString());
        assertThat(ConfigFileUtil.isBinaryPersistenceEnabled(properties)).isTrue();
    }
}
