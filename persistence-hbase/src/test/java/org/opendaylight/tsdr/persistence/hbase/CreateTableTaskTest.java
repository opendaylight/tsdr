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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.IOException;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.tsdr.spi.scheduler.SchedulerService;

/**
 * UNnit tests for CreateTableTask.
 *
 * @author <a href="mailto:chaudhry.usama@xflowresearch.com">Chaudhry Muhammad Usama</a>
 * @author Thomas Pantelis
 */
public class CreateTableTaskTest {
    private final SchedulerService mockSchedulerService = mock(SchedulerService.class);
    private final HBaseDataStore mockDataStore = mock(HBaseDataStore.class);
    private final CreateTableTask tableService = new CreateTableTask(mockDataStore,
            Arrays.asList("table1", "table2", "table3"), mockSchedulerService, 50);

    @Before
    public void setup() {
    }

    @Test
    public void testRun() throws IOException {
        tableService.start();
        verify(mockSchedulerService).scheduleTask(tableService);
        tableService.run();

        assertTrue(tableService.completionFuture().isDone());
        verify(mockDataStore).createTable("table1");
        verify(mockDataStore).createTable("table2");
        verify(mockDataStore).createTable("table3");
        verifyNoMoreInteractions(mockDataStore, mockSchedulerService);
    }

    @Test
    public void testRunWithException() throws IOException {
        doNothing().when(mockDataStore).createTable("table1");
        doThrow(new IOException("mock")).when(mockDataStore).createTable("table2");
        doNothing().when(mockDataStore).createTable("table3");

        tableService.run();

        assertFalse(tableService.completionFuture().isDone());
        verify(mockSchedulerService).scheduleTask(tableService, 50L);
        verify(mockDataStore).createTable("table1");
        verify(mockDataStore).createTable("table2");
        verify(mockDataStore).createTable("table3");
        verifyNoMoreInteractions(mockDataStore);

        reset(mockDataStore);
        tableService.run();

        assertTrue(tableService.completionFuture().isDone());
        verify(mockDataStore).createTable("table2");
        verifyNoMoreInteractions(mockDataStore, mockSchedulerService);
    }
}
