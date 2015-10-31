/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 * Copyright (c) 2015 Cisco Systems,  Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.spi.persistence;


import java.util.Date;
import java.util.List;

import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrlogrecord.input.TSDRLogRecord;

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
     * @param metricRecord
     */
    void store(TSDRMetricRecord metricRecord);

    /**
     * Store TSDRMetricRecord.
     * @param logRecord
     */
    void store(TSDRLogRecord logRecord);

    /**
     * Store a list of TSDRRecord.
     * @param recordList
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
     * @param metricsCategory -- this is required value for category
     * @param startDateTime  -- can be null else will be the startDateTime
     * @param endDateTime   -- can be null else will be the endDateTime
     * @return List of persistence store dependents records
     */
    List<?> getMetrics(String metricsCategory,Date startDateTime, Date endDateTime);

    /**
     * Returns the TSDRRecords based on category, startTime, and endTime
     * @param category
     * @param startTime
     * @param endTime
     * @return
     */
    List<?> getTSDRMetrics(DataCategory category, Long startTime, Long endTime);
    /**
     * Purges the data from TSDR data store.
     * @param category -- the category of the data.
     * @param timestamp -- the retention time.
     */
    void purgeTSDRRecords(DataCategory category, Long timestamp);
    /**
     * Purges all the data from TSDR data store.
     * @param timestamp
     */
    void purgeAllTSDRRecords(Long timestamp);
}
