package org.opendaylight.tsdr.service;

import org.opendaylight.tsdr.entity.Metric;

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
     * <Note>Used maxResutls with  caution to not cause performance issue </Note>
     * @param maxResults
     */
    List<Metric> getAll(int maxResults);

    void close();

}
