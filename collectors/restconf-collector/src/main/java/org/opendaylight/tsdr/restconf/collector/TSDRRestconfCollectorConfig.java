/*
 * Copyright (c) 2016 Saugo360 and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.restconf.collector;

import com.google.common.annotations.VisibleForTesting;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
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
 *
 *         Created: Dec 16th, 2016
 *
 */
public class TSDRRestconfCollectorConfig implements ManagedService {

    /**
     * The instance of this class (for singleton pattern).
     */
    private static TSDRRestconfCollectorConfig INSTANCE = null;

    /**
     * The cached values of the configurations.
     */
    private final Dictionary<String, String> configurations = new Hashtable<>();

    /**
     * The logger of the class.
     */
    private static Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * The default value for which http methods should be logged. GET has been omitted, since there are many GET
     * requests that are usually sent everytime DLUX is started, which will result in a lot of noisy data.
     */
    private static final String DEFAULT_METHODS_TO_LOG = "POST,PUT,DELETE";

    /**
     * The default value for which paths in the tree should be logged.
     */
    private static final String DEFAULT_PATHS_TO_LOG = ".*";

    /**
     * The default value for which IP addresses to log from.
     */
    private static final String DEFAULT_REMOTE_ADDRESSES_TO_LOG = ".*";

    /**
     * The default value for what content of requests should be logged.
     */
    private static final String DEFAULT_CONTENT_TO_LOG = ".*";

    /**
     * An array of possible http methods. There are other methods but they are not used in RESTCONF.
     */
    private static final String[] HTTP_METHODS = {"POST", "PUT", "GET", "DELETE"};

    private TSDRRestconfCollectorConfig() {
    }

