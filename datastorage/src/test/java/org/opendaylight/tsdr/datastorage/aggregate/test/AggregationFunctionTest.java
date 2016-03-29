/*
 * Copyright (c) 2016 The OpenNMS Group Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.datastorage.aggregate.test;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.Test;
import org.opendaylight.tsdr.datastorage.aggregate.*;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.AggregationType;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.gettsdrmetrics.output.Metrics;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.gettsdrmetrics.output.MetricsBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:jesse@opennms.org">Jesse White</a>
 */
public class AggregationFunctionTest {
    private static final double DELTA = 0.00001;

    private static ServiceLoader<AggregationFunction> aggregationFunctions = ServiceLoader
            .load(AggregationFunction.class);

    @Test
    public void canFindSingleImplementationForEveryType() {
        Map<AggregationType, Integer> numberOfImplementationsByType = Maps.newHashMap();
        for (AggregationFunction function : aggregationFunctions) {
            Integer numberOfImplementations = numberOfImplementationsByType.get(function.getType());
            if (numberOfImplementations == null) {
                numberOfImplementations = 0;
            }
            numberOfImplementationsByType.put(function.getType(), numberOfImplementations + 1);
        }

        for (AggregationType type : AggregationType.values()) {
            assertEquals("No unique type for found: " + type,
                    Integer.valueOf(1), numberOfImplementationsByType.get(type));
        }
    }

    @Test
    public void canCalculateMax() {
        Max max = new Max();
        assertEquals(null, max.aggregate(getMetricsWithValues()));
        assertEquals(1, max.aggregate(getMetricsWithValues(1)).doubleValue(), DELTA);
        assertEquals(8, max.aggregate(getMetricsWithValues(2,4,6,8)).doubleValue(), DELTA);
    }

    @Test
    public void canCalculateMean() {
        Mean mean = new Mean();
        assertEquals(null, mean.aggregate(getMetricsWithValues()));
        assertEquals(1, mean.aggregate(getMetricsWithValues(1)).doubleValue(), DELTA);
        assertEquals(5, mean.aggregate(getMetricsWithValues(2,4,6,8)).doubleValue(), DELTA);
    }

    @Test
    public void canCalculateMedian() {
        Median median = new Median();
        assertEquals(null, median.aggregate(getMetricsWithValues()));
        assertEquals(1, median.aggregate(getMetricsWithValues(1)).doubleValue(), DELTA);
        // Even number of metrics, the average of the two middle values should be returned
        assertEquals(5, median.aggregate(getMetricsWithValues(2,4,6,8)).doubleValue(), DELTA);
        // Odd number of metrics, the middle one should be returned
        assertEquals(6, median.aggregate(getMetricsWithValues(2,4,6,8,100)).doubleValue(), DELTA);
    }

    @Test
    public void canCalculateMin() {
        Min min = new Min();
        assertEquals(null, min.aggregate(getMetricsWithValues()));
        assertEquals(1, min.aggregate(getMetricsWithValues(1)).doubleValue(), DELTA);
        assertEquals(2, min.aggregate(getMetricsWithValues(2,4,6,8)).doubleValue(), DELTA);
    }

    private static List<Metrics> getMetricsWithValues(double... values) {
        final List<Metrics> metrics = Lists.newArrayList();
        for (double value : values) {
            metrics.add(new MetricsBuilder()
                    .setMetricValue(BigDecimal.valueOf(value)).build());
        }
        return metrics;
    }
}
