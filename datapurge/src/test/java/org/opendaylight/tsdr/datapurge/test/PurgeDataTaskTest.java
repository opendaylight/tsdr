/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.datapurge.test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.tsdr.datapurge.PurgeDataTask;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.PurgeAllTSDRRecordInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRService;
/**
 *
 *
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 *
 * Created: January 1, 2016
 */

public class PurgeDataTaskTest {
    private RpcProviderRegistry rpcRegistry = Mockito.mock(RpcProviderRegistry.class);
    PurgeDataTask purgeTask = new PurgeDataTask(rpcRegistry);
    @Before
    public void setup() {
        TSDRService storageService = Mockito.mock(TSDRService.class);
        Mockito.when(rpcRegistry.getRpcService(TSDRService.class)).thenReturn(storageService);
        purgeTask.setRetentionTimeinHours(1440);
    }
    @Test
    /**
     * Test to see if purgeTask.purgeData() is called successfully.
     */
    public void testPurgeData(){
        PurgeDataTask purgeTask = new PurgeDataTask(rpcRegistry);
        purgeTask.purgeData();
        assertTrue(true);
    }

    @Test
    /**
     * Test the getRetentionTimeInHour() method.
     */
    public void testGetRetentionTimeInHour(){
        assertTrue(purgeTask.getRetentionTimeinHours()==1440);
    }
    @Test
    /**
     * Test the setRetentionTimeInHour() method.
     */
    public void testSetRetentionTimeInHour(){
        purgeTask.setRetentionTimeinHours(24);
        assertTrue(purgeTask.getRetentionTimeinHours()==24);
    }
    @Test
    /**
     * Test the runTask() method in PurgeDataTask.
     */
    public void testRunTask(){
        purgeTask.runTask();
        assertTrue(Thread.currentThread().getName().contains("PurgeData"));
    }
    @Test
    /**
     * Test the setScheduledFuture() method.
     */
    public void testSetScheduledFuture(){
        ScheduledFuture future = null;
        purgeTask.setScheduledFuture(future);
        assertTrue(true);
    }
    @After
    public void teardown() {
        rpcRegistry = null;
    }

}