    /**
     * returns the instance of the singleton.
     * @return the instance of the class. If the instance was null, a new instance is created
     */
    public static synchronized TSDRRestconfCollectorConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new TSDRRestconfCollectorConfig();
        }
        return INSTANCE;
    }

    /**
     * only call this method in testing to do mocking.
     * @param instance the instance of the class
     */
    @VisibleForTesting
    public static void setInstance(TSDRRestconfCollectorConfig instance) {
        INSTANCE = instance;
    }

    /**
     * only call this method in testing to do mocking.
     * @param logger the instance of the logger
     */
    public static void setLogger(Logger logger) {
        log = logger;
    }

    /**
     * this function is called automatically everytime the configuration of the collector changes, and when the module
     * is first started. The function validates the configuration values and caches them for later use.
     * @param properties the properties of the collector
     */
    @Override
    public synchronized void updated(Dictionary properties) throws ConfigurationException {
        if (properties != null && !properties.isEmpty()) {

            String methods = (String)properties.get("METHODS_TO_LOG");
            if (!validateMethods(methods)) {
                log.error("Value specified for METHODS_TO_LOG: " + methods + " is invalid. Will use default value of "
                    + DEFAULT_METHODS_TO_LOG);
                methods = DEFAULT_METHODS_TO_LOG;
            }
            configurations.put("METHODS_TO_LOG", methods);

            String paths = (String)properties.get("PATHS_TO_LOG");
            if (!validatePaths(paths)) {
                log.error("Value specified for PATHS_TO_LOG: " + paths + " is invalid. Will use default value of "
                    + DEFAULT_PATHS_TO_LOG);
                paths = DEFAULT_PATHS_TO_LOG;
            }
            configurations.put("PATHS_TO_LOG", paths);

            String remoteAddresses = (String)properties.get("REMOTE_ADDRESSES_TO_LOG");
            if (!validateRemoteAddresses(remoteAddresses)) {
                log.error("Value specified for REMOTE_ADDRESSES_TO_LOG: " + remoteAddresses
                    + " is invalid. Will use default value of " + DEFAULT_REMOTE_ADDRESSES_TO_LOG);
                remoteAddresses = DEFAULT_REMOTE_ADDRESSES_TO_LOG;
            }
            configurations.put("REMOTE_ADDRESSES_TO_LOG", remoteAddresses);

            String content = (String)properties.get("CONTENT_TO_LOG");
            if (!validateContent(content)) {
                log.error("Value specified for CONTENT_TO_LOG: " + content + " is invalid. Will use default value of "
                    + DEFAULT_CONTENT_TO_LOG);
                content = DEFAULT_CONTENT_TO_LOG;
            }
            configurations.put("CONTENT_TO_LOG", content);

            log.info("TSDRRestconfCollectorConfig updated with {}", configurations);

        } else {

            log.error("The configuration properties are either empty or non-existent will use default values of: "
                + "METHODS_TO_LOG=" + DEFAULT_METHODS_TO_LOG + " PATHS_TO_LOG=" + DEFAULT_PATHS_TO_LOG
                + " REMOTE_ADDRESSES_TO_LOG=" + DEFAULT_REMOTE_ADDRESSES_TO_LOG + " CONTENT_TO_LOG="
                + DEFAULT_CONTENT_TO_LOG);

            configurations.put("METHODS_TO_LOG", DEFAULT_METHODS_TO_LOG);
            configurations.put("PATHS_TO_LOG", DEFAULT_PATHS_TO_LOG);
            configurations.put("REMOTE_ADDRESSES_TO_LOG", DEFAULT_REMOTE_ADDRESSES_TO_LOG);
            configurations.put("CONTENT_TO_LOG", DEFAULT_CONTENT_TO_LOG);
        }
    }

    /**
     * called by users of the class to retrieve properties of the collector.
     * @param name the name of the property whose value we want to retrieve
     * @return the value of the property
     */
    public String getProperty(String name) {
        return this.configurations.get(name);
    }

    /**
     * validates that the list of http methods is valid to be used.
     * the list should be comma-separated, should contain valid HTTP methods, and should not have the methods repeated
     * multiple times
     * @param methds the list of http methods to validate
     * @return returns true if the list is valid
     */
    private boolean validateMethods(String methods) {
        if (methods != null && !methods.equals("")) {

            // We check the format of the methods
            String[] methodsArray = methods.split(",");

            // Each element in the array needs to be one of the known HTTP methods,
            // and needs to be unique
            for (int i = 0; i < methodsArray.length; i++) {
                if (!Arrays.asList(HTTP_METHODS).contains(methodsArray[i])) {
                    log.error("HTTP method " + methodsArray[i] + " is not recognized");
                    return false;
                }

                for (int j = 0; j < methodsArray.length; j++) {
                    if (i == j) {
                        continue;
                    }

                    if (methodsArray[i].equals(methodsArray[j])) {
                        log.error("HTTP method " + methodsArray[i] + " is repeated multiple times");
                        return false;
                    }
                }
            }
            return true;
        }
        log.error("HTTP_METHODS_TO_LOG is either empty or non-existent");
        return false;
    }

    /**
     * validates that the path specified is valid to be used.
     * the path should be a valid regular expression
     * @param paths the regular expression of the paths
     * @return returns true if the paths expression is a valid regular expression
     */
    private boolean validatePaths(String paths) {
        if (paths != null && !paths.equals("")) {
            try {
                Pattern.compile(paths);
            } catch (PatternSyntaxException exception) {
                log.error("Pattern " + paths + " is not parsable. Error: " + exception.toString());
                return false;
            }
            return true;
        }
        log.error("PATHS_TO_LOG is either empty or non-existent");
        return false;
    }

    /**
     * validates that the remote addresses specified is valid to be used.
     * the remote addresses expression should be a valid regular expression
     * @param addresses the regular expression of the remote addresses
     * @return returns true if the remote addresses expression is a valid regular expression
     */
    private boolean validateRemoteAddresses(String addresses) {
        if (addresses != null && !addresses.equals("")) {
            try {
                Pattern.compile(addresses);
            } catch (PatternSyntaxException exception) {
                log.error("Pattern " + addresses + " is not parsable. Error: " + exception.toString());
                return false;
            }
            return true;
        }
        log.error("REMOTE_ADDRESSES_TO_LOG is either empty or non-existent");
        return false;
    }

    /**
     * validates that the content specified is valid to be used.
     * the content should be a valid regular expression
     * @param content the regular expression of the paths
     * @return returns true if the content expression is a valid regular expression
     */
    private boolean validateContent(String content) {
        if (content != null && !content.equals("")) {
            try {
                Pattern.compile(content);
            } catch (PatternSyntaxException exception) {
                log.error("Pattern " + content + " is not parsable. Error: " + exception.toString());
                return false;
            }
            return true;
        }
        log.error("CONTENT_TO_LOG is either empty or non-existent");
        return false;
    }

}
