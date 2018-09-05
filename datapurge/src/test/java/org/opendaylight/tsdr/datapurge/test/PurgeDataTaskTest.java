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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.tsdr.datapurge.PurgeDataTask;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.PurgeAllTSDRRecordOutputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRService;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

/**
 * Test for the PurgeDataTask.
 *
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 */
public class PurgeDataTaskTest {
    private final RpcProviderRegistry rpcRegistry = Mockito.mock(RpcProviderRegistry.class);
    private final PurgeDataTask purgeTask = new PurgeDataTask(rpcRegistry);

    @Before
    public void setup() {
        TSDRService storageService = Mockito.mock(TSDRService.class);
        Mockito.when(rpcRegistry.getRpcService(TSDRService.class)).thenReturn(storageService);
        Mockito.when(storageService.purgeAllTSDRRecord(any()))
                .thenReturn(RpcResultBuilder.success(new PurgeAllTSDRRecordOutputBuilder().build()).buildFuture());
        purgeTask.setRetentionTimeinHours(1440);
    }

    /**
     * Test to see if purgeTask.purgeData() is called successfully.
     */
    @Test
    public void testPurgeData() {
        purgeTask.purgeData();
        assertTrue(true);
    }

    /**
     * Test the getRetentionTimeInHour() method.
     */
    @Test
    public void testGetRetentionTimeInHour() {
        assertTrue(purgeTask.getRetentionTimeinHours() == 1440);
    }

    /**
     * Test the setRetentionTimeInHour() method.
     */
    @Test
    public void testSetRetentionTimeInHour() {
        purgeTask.setRetentionTimeinHours(24);
        assertTrue(purgeTask.getRetentionTimeinHours() == 24);
    }

    /**
     * Test the run() method in PurgeDataTask.
     */
    @Test
    public void testRun() {
        purgeTask.run();
        assertTrue(Thread.currentThread().getName().contains("PurgeData"));
    }
}
