/*
 * Copyright (c) 2016 xFlow Research Inc. and others.  All rights reserved
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hbase;

import static org.mockito.Mockito.mock;

import java.util.concurrent.ScheduledFuture;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * UNnit tests for CreateTableTask.
 *
 * @author <a href="mailto:chaudhry.usama@xflowresearch.com">Chaudhry Muhammad Usama</a>
 */
public class CreateTableTaskTest {
    public CreateTableTask tableService;
    private ScheduledFuture<?> future;

    @Before
    public void setup() {
        future = mock(ScheduledFuture.class);
        tableService = new CreateTableTask() {
            @Override
            public void runCreateTable(String tableName) {
                return;
            }
        };

    }

    @Test
    public void testCreateTables() {
        tableService.setScheduledFuture(future);
        Assert.assertNotNull(tableService.future);
        tableService.createTables();
        Assert.assertTrue(tableService.pendingTableNames.size() == 0);

    }

    @Test
    public void testRunTask() {
        tableService.setScheduledFuture(future);
        Assert.assertNotNull(tableService.future);
        tableService.runTask();

    }

    @Test
    public void testException() {
        CreateTableTask tableService1 = new CreateTableTask() {
            @Override
            public void runCreateTable(String tableName) throws Exception {
                throw new Exception();
            }
        };
        tableService1.setScheduledFuture(future);
        Assert.assertNotNull(tableService1.future);
        tableService1.createTables();
        Assert.assertTrue(tableService1.pendingTableNames.size() != 0);
    }

    @Test
    public void testSetScheduledFuture() {
        tableService.setScheduledFuture(null);
        Assert.assertNull(tableService.future);
    }

    @After
    public void teardown() {
        tableService = null;
    }
}
