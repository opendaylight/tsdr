/*
 * Copyright (c) 2016 Cisco Systems,  Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.spi.persistence;

import java.util.List;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.binary.data.rev160325.storetsdrbinaryrecord.input.TSDRBinaryRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;

/**
 * This interface provides a list of APIs for accessing TSDR Metric persistence data store.
 * Created by saichler@gmail.com on 3/25/16.
 * @author <a href="mailto:saichler@gmail.com">Sharon Aicler</a>
 */
public interface TSDRBinaryPersistenceService {
    /**
     * Store TSDRBinaryRecord.
     * @param binaryRecord - an instane of tsdr log record
     */
    void storeBinary(TSDRBinaryRecord binaryRecord);

    /**
     * Store a list of TSDRRecord.
     * @param recordList - a list of tsdrRecord
     */
    void storeBinary(List<TSDRBinaryRecord> recordList);

    /**
     * Returns the TSDRLogRecords based on category, startTime, and endTime.
     *
     * @param tsdrBinaryKey - The tsdr log key, can be also just Data Category
     * @param startTime - The starting time
     * @param endTime - The end time
     * @return - A list of log records
     */
    List<TSDRBinaryRecord> getTSDRBinaryRecords(String tsdrBinaryKey, long startTime, long endTime);

    /**
     * Purges all the data from TSDR data store older than the retention timestamp.
     *
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
