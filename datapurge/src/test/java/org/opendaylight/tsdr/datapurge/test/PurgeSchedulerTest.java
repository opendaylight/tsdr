/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.datapurge.test;
/**
*
*
* @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
*
* Created: January 1, 2016
*/
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.tsdr.datapurge.PurgingScheduler;
import org.opendaylight.tsdr.datapurge.TSDRDataPurgeConfig;

public class PurgeSchedulerTest {
    private RpcProviderRegistry rpcRegistry = mock(RpcProviderRegistry.class);
    PurgingScheduler purgingScheduler = PurgingScheduler.getInstance();

    /**
     * Test the reSchedule() method.
     */
    @Test
    public void testSchedule(){
        purgingScheduler.setEnabled(true);
        purgingScheduler.setPurgingInterval(24 * 60);
        purgingScheduler.setPurgingTime("23:59:59");
        purgingScheduler.setRetentionTime(7 * 24);
        purgingScheduler.schedule();
        assertTrue(purgingScheduler.isRunning());
    }
    /**
     * Test the schedulePurgingTask() method.
     */
    @Test
    public void testSchedulePurgingTask(){
        purgingScheduler.setEnabled(true);
        purgingScheduler.setPurgingInterval(24 * 60);
        purgingScheduler.setPurgingTime("23:59:59");
        purgingScheduler.setRetentionTime(7 * 24);
        purgingScheduler.schedulePurgingTask();
        assertTrue(purgingScheduler.isRunning());
    }
    /**
     * Test the cancelScheduledTask() method.
     */
    @Test
    public void testCancelScheduledTask(){
        purgingScheduler.setEnabled(true);
        purgingScheduler.setPurgingInterval(24 * 60);
        purgingScheduler.setPurgingTime("23:59:59");
        purgingScheduler.setRetentionTime(7 * 24);
        purgingScheduler.schedulePurgingTask();
        purgingScheduler.cancelScheduledTask();
        assertTrue(!purgingScheduler.isRunning());
    }
    /**
     * Test the isEnabled() method.
     */
    @Test
    public void testGetIsEnabled(){
        purgingScheduler.setEnabled(false);
        assertTrue(purgingScheduler.isEnabled()==false);
        purgingScheduler.setEnabled(true);
        assertTrue(purgingScheduler.isEnabled()==true);
    }
    /**
     * Test the getRetentionTime() method.
     */
    @Test
    public void testGetRetentionTime(){
        purgingScheduler.setRetentionTime(24);;
        assertTrue(purgingScheduler.getRetentionTime()==24);
    }
    /**
     * Test the getPurgingTime() method.
     */
    @Test
    public void testGetPurgingTime(){
        purgingScheduler.setPurgingTime("23:59:59");;;
        assertTrue(purgingScheduler.getPurgingTime().equals("23:59:59"));
        purgingScheduler.setPurgingTime("12:00:00");;;
        assertTrue(purgingScheduler.getPurgingTime().equals("12:00:00"));
    }
    /**
     * Test the getPurgingInterval() method.
     */
    @Test
    public void testGetPurgingInterval(){
        purgingScheduler.setPurgingInterval(1440);
        assertTrue(purgingScheduler.getPurgingInterval() == 1440);
        purgingScheduler.setPurgingInterval(60);
        assertTrue(purgingScheduler.getPurgingInterval() == 60);
    }
    /**
     * Test the isRunning() method.
     */
    @Test
    public void testIsRunning(){
        purgingScheduler.setEnabled(true);
        purgingScheduler.setPurgingInterval(24 * 60);
        purgingScheduler.setPurgingTime("23:59:59");
        purgingScheduler.setRetentionTime(7 * 24);
        purgingScheduler.schedule();
        assertTrue(purgingScheduler.isRunning());
    }
    @After
    public void teardown() {
        rpcRegistry = null;
        //make sure no scheduled purging task kicked off by this UT.
        purgingScheduler.cancelScheduledTask();
    }
}
