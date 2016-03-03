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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.AggregationType;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.gettsdrmetrics.output.Metrics;

/**
 * Calculates the median for a list of metrics.
 *
 * @author <a href="mailto:jesse@opennms.org">Jesse White</a>
 */
public class Median implements AggregationFunction {

    @Override
    public AggregationType getType() {
        return AggregationType.MEDIAN;
    }

    @Override
    public BigDecimal aggregate(List<Metrics> metrics) {
        if (metrics.size() == 0) {
            // BigDecimal has no equivalent notion of Double.NaN, so we use null
            return null;
        }

        // Sort the list of metrics in ascending order
        Collections.sort(metrics, new Comparator<Metrics>() {
            @Override
            public int compare(Metrics m1, Metrics m2) {
                return m1.getMetricValue().compareTo(m2.getMetricValue());
            }
        });

        final int middle = Math.floorDiv(metrics.size(), 2);
        if (metrics.size() % 2 == 0) {
            final BigDecimal left = metrics.get(middle - 1).getMetricValue();
            final BigDecimal right = metrics.get(middle).getMetricValue();
            return left.add(right).divide(BigDecimal.valueOf(2), RoundingMode.HALF_EVEN);
        } else {
            return metrics.get(middle).getMetricValue();
        }
    }
}
