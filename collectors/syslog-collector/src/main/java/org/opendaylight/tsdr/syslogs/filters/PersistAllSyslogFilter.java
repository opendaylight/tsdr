/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.syslogs.filters;

import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecordBuilder;

/**
 * Implementation of SyslogFilter that persists.
 *
 * @author Sharon Aicler(saichler@gmail.com)
 */
public class PersistAllSyslogFilter implements SyslogFilter {
    private int index = 0;

    @Override
    public boolean match(String syslog) {
        return true;
    }

    @Override
    public TSDRLogRecord filterAndParseSyslog(String syslog, String forwearderAddress,String originatorAddress) {
        TSDRLogRecordBuilder record = new TSDRLogRecordBuilder();
        record.setNodeID(originatorAddress);
        record.setRecordFullText(syslog);
        record.setTSDRDataCategory(DataCategory.SYSLOG);
        record.setTimeStamp(System.currentTimeMillis());
        record.setIndex(index++);
        return record.build();
    }

    @Override
    public int getPersistenceDestination(TSDRLogRecord logRecord) {
        return 1;
    }
}
