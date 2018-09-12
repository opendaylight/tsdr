/*
 * Copyright © 2018 Kontron Canada Inc and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.log;

import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.opendaylight.tsdr.spi.util.ConfigFileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for getting a list of ignored class for logging and
 * compile them and expose them to other classes.
 *
 * @author Matthieu Cauffiez
 */
public class TsdrLogCollectorConfig {
    private static final String IGNORED_CLASSES_KEY = "ignored-classes";
    private static final String SEPARATOR = ",";
    private static final Logger LOG = LoggerFactory
            .getLogger(TsdrLogCollectorConfig.class);

    private final Set<Pattern> ignoredCategories;

    public TsdrLogCollectorConfig(String configFileName) {
        Map<String, String> configFile = Collections.emptyMap();
        try {
            configFile = ConfigFileUtil.loadConfig(configFileName);
        }
        catch (IOException e) {
            LOG.error("Using default values, error when opening the file {} : ", configFileName, e);
        }
        ignoredCategories = Collections.unmodifiableSet(
                ImmutableSet.<String>builder().add("org.opendaylight.tsdr.*")
                .add("org.apache.aries.*")
                .add(configFile.getOrDefault(IGNORED_CLASSES_KEY, "")
                        .split(SEPARATOR)).build().stream()
                .map(str -> Pattern.compile(str)).collect(Collectors.toSet()));
    }

    public Set<Pattern> getIgnoredCategories() {
        return ignoredCategories;
    }
}
