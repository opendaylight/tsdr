/*
 * Copyright (c) 2016 Cisco Systems,  Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.spi.persistence;

import java.util.List;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;

/**
 * This interface provides a list of APIs for accessing TSDR Metric persistence data store.
 * Created by saichler@gmail.com on 3/25/16.
 * @author <a href="mailto:saichler@gmail.com">Sharon Aicler</a>
 */
public interface TSDRMetricPersistenceService {
    /**
     * Store TSDRMetricRecord.
     * @param metricRecord - An instance of tsdr metric record
     */
    void storeMetric(TSDRMetricRecord metricRecord);

    /**
     * Store a list of TSDRMetricRecord.
     * @param recordList - a list of tsdr metric Records
     */
    void storeMetric(List<TSDRMetricRecord> recordList);

    /**
     * Returns the list of metrics based on startDateTime and endDateTime
     * If startDateTime OR(/AND)  endDateTime is not specified returns the recent
     * predefined N metrics
     *
     * @param tsdrMetricKey -- The tsdr metric key, can also be just Data Category,
     * @param startDateTime  --The start time in milis
     * @param endDateTime   -- The end time in milis
     * @return - List of persistence store dependents records
     */
    //format of tsdrMetricKey is "[NID=<node id>][DC=<data category>][MN=<metric name>][RK=<a list or record keys>][TS=<timestamp - for hbase>]"
    List<TSDRMetricRecord> getTSDRMetricRecords(String tsdrMetricKey,long startDateTime, long endDateTime);

    /**
     * Purges all the data from TSDR data store older than the
     * retention timestamp
     * @param timestamp - The time stamp
     */
    void purge(long timestamp);

    /**
     * Purges the data from TSDR data store.
     * @param category -- the category of the data.
     * @param timestamp -- the retention time.
     */
    void purge(DataCategory category, long timestamp);
}
