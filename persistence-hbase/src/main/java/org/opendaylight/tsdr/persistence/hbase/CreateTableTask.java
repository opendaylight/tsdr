/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hbase;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import org.opendaylight.tsdr.spi.scheduler.SchedulerService;
import org.opendaylight.tsdr.spi.scheduler.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a task that create HBase tables during the initialization
 * time of HBase data store. It extends the TSDR Task, which is schedulable
 * by TSDR Scheduler.
 *
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 * @author Thomas Pantelis
 */
class CreateTableTask extends Task {
    private static final Logger LOG = LoggerFactory.getLogger(CreateTableTask.class);

    private final List<String> pendingTableNames;
    private final HBaseDataStore dataStore;
    private final SchedulerService schedulerService;
    private final long retryInterval;
    private final SettableFuture<?> completionFuture = SettableFuture.create();

    CreateTableTask(HBaseDataStore dataStore, List<String> pendingTableNames, SchedulerService schedulerService,
            long retryInterval) {
        this.dataStore = dataStore;
        this.pendingTableNames = new ArrayList<>(pendingTableNames);
        this.schedulerService = schedulerService;
        this.retryInterval = retryInterval;
    }

    CreateTableTask start() {
        schedulerService.scheduleTask(this);
        return this;
    }

    FluentFuture<?> completionFuture() {
        return completionFuture;
    }

    @Override
    public void runTask() {
        String oldName = Thread.currentThread().getName();
        Thread.currentThread().setName("TSDR HBase Data Store CreateTableTask");

        try {
            createTables();
        } finally {
            Thread.currentThread().setName(oldName);
        }
    }

    private void createTables() {
        LOG.debug("Entering createTables()");
        Iterator<String> tableNameIter = pendingTableNames.iterator();
        while (tableNameIter.hasNext()) {
            String tableName = tableNameIter.next();
            try {
                dataStore.createTable(tableName);
                tableNameIter.remove();
            } catch (IOException t) {
                LOG.error("Error creating table {}", tableName, t);
            }
        }

        LOG.info("Exiting createTables() - pending tables count:" + pendingTableNames.size());
        if (pendingTableNames.isEmpty()) {
            completionFuture.set(null);
        } else {
            schedulerService.scheduleTask(this, retryInterval);
        }
    }

    @Override
    public void setScheduledFuture(ScheduledFuture scheduledFuture) {
    }
}
