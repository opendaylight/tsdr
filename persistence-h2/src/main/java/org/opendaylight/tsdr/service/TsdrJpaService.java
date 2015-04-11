package org.opendaylight.tsdr.service;

import org.opendaylight.tsdr.entity.Metric;

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
     * <Note>Used currently for debugging purpose only</Note>
     */
    void deleteAll();

    /**
     * Gets the specified number of recent metrics from store
     *
     * if maxResults is not specified defaults to 1000
     *
     * <Note>Used maxResults with  caution to not cause performance issue </Note>
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
     * @return List<Metric> of metrics if found else empty list
     */


    List<Metric> getMetricsFilteredByCategory(String category,Date startDateTime,Date endDateTime );

    void close();

}
