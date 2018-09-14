/*
 * Copyright (c) 2016 Saugo360 and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.restconf.collector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opendaylight.tsdr.restconf.collector.TSDRRestconfCollectorConfig.ADDRESSES_TO_LOG;
import static org.opendaylight.tsdr.restconf.collector.TSDRRestconfCollectorConfig.CONTENT_TO_LOG;
import static org.opendaylight.tsdr.restconf.collector.TSDRRestconfCollectorConfig.METHODS_TO_LOG;
import static org.opendaylight.tsdr.restconf.collector.TSDRRestconfCollectorConfig.PATHS_TO_LOG;

import com.google.common.collect.ImmutableSet;
import java.util.Dictionary;
import java.util.Hashtable;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;

/**
 * This class is responsible for testing the TSDRRestconfCollectorConfig class.
 *
 * @author <a href="mailto:a.alhamali93@gmail.com">AbdulRahman AlHamali</a>
 */
public class TSDRRestconfCollectorConfigTest {

    /**
     * the restconf collector config instance that we want to run our tests on.
     */
    private final TSDRRestconfCollectorConfig configObject = new TSDRRestconfCollectorConfig();

    @Test
    public void testCorrectValues() throws ConfigurationException {
        Dictionary<String, String> properties = new Hashtable<>();

        properties.put(METHODS_TO_LOG, "GET,PUT");
        properties.put(PATHS_TO_LOG, "/operations/.*");
        properties.put(ADDRESSES_TO_LOG, "127\\.0\\.0\\.1");
        properties.put(CONTENT_TO_LOG, ".*loggable.*");

        configObject.updated(properties);

        assertEquals(ImmutableSet.of("GET", "PUT"), configObject.getMethodsToLog());

        assertTrue(configObject.getPathsToLog().matcher("/operations/foo").matches());
        assertFalse(configObject.getPathsToLog().matcher("/operation/foo").matches());

        assertTrue(configObject.getAddressesToLog().matcher("127.0.0.1").matches());
        assertFalse(configObject.getAddressesToLog().matcher("128.0.0.1").matches());

        assertTrue(configObject.getContentToLog().matcher("this is loggable ...").matches());
        assertFalse(configObject.getContentToLog().matcher("this is not").matches());
    }

    @Test(expected = ConfigurationException.class)
    public void testUnrecognizedMethod() throws ConfigurationException {
        Dictionary<String, String> properties = new Hashtable<>();

        properties.put(METHODS_TO_LOG, "GERT,PUT");

        configObject.updated(properties);
    }

    @Test(expected = ConfigurationException.class)
    public void testInvalidPathRegex() throws ConfigurationException {
        Dictionary<String, String> properties = new Hashtable<>();

        properties.put(PATHS_TO_LOG, "(");

        configObject.updated(properties);
    }

    @Test(expected = ConfigurationException.class)
    public void testInvalidRemoteAddressesRegex() throws ConfigurationException {
        Dictionary<String, String> properties = new Hashtable<>();

        properties.put(ADDRESSES_TO_LOG, "(");

        configObject.updated(properties);
    }

    @Test(expected = ConfigurationException.class)
    public void testInvalidContentRegex() throws ConfigurationException {
        Dictionary<String, String> properties = new Hashtable<>();

        properties.put(CONTENT_TO_LOG, "(");

        configObject.updated(properties);
    }

    @Test
    public void testEmptyProperties() throws ConfigurationException {
        Dictionary<String, String> properties = new Hashtable<>();

        properties.put(METHODS_TO_LOG, "");
        properties.put(PATHS_TO_LOG, "");
        properties.put(ADDRESSES_TO_LOG, "");
        properties.put(CONTENT_TO_LOG, "");

        configObject.updated(properties);

        assertEquals(ImmutableSet.of("POST", "PUT", "DELETE", "PATCH"), configObject.getMethodsToLog());
        assertTrue(configObject.getPathsToLog().matcher("foo").matches());
        assertTrue(configObject.getAddressesToLog().matcher("foo").matches());
        assertTrue(configObject.getContentToLog().matcher("foo").matches());
    }

    @Test
    public void testInit() throws ConfigurationException {
        configObject.init();

        assertEquals(ImmutableSet.of("POST", "PUT", "DELETE", "PATCH"), configObject.getMethodsToLog());
        assertTrue(configObject.getPathsToLog().matcher("foo").matches());
        assertTrue(configObject.getAddressesToLog().matcher("foo").matches());
        assertTrue(configObject.getContentToLog().matcher("foo").matches());
    }
}
