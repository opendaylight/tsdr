/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 * Copyright (c) 2015 Cisco Systems,  Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.spi.persistence;


import java.util.List;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;

/**
 * This interface provides a list of APIs for accessing TSDR persistence data store.
 *
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 *
 * Created: Feb 24, 2015
 *
 * Modified: Mar 18, 2015
 *
 * @author <a href="mailto:syedbahm@cisco.com">Basheeruddin Ahmed</a>
 *
 * Modified: Oct 7, 2015
 *
 * @author <a href="mailto:saichler@cisco.com">Sharon Aicler</a>
 *
 */
public interface TsdrPersistenceService {

    /**
     * Store TSDRMetricRecord.
     * @param metricRecord - An instance of tsdr metric record
     */
    void store(TSDRMetricRecord metricRecord);

    /**
     * Store TSDRMetricRecord.
     * @param logRecord - an instane of tsdr log record
     */
    void store(TSDRLogRecord logRecord);

    /**
     * Store a list of TSDRRecord.
     * @param recordList - a list of tsdrRecord
     */
    void store(List<TSDRRecord> recordList);

    /**
     * Starts the persistence service
     *
     * @param timeout -- indicates the time given for the starting of persistence,
     * after which the caller will consider it non-funcational
     */
    void start(int timeout);

    /**
     * Stops the persistence service
     *
     * @param timeout -- indicates the time given for the stopping of persistence,
     * after which the caller will assume its stopped
     */
    void stop(int timeout);

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
     * Returns the TSDRLogRecords based on category, startTime, and endTime
     * @param tsdrLogKey - The tsdr log key, can be also just Data Category
     * @param startTime - The starting time
     * @param endTime - The end time
     * @return - A list of log records
     */
    List<TSDRLogRecord> getTSDRLogRecords(String tsdrLogKey, long startTime, long endTime);

    /**
     * Purges the data from TSDR data store.
     * @param category -- the category of the data.
     * @param timestamp -- the retention time.
     */

    //TODO: change from Long to long if there is no specific reason for using Long.
    //TODO:change name of method to purge
    void purgeTSDRRecords(DataCategory category, Long timestamp);

    /**
     * Purges all the data from TSDR data store older than the
     * retention timestamp
     * @param timestamp - The time stamp
     */
    //TODO:change name to purgeAll and Long to long
    void purgeAllTSDRRecords(Long timestamp);
}
