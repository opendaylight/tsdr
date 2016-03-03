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

import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.AggregationType;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.gettsdrmetrics.output.Metrics;

/**
 * Used to reduce, or aggregate a list of metrics to a single value.
 *
 * @author <a href="mailto:jesse@opennms.org">Jesse White</a>
 */
public interface AggregationFunction {

    /**
     * Gets the type of aggregation function.
     *
     * @return type
     */
    AggregationType getType();

    /**
     * Aggregates the list of metrics down to a single value.
     *
     * @param metrics list of metrics to aggregate
     * @return aggregated value, or null if NaN
     */
    BigDecimal aggregate(List<Metrics> metrics);
}
