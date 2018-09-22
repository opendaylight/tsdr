/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.syslogs.filters;

/**
 * A syslog filter.
 *
 * @author Sharon Aicler(saichler@gmail.com)
 */
@FunctionalInterface
public interface SyslogFilter {
    /**
     * Returns true if the given syslog parameters are compatible with this filter.
     *
     * @param syslog The syslog plain text
     * @param packetSourceAddress the syslog's packet source address
     * @param originatorAddress the syslog's originator address
     */
    boolean matches(String syslog, String packetSourceAddress, String originatorAddress);
}
