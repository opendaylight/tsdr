/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.spi.scheduler;

import java.util.concurrent.ScheduledFuture;

/**
 * This class is the abstract class for all tasks tha can be scheduled
 * by the SchedulerService.
 *
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 */
public abstract class Task implements Runnable {

    @Override
    public void run() {
        runTask();
    }

    public abstract void runTask();

    public abstract void setScheduledFuture(ScheduledFuture scheduledFuture);

}
