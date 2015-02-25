/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence;

import java.util.List;

import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;


/**
 * This interface provides a list of APIs for accessing TSDR persistence data store.
 *
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 *
 * Created: Feb 24, 2015
 *
 */
public interface TSDRPersistenceService {

    /**
     * Store TSDRMetricRecord.
     * @param metricRecord
     */
    public void store(TSDRMetricRecord metricRecord);

    /**
     * Store a list of TSDRMetricRecord.
     * @param metricRecordList
    */
    public void store(List<TSDRMetricRecord> metricRecordList);


    /**
     * Close db connections
     */
    public void closeConnections();
}
