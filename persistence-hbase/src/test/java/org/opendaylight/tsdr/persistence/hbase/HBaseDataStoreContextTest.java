/*
 * Copyright (c) 2016 xFlow Research Inc. and others.  All rights reserved
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hbase;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for HBaseDataStoreContext.
 *
 * @author <a href="mailto:chaudhry.usama@xflowresearch.com">Chaudhry Muhammad Usama</a>
 */
public class HBaseDataStoreContextTest {
    private final HBaseDataStoreContext contextService = new HBaseDataStoreContext();

    @Test
    public void testGetZookeeperQuorum() {
        assertTrue("localhost".equals(contextService.getZookeeperQuorum()));
    }

    @Test
    public void testGetZookeeperClientport() {
        assertTrue("2181".equals(contextService.getZookeeperClientport()));
    }

    @Test
    public void testGetPoolSize() {
        assertTrue(contextService.getPoolSize() == 5);
    }

    @Test
    public void testGetWriteBufferSize() {
        assertTrue(contextService.getWriteBufferSize() == 512);
    }

    @Test
    public void testGetAutoFlush() {
        assertFalse(contextService.getAutoFlush());
    }

    @Test
    public void testGetPropertyInLong() {
        assertTrue(HBaseDataStoreContext.getPropertyInLong(null) == null);
    }
}
