/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.syslogs.filters;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opendaylight.tsdr.syslogs.server.decoder.Message;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecordBuilder;

/**
 * Manages syslog filters.
 *
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class SyslogFilterManager {
    private static final SyslogFilter WILDCARD_FILTER = (syslog, packetSourceAddress, originatorAddress) -> true;

    private final AtomicInteger index = new AtomicInteger();
    private final Map<String, SyslogFilter> filters = ImmutableMap.of();

    @Nullable
    public TSDRLogRecord applyFilters(@Nonnull Message message) {
        String syslogString = message.getContent().trim();
        String packetHostAddress = message.getHostname();

        String syslogOriginator = packetHostAddress;
        int index1 = syslogString.indexOf("Original Address");
        if (index1 != -1) {
            int index2 = syslogString.indexOf("=", index1);
            if (index2 != -1)  {
                int index3 = syslogString.indexOf(" ", index2 + 2);
                if (index3 != -1) {
                    syslogOriginator = syslogString.substring(index2 + 1, index3).trim();
                }
            }
        }

        SyslogFilter filter = filters.getOrDefault(syslogOriginator, WILDCARD_FILTER);

        if (filter.matches(syslogString, packetHostAddress, syslogOriginator)) {
            return new TSDRLogRecordBuilder().setNodeID(syslogOriginator).setRecordFullText(syslogString)
                .setTSDRDataCategory(DataCategory.SYSLOG).setTimeStamp(System.currentTimeMillis())
                .setIndex(index.getAndIncrement()).build();
        }

        return null;
    }
}
