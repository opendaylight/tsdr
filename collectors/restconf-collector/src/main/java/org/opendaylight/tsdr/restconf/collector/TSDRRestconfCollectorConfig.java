/*
 * Copyright (c) 2016 Saugo360 and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.restconf.collector;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for reading the configuration of the module from its configuration file, and providing
 * this configuration for the other classes. In addition, the class validates the values of the configuration items
 * and replaces them with default values in case they were invalid.
 * It is worth mentioning that there were two options for storing the configuration; the first was in a configuration
 * file, the second was in the MD-SAL. I chose the former, because one of the main reasons for creating this module,
 * is being able to review restconf accesses in case of penetration, in order to see what the intruder has, read,
 * changed, deleted, etc. However, if the configuration is stored in the MD-SAL, the intruder could start by switching
 * off the collector (since MD-SAL configuration is accessible from restconf), and then doing whatever he/she wants to
 * do. That's why it is safer to place the configuration in a configuration file.
 *
 * @author <a href="mailto:a.alhamali93@gmail.com">AbdulRahman AlHamali</a>
 */
@Singleton
public class TSDRRestconfCollectorConfig implements ManagedService {
    private static final Logger LOG = LoggerFactory.getLogger(TSDRRestconfCollectorConfig.class);

    static final String METHODS_TO_LOG = "METHODS_TO_LOG";
    static final String CONTENT_TO_LOG = "CONTENT_TO_LOG";
    static final String ADDRESSES_TO_LOG = "REMOTE_ADDRESSES_TO_LOG";
    static final String PATHS_TO_LOG = "PATHS_TO_LOG";

    /**
     * The default value for which http methods should be logged. GET has been omitted, since there are many GET
     * requests which can result in a lot of noisy data.
     */
    private static final String DEFAULT_METHODS_TO_LOG = "POST,PUT,DELETE,PATCH";

    /**
     * The default value for which paths in the tree should be logged.
     */
    private static final String DEFAULT_PATHS_TO_LOG = ".*";

    /**
     * The default value for which IP addresses to log from.
     */
    private static final String DEFAULT_ADDRESSES_TO_LOG = ".*";

    /**
     * The default value for what content of requests should be logged.
     */
    private static final String DEFAULT_CONTENT_TO_LOG = ".*";

    /**
     * The set of possible http methods. There are other methods but they are not used in RESTCONF.
     */
    private static final Set<String> ALLOWED_HTTP_METHODS = ImmutableSet.of("POST", "PUT", "GET", "DELETE", "PATCH");

    private volatile Set<String> methodsToLog;
    private volatile Pattern pathsToLog;
    private volatile Pattern addressesToLog;
    private volatile Pattern contentToLog;

    @Inject
    public TSDRRestconfCollectorConfig() {
    }

    @PostConstruct
    public void init() throws ConfigurationException {
        parseProperties(null);
    }

    /**
     * this function is called automatically every time the configuration of the collector changes, and when the module
     * is first started. The function validates the configuration values and caches them for later use.
     * @param properties the properties of the collector
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    @Override
    public synchronized void updated(Dictionary properties) throws ConfigurationException {
        parseProperties(properties);

        LOG.info("TSDRRestconfCollectorConfig updated with: {} : {}, {} : {}, {} : {}, {} : {}",
            METHODS_TO_LOG, methodsToLog, PATHS_TO_LOG, pathsToLog, ADDRESSES_TO_LOG, addressesToLog,
            CONTENT_TO_LOG, contentToLog);
    }

    public Set<String> getMethodsToLog() {
        return methodsToLog;
    }

    public Pattern getPathsToLog() {
        return pathsToLog;
    }

    public Pattern getAddressesToLog() {
        return addressesToLog;
    }

    public Pattern getContentToLog() {
        return contentToLog;
    }

    private void parseProperties(Dictionary<String, String> properties) throws ConfigurationException {
        if (properties == null) {
            properties = new Hashtable<>();
        }

        methodsToLog = parseProperty(properties, METHODS_TO_LOG, DEFAULT_METHODS_TO_LOG, methods ->
            ImmutableSet.copyOf(Arrays.asList(methods.split(",")).stream().peek(
                TSDRRestconfCollectorConfig::validateMethod).iterator()));

        pathsToLog = parseProperty(properties, PATHS_TO_LOG, DEFAULT_PATHS_TO_LOG, Pattern::compile);

        addressesToLog = parseProperty(properties, ADDRESSES_TO_LOG, DEFAULT_ADDRESSES_TO_LOG, Pattern::compile);

        contentToLog = parseProperty(properties, CONTENT_TO_LOG, DEFAULT_CONTENT_TO_LOG, Pattern::compile);
    }

    @SuppressWarnings("checkstyle:IllegalCatch")
    private static <T> T parseProperty(Dictionary<String, String> from, String name, String defaultValue,
            Function<String, T> parser) throws ConfigurationException {
        String value = from.get(name);
        if (Strings.isNullOrEmpty(value)) {
            LOG.debug("{} is either empty or non-existent - using default setting of {}", name, defaultValue);
            value = defaultValue;
        }

        try {
            return parser.apply(value);
        } catch (RuntimeException e) {
            throw new ConfigurationException(name, "Value \"" + value + "\" is invalid", e);
        }
    }

    private static void validateMethod(String method) {
        if (!ALLOWED_HTTP_METHODS.contains(method)) {
            throw new IllegalArgumentException("Invalid HTTP method " + method);
        }
    }
}
