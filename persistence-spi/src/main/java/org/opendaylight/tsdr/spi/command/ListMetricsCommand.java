/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.spi.command;

import java.io.PrintStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.opendaylight.tsdr.spi.command.completer.ListMetricsCommandCompleter;
import org.opendaylight.tsdr.spi.persistence.TSDRLogPersistenceService;
import org.opendaylight.tsdr.spi.persistence.TSDRMetricPersistenceService;
import org.opendaylight.tsdr.spi.util.FormatUtil;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This command is provided to get a list of metrics based on arguments passed.
 *
 * @author <a href="mailto:syedbahm@cisco.com">Basheeruddin Ahmed</a>
 */
@Service
@Command(scope = "tsdr", name = "list",
    description = "Lists recent 1000 metrics(default) or returns time specified metrics")
public class ListMetricsCommand implements Action {
    private static final Logger LOG = LoggerFactory.getLogger(ListMetricsCommand.class);

    @Argument(index = 0, name = "category", required = true,
            description = "The category of the metrics we want to get", multiValued = false)
    @Completion(ListMetricsCommandCompleter.class)
    public String category = null;
    @Argument(index = 1, name = "startDateTime", required = false,
            description = "list the metrics from this time (format: MM/dd/yyyy HH:mm:ss)", multiValued = false)
    public String startDateTime = null;
    @Argument(index = 2, name = "endDateTime", required = false,
            description = "list the metrics till this time (format: MM/dd/yyyy HH:mm:ss)", multiValued = false)
    public String endDateTime = null;

    @Reference TSDRMetricPersistenceService metricService;
    @Reference TSDRLogPersistenceService logService;
    @Reference protected Session session;

    public ListMetricsCommand() {

    }

    private long getDate(String dateTime) {
        if (dateTime == null) {
            return System.currentTimeMillis();
        }
        DateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US);
        Date date = null;
        try {
            date = format.parse(dateTime);
        } catch (ParseException e) {
            //Note we will log just a warning for this exception without stack trace
            // As this is expected in some cases
            LOG.warn("getDate for " + dateTime + "caused exception {}", e);
            return 0;
        }
        return date.getTime();
    }


    @Override
    @SuppressWarnings("checkstyle:RegexpSingleLineJava")
    public Object execute() throws Exception {
        PrintStream ps = this.session != null ? this.session.getConsole() : System.out;

        long startDate = 0;
        long endDate = Long.MAX_VALUE;
        if (startDateTime != null) {
            startDate = getDate(startDateTime);
            endDate = getDate(endDateTime);
        }

        if (startDate >= endDate) {
            ps.println("StatDateTime value cannot be greater or equal to EndDateTime");
            return null;
        }
        DataCategory dataCategory = DataCategory.valueOf(category);
        if ((dataCategory == DataCategory.NETFLOW || dataCategory == DataCategory.SYSLOG
                || dataCategory == DataCategory.LOGRECORDS || dataCategory == DataCategory.RESTCONF)
                && logService != null) {
            List<TSDRLogRecord> logs = logService.getTSDRLogRecords(category, startDate, endDate);
            if (logs == null || logs.isEmpty()) {
                ps.println("No data of this category in the specified time range. ");
                return null;
            }
            ps.println(listLogs(logs));

        } else if (metricService != null) {
            List<TSDRMetricRecord> metrics = metricService.getTSDRMetricRecords(category, startDate, endDate);
            if (metrics == null || metrics.isEmpty()) {
                ps.println("No data of this category in the specified time range. ");
                return null;
            }
            ps.println(listMetrics(metrics));
        }

        return null;
    }

    private String listMetrics(List<TSDRMetricRecord> metrics) {
        StringBuilder buffer = new StringBuilder();
        for (TSDRMetricRecord metric : metrics) {
            buffer.append(FormatUtil.getTSDRMetricKeyWithTimeStamp(metric));
            buffer.append("[").append(metric.getMetricValue()).append("]\n");
        }
        return buffer.toString();
    }

    private String listLogs(List<TSDRLogRecord> logs) {
        StringBuilder buffer = new StringBuilder();
        for (TSDRLogRecord log : logs) {
            buffer.append(FormatUtil.getTSDRLogKeyWithTimeStamp(log));
            buffer.append("[").append(log.getRecordFullText()).append("]\n");
        }
        return buffer.toString();
    }
}
