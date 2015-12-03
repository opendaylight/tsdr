/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.command;

import org.apache.karaf.shell.commands.Command;
import org.opendaylight.tsdr.spi.command.AbstractListMetricsCommand;
import org.opendaylight.tsdr.spi.util.FormatUtil;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;

import java.util.List;

/**
 *
 * @author <a href="mailto:syedbahm@cisco.com">Basheeruddin Ahmed</a>
 *
 */
@Command(scope = "tsdr", name = "list", description = "Lists recent 1000 metrics(default) or returns time specified metrics")
public class ListMetricsCommand extends AbstractListMetricsCommand {
    /**
     * Format and print out the result of the metrics on Karaf console.
     */
    @Override
    protected String listMetrics(List<TSDRMetricRecord> metrics) {
        StringBuilder buffer = new StringBuilder();
        for (TSDRMetricRecord metric : metrics) {
            buffer.append(FormatUtil.getTSDRMetricKeyWithTimeStamp(metric));
            buffer.append("[").append(metric.getMetricValue()).append("]\n");
        }
        return buffer.toString();
    }
}
