/*
 * Copyright (c) 2016 The OpenNMS Group Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.datastorage.aggregate;

import java.math.BigDecimal;
import java.util.List;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.AggregationType;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.gettsdrmetrics.output.Metrics;

/**
 * Calculates the max for a list of metrics.
 *
 * @author <a href="mailto:jesse@opennms.org">Jesse White</a>
 */
public class Max implements AggregationFunction {

    @Override
    public AggregationType getType() {
        return AggregationType.MAX;
    }

    @Override
    public BigDecimal aggregate(List<Metrics> metrics) {
        BigDecimal max  = null;
        for (Metrics metric : metrics) {
            if (max == null || max.compareTo(metric.getMetricValue()) < 0) {
                max = metric.getMetricValue();
            }
        }
        return max;
    }
}
