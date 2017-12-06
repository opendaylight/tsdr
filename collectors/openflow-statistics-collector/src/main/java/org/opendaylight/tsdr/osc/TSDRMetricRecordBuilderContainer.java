/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.osc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrmetricrecord.input.TSDRMetricRecordBuilder;

/**
 * This container aggregate several metrics builders that correlate to the same InstanceIdentifier. It has an array of
 * builders that their metric value is update by notification the class assums that the order of initially adding
 * the different metrics to the container is also kept during update so it the metrics were added as "A,B,C" to the
 * container the place in the array will be 0,1,2 so when updating metric A you need to update builder at 0 and so on.
 *
 * @author Sharon Aicler(saichler@gmail.com)
 */
public class TSDRMetricRecordBuilderContainer {
    // List of metric record builders
    @GuardedBy("builders")
    private final List<TSDRMetricRecordBuilder> builders = new ArrayList<>();

    // A set to make sure the same metric is not been added twice.
    @GuardedBy("builders")
    private final Set<String> metricNames = new HashSet<>();

    /*
     * Happens only once per metric path+type, a new builder is added to be the
     * cache for this metric type
     */
    public void addBuilder(TSDRMetricRecordBuilder builder) {
        synchronized (builders) {
            if (!metricNames.contains(builder.getMetricName())) {
                metricNames.add(builder.getMetricName());
                builders.add(builder);
            }
        }
    }

    // Return the array of builders for the update operations following notifications.
    public TSDRMetricRecordBuilder[] getBuilders() {
        synchronized (builders) {
            return builders.toArray(new TSDRMetricRecordBuilder[builders.size()]);
        }
    }
}
