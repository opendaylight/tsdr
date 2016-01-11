/*
 * Copyright (c) 2016 xFlow Research Inc. and others.  All rights reserved
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.datastorage.persistence.hbase;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:chaudhry.usama@xflowresearch.com">Chaudhry Muhammad Usama</a>
 * Created by Chaudhry Usama on 1/6/16.
 */
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.tsdr.persistence.hbase.HBaseDataStoreContext;
/**
 * @author <a href="mailto:chaudhry.usama@xflowresearch.com">Chaudhry Muhammad Usama</a>
 * Created by Chaudhry Usama on 1/6/16.
 */
public class HBaseDataStoreContextTest {
    public HBaseDataStoreContext ContextService = null;

    @Before
    public void setUp() {
        ContextService = new HBaseDataStoreContext();
    }

    @Test
    public void testGetZookeeperQuorum() {
        boolean result = false;
        System.out.println(ContextService.getZookeeperQuorum());
        result = ContextService.getZookeeperQuorum() == "localhost";
        assertTrue(result);
    }

    @Test
    public void testGetZookeeperClientport() {
        boolean result = false;
        System.out.println(ContextService.getZookeeperClientport());
        result = ContextService.getZookeeperClientport() == "2181";
        assertTrue(result);
    }

    @Test
    public void testGetPoolSize() {
        boolean result = false;
        System.out.println(ContextService.getPoolSize());
        result = ContextService.getPoolSize() == 5;
        assertTrue(result);
    }

    @Test
    public void testGetWriteBufferSize() {
        boolean result = false;
        System.out.println(ContextService.getWriteBufferSize());
        result = ContextService.getWriteBufferSize() == 512;
        assertTrue(result);
    }

    @Test
    public void testGetAutoFlush() {
        boolean result = false;
        System.out.println(ContextService.getAutoFlush());
        result = ContextService.getAutoFlush() == false;
        assertTrue(result);
    }

    @Test
    public void testGetPropertyInLong() {
        boolean result = false;
        System.out.println(HBaseDataStoreContext.getPropertyInLong(null));
        result = HBaseDataStoreContext.getPropertyInLong(null) == null;
        assertTrue(result);
    }

    @After
    public void tearDown() {
        ContextService = null;
    }

}
