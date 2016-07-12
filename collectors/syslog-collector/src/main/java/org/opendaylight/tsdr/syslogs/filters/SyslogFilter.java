/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.syslogs.filters;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecord;

/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public interface SyslogFilter {
    //returns true if this syslog is compatible with this filter and this filter should be applied to it.
    public boolean match(String syslog);

    /**
     * if "match" then parse the syslog while filtering it.
     * If null is returned then the syslog is dropped.
     * Sometimes there is a syslog forwarder and this is its address. The originator address is in the syslog body, so we might want to filter according to that.
     * @param syslog The syslog plain text
     * @param forwarderAddress the datagram packet source address
     * @param originatorAddress the datagram packet source address
     * @return TSDRLogRecord
     */
    public TSDRLogRecord filterAndParseSyslog(String syslog,String forwarderAddress,String originatorAddress);
    //Determinate the persistence destination of the syslog
    //Currently we have only one, we might have multiple in the future.
    public int getPersistenceDestination(TSDRLogRecord logRecord);
}
