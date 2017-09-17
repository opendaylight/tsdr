/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.datapurge.test;
/**
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 * <p>
 * Created: January 1, 2016
 */

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.After;
import org.junit.Test;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.tsdr.datapurge.PurgingScheduler;

public class PurgeSchedulerTest {
    private RpcProviderRegistry rpcRegistry = mock(RpcProviderRegistry.class);
    PurgingScheduler purgingScheduler = new PurgingScheduler(rpcRegistry, true, 24 * 60, "23:59:59", 7 * 24);

    /**
     * Test the reSchedule() method.
     */
    @Test
    public void testSchedule() {
        purgingScheduler.schedule();
        assertTrue(purgingScheduler.isRunning());
    }

    /**
     * Test the cancelScheduledTask() method.
     */
    @Test
    public void testCancelScheduledTask() {
        purgingScheduler.schedule();
        purgingScheduler.cancelScheduledTask();
        assertTrue(!purgingScheduler.isRunning());
    }

    /**
     * Test the isEnabled() method.
     */
    @Test
    public void testGetIsEnabled() {
        assertTrue(purgingScheduler.isEnabled());
    }

    /**
     * Test the getRetentionTime() method.
     */
    @Test
    public void testGetRetentionTime() {
        assertTrue(purgingScheduler.getRetentionTime() == 7 * 24);
    }

    /**
     * Test the getPurgingTime() method.
     */
    @Test
    public void testGetPurgingTime() {
        assertTrue(purgingScheduler.getPurgingTime().equals("23:59:59"));
    }

    /**
     * Test the getPurgingInterval() method.
     */
    @Test
    public void testGetPurgingInterval() {
        assertTrue(purgingScheduler.getPurgingInterval() == 1440);
    }

    @After
    public void teardown() {
        rpcRegistry = null;
        //make sure no scheduled purging task kicked off by this UT.
        purgingScheduler.cancelScheduledTask();
    }
}
