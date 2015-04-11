/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.command;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.tsdr.entity.Metric;
import org.opendaylight.tsdr.model.TSDRConstants;
import org.opendaylight.tsdr.service.TsdrJpaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 *
 * @author <a href="mailto:syedbahm@cisco.com">Basheeruddin Ahmed</a>
 *
 */

public class ListMetricsCommand extends AbstractListMetricsCommand {
    private final Logger
        log = LoggerFactory.getLogger(ListMetricsCommand.class);


    @Override
    protected String listMetrics(List<?> metrics) {
        List<Metric> metricList = (List<Metric>) metrics;
        StringBuffer buffer = null;
        buffer = new StringBuffer();

        //TODO: MetricID=<MetricID>|ObjectKeys =<concatenated_object_keys>|TimeStamp = <timestamp>|MetricValue = <MetricValue
        for (Metric metric : metricList) {

            buffer.append(metric.getId())
                    .append(" | ")
                    .append(metric.getMetricName())
                    .append(" | ")
                    .append(metric.getMetricValue())
                    .append(" | ")
                    .append(metric.getMetricTimeStamp())
                    .append(" | ")
                    .append(metric.getNodeId())
                    .append(" | ")
                    .append(metric.getMetricCategory())
                    .append(" | ")
                    .append(metric.getInfo())
                    .append("\n");

        }

        return buffer.toString();
    }
}
