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
import org.opendaylight.tsdr.service.TsdrJpaService;
import org.opendaylight.tsdr.spi.command.AbstractListMetricsCommand;
import org.opendaylight.tsdr.spi.model.TSDRConstants;
import org.opendaylight.tsdr.spi.util.FormatUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 *
 * @author <a href="mailto:syedbahm@cisco.com">Basheeruddin Ahmed</a>
 *
 */
@Command(scope = "tsdr", name = "list", description = "Lists recent 1000 metrics(default) or returns time specified metrics")
public class ListMetricsCommand extends AbstractListMetricsCommand {
    private final Logger
        log = LoggerFactory.getLogger(ListMetricsCommand.class);

    public static String getFixedFormatString(String value,long length){
        return String.format("%1$"+length+"s",value);
    }
    @Override
    protected String listMetrics(List<?> metrics) {
        List<Metric> metricList = (List<Metric>) metrics;
        StringBuffer buffer = null;
        buffer = new StringBuffer();

        for (Metric metric : metricList) {

                   //.append(getFixedFormatString(String.valueOf(metric.getId()),15))
                    buffer.append("TimeStamp = ")
                    .append(FormatUtil.getFormattedTimeStamp(metric.getMetricTimeStamp().getTime(),
                            FormatUtil.COMMAND_OUT_TIMESTAMP))
                    .append("|MetricName = ")
                    .append(metric.getMetricName())
                    .append("|MetricValue = ")
                    .append(metric.getMetricValue())
                    .append("|MetricCategory = ")
                    .append(metric.getMetricCategory())
                    .append("|MetricDetails = ")
                    .append(metric.getMetricDetails())
                    .append("\n");

        }

        return buffer.toString();
    }
}
