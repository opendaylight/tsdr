/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.spi.scheduler;

import com.google.common.util.concurrent.ListenableScheduledFuture;

/**
 * This interface provides scheduling service to schedule a Task either immediately or with a delay.
 *
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 */
public interface SchedulerService {
    ListenableScheduledFuture<?> scheduleTaskAtFixedRate(Task task, long initialDelay, long retryInterval);

    ListenableScheduledFuture<?> scheduleTask(Task task);

    ListenableScheduledFuture<?> scheduleTask(Task task, long delay);
}
