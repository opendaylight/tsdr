/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.datapurge.test;

import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.tsdr.datapurge.TSDRDataPurgeConfig;

import static org.junit.Assert.assertTrue;

/**
*
*
* @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
*
* Created: January 1, 2016
*/
public class TSDRDataPurgeConfigTest {

    private TSDRDataPurgeConfig tsdrPurgeConfig = TSDRDataPurgeConfig.getInstance();

    @Before
    public void setup() {
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("data_purge_enabled", "true");
        properties.put("data_purge_time", "23:59:59");
        properties.put("data_purge_interval_in_minutes", "1440");
        properties.put("retention_time_in_hours", "168");
        try{
            tsdrPurgeConfig.updated(properties);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    /**
     * Test getProperty() method.
     */
    @Test
    public void testGetProperty(){
        assertTrue(tsdrPurgeConfig.getProperty("data_purge_enabled").equals("true"));
        assertTrue(tsdrPurgeConfig.getProperty("data_purge_time").equals("23:59:59"));
        assertTrue(tsdrPurgeConfig.getProperty("data_purge_interval_in_minutes").equals("1440"));
        assertTrue(tsdrPurgeConfig.getProperty("retention_time_in_hours").equals("168"));
    }
    /**
     * Test getConfiguration() method.
     */
    @Test
    public void testGetConfiguration(){
        Dictionary<String, Object> configProperties = tsdrPurgeConfig.getConfiguration();
        assertTrue(configProperties.get("data_purge_enabled").equals("true"));
        assertTrue(configProperties.get("data_purge_time").equals("23:59:59"));
        assertTrue(configProperties.get("data_purge_interval_in_minutes").equals("1440"));
        assertTrue(configProperties.get("retention_time_in_hours").equals("168"));
    }
    /**
     * Test updated() method.
     */
    @Test
    public void testUpdated(){
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("data_purge_enabled", "false");
        try{
            tsdrPurgeConfig.updated(properties);
        }catch (Exception e){
            e.printStackTrace();
        }
        assertTrue(tsdrPurgeConfig.getProperty("data_purge_enabled").equals("false"));
    }

    @After
    public void teardown() {
        tsdrPurgeConfig = null;
    }
}
