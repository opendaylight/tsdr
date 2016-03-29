/*
 * Copyright (c) 2016 The OpenNMS Group Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.datastorage.aggregate;

import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.AggregationType;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.gettsdrmetrics.output.Metrics;

import java.math.BigDecimal;
import java.util.List;

/**
 * Calculates the min for a list of metrics.
 *
 * @author <a href="mailto:jesse@opennms.org">Jesse White</a>
 */
public class Min implements AggregationFunction {

    @Override
    public AggregationType getType() {
        return AggregationType.MIN;
    }

    @Override
    public BigDecimal aggregate(List<Metrics> metrics) {
        BigDecimal min  = null;
        for (Metrics metric : metrics) {
            if (min == null || min.compareTo(metric.getMetricValue()) > 0) {
                min = metric.getMetricValue();
            }
        }
        return min;
    }
}
