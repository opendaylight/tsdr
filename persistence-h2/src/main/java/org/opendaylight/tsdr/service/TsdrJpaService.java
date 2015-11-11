/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.service;

import org.opendaylight.tsdr.entity.Metric;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;

import java.util.Date;
import java.util.List;

/**
 * Defines the supported operations on the JPA store
 *
 * @author <a href="mailto:syedbahm@cisco.com">Basheeruddin Ahmed</a>
 */
public interface TsdrJpaService {
    /**
     * Adds a metric to store
     * @param metric
     */
    void add(Metric metric);

    /**
     * Deletes all  metrics in store
     * <p>
     * Used currently for debugging purpose only
     * </p>
     */
    void deleteAll();

    /**
     * Gets the specified number of recent metrics from store
     *
     * if maxResults is not specified defaults to 1000
     * <p>
     * Note: Used maxResults with  caution to not cause performance issue
     * </p>
     * @param maxResults
     */
    List<Metric> getMetricsFilteredByCategory(String category,int maxResults);


    /**
     *  Gets the specified number of metrics from store filtered by startDate and endDate
     * for a specific metric category
     *
     * @param category  -- the category of metrics to filter upon
     * @param startDateTime -- indicates from this time
     * @param endDateTime  --- indicate to this time
     * @return List&lt;Metric&gt; of metrics if found else empty list
     */


    List<Metric> getMetricsFilteredByCategory(String category,Date startDateTime,Date endDateTime );

    void close();

    /**
     * Purge the metrics older than the retentions time
     * of a particular category from store
     * @param category -- which category to purge
     * @param retentionTime - epoch time indicating older than this time records should be purged
     */
    void purge(DataCategory category, long retentionTime);

    /**
     * Purge the metrics older than the retentions time under all categories
     * @param retentionTime - epoch time indicating older than this time records should be purged
     */
    void purgeAll(long retentionTime);
}
