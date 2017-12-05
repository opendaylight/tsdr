/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.spi.scheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides scheduling service to schedule a Task either
 * immediately or with a delay.
 *
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 */
public class SchedulerService {
    private static final Logger LOG = LoggerFactory.getLogger(SchedulerService.class);

    private static final int THREADS_COUNT = 10;
    private static SchedulerService schedulerService;

    private final ScheduledExecutorService scheduler;

    private SchedulerService() {
        scheduler = Executors.newScheduledThreadPool(THREADS_COUNT);
        LOG.debug("Scheduler Service created the Thread Pool with Threads {}\n" , THREADS_COUNT);
    }

    public static synchronized SchedulerService getInstance() {
        if (schedulerService == null) {
            schedulerService = new SchedulerService();
        }
        return schedulerService;
    }

    public ScheduledFuture<?> scheduleTaskAtFixedRate(Task task, long initialDelay, long retryInterval) {
        final ScheduledFuture<?> scheduledFuture = scheduler.scheduleAtFixedRate(task, initialDelay, retryInterval,
                TimeUnit.SECONDS);
        task.setScheduledFuture(scheduledFuture);
        return scheduledFuture;
    }

    public ScheduledFuture<?> scheduleTask(Task task) {
        return scheduleTask(task, 0L);
    }

    public ScheduledFuture<?> scheduleTask(Task task, long delay) {
        final ScheduledFuture<?> scheduledFuture = scheduler.schedule(task, delay, TimeUnit.SECONDS);
        task.setScheduledFuture(scheduledFuture);
        return scheduledFuture;
    }
}
