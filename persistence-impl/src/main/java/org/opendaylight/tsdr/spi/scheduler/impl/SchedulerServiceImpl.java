/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.spi.scheduler.impl;

import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.tsdr.spi.scheduler.SchedulerService;
import org.opendaylight.yangtools.util.concurrent.ThreadFactoryProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of SchedulerService backed by a ScheduledExecutorService.
 *
 * @author Thomas Pantelis
 */
@Singleton
public class SchedulerServiceImpl implements SchedulerService {
    private static final Logger LOG = LoggerFactory.getLogger(SchedulerServiceImpl.class);

    private static final int THREADS_COUNT = 10;

    private final ListeningScheduledExecutorService scheduleExecutor;

    @Inject
    public SchedulerServiceImpl() {
        ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(THREADS_COUNT,
                ThreadFactoryProvider.builder().namePrefix("TSDR-Scheduler").logger(LOG).build().get());
        scheduler.setKeepAliveTime(1, TimeUnit.MINUTES);
        scheduler.allowCoreThreadTimeOut(true);
        scheduleExecutor = MoreExecutors.listeningDecorator(scheduler);
        LOG.debug("Scheduler Service created the thread pool with {} threads" , THREADS_COUNT);
    }

    @PreDestroy
    public void close() {
        scheduleExecutor.shutdownNow();
    }

    @Override
    public ListenableScheduledFuture<?> scheduleTaskAtFixedRate(Runnable task, long initialDelay, long retryInterval) {
        return scheduleExecutor.scheduleAtFixedRate(task, initialDelay, retryInterval, TimeUnit.MILLISECONDS);
    }

    @Override
    public ListenableScheduledFuture<?> scheduleTask(Runnable task) {
        return scheduleTask(task, 0L);
    }

    @Override
    public ListenableScheduledFuture<?> scheduleTask(Runnable task, long delay) {
        return scheduleExecutor.schedule(task, delay, TimeUnit.MILLISECONDS);
    }
}
