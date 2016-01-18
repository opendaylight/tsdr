/*
 * Copyright (c) 2016 xFlow Research Inc. and others.  All rights reserved
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.datastorage.persistence.hbase;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.tsdr.persistence.hbase.CreateTableTask;
import org.opendaylight.tsdr.persistence.hbase.HBaseDataStore;
import org.opendaylight.tsdr.persistence.hbase.HBaseDataStoreFactory;

import junit.framework.Assert;

import static org.mockito.Mockito.mock;

import java.util.concurrent.ScheduledFuture;

import static org.mockito.Matchers.any;


/**
 * @author <a href="mailto:chaudhry.usama@xflowresearch.com">Chaudhry Muhammad Usama</a>
 * Created by Chaudhry Usama on 1/6/16.
 */

public class CreateTableTaskTest {
    public CreateTableTask TableService = null;
    private ScheduledFuture future = null;

    @Before
    public void setup() {
        future = mock(ScheduledFuture.class);
        TableService = new CreateTableTask(){@Override public void runCreateTable (String tableName) throws Throwable{return;}};

   }
    @Test
    public void testCreateTables() {
        TableService.setScheduledFuture(future);
        Assert.assertNotNull(TableService.future);
        TableService.createTables();
        Assert.assertTrue(TableService.pendingTableNames.size()==0);

    }

    @Test
    public void testRunTask() {
        TableService.setScheduledFuture(future);
        Assert.assertNotNull(TableService.future);
        TableService.runTask();

    }

    @Test
    public void testException() {
        CreateTableTask TableService1 = null;
        TableService1 = new CreateTableTask(){@Override public void runCreateTable (String tableName) throws Throwable{throw new Exception();}};
        TableService1.setScheduledFuture(future);
        Assert.assertNotNull(TableService1.future);
        TableService1.createTables();
        Assert.assertTrue(TableService1.pendingTableNames.size()!=0);
    }

    @Test
    public void testSetScheduledFuture() {
        TableService.setScheduledFuture(null);
        Assert.assertNull(TableService.future);
    }

    @After
    public void teardown() {
        TableService = null;
    }

}