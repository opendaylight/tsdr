/*
 * Copyright (c) 2016 The OpenNMS Group Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.datastorage.aggregate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.AggregationType;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.gettsdrmetrics.output.Metrics;

/**
 * Calculates the mean, or average, for a list of metrics.
 *
 * @author <a href="mailto:jesse@opennms.org">Jesse White</a>
 */
public class Mean implements AggregationFunction {

    @Override
    public AggregationType getType() {
        return AggregationType.MEAN;
    }

    @Override
    public BigDecimal aggregate(List<Metrics> metrics) {
        if (metrics.size() == 0) {
            // BigDecimal has no equivalent notion of Double.NaN, so we use null
            return null;
        }

        BigDecimal sum = BigDecimal.ZERO;
        for (Metrics metric : metrics) {
            sum = sum.add(metric.getMetricValue());
        }

        return sum.divide(BigDecimal.valueOf(metrics.size()), RoundingMode.HALF_EVEN);
    }
}
