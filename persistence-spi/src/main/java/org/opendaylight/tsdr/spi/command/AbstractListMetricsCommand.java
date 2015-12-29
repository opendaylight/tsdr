/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.spi.command;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.tsdr.spi.persistence.TsdrPersistenceService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This command is provided to get a list of metrics based on arguments passed
 *
 * @author <a href="mailto:syedbahm@cisco.com">Basheeruddin Ahmed</a>
 *
 */
@Command(scope = "tsdr", name = "list", description = "Lists recent 1000 metrics(default) or returns time specified metrics")
public abstract class AbstractListMetricsCommand extends OsgiCommandSupport {
    private final Logger
            log = LoggerFactory.getLogger(AbstractListMetricsCommand.class);
    protected TsdrPersistenceService persistenceService;

    @Argument(index=0, name="category", required=true, description="The category of the metrics we want to get", multiValued=false)
    public String category = null;
    @Argument(index=1, name="startDateTime", required=false, description="list the metrics from this time (format: MM/dd/yyyy HH:mm:ss)", multiValued=false)
    public String startDateTime = null;
    @Argument(index=2, name="endDateTime", required=false, description="list the metrics till this time (format: MM/dd/yyyy HH:mm:ss)", multiValued=false)
    public String endDateTime = null;

    public void setPersistenceService(TsdrPersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }


    protected long getDate(String dateTime){
        if(dateTime == null){
            return System.currentTimeMillis();
        }
        DateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US);
        Date date = null;
        try {
            date = format.parse(dateTime);
        } catch (ParseException e) {
            //Note we will log just a warning for this exception without stack trace
            // As this is expected in some cases
            System.out.println("Time format is invalid and will be ignored.");
            log.warn("getDate for " + dateTime + "caused exception {}", e);
        }
        return date.getTime();
    }


    @Override
    protected Object doExecute() throws Exception {

        long startDate = 0;
        long endDate = Long.MAX_VALUE;
        if(startDateTime!=null) {
            startDate = getDate(startDateTime);
            endDate = getDate(endDateTime);
        }

        if (startDate >= endDate) {
            System.out.println("StatDateTime value cannot be greater or equal to EndDateTime");
            return null;
        }
        DataCategory dataCategory = DataCategory.valueOf(category);
        if (persistenceService != null) {
            if(dataCategory== DataCategory.NETFLOW || dataCategory==DataCategory.SYSLOG || dataCategory==DataCategory.LOGRECORDS){
                List<TSDRLogRecord> logs = persistenceService.getTSDRLogRecords(category, startDate, endDate);
                if (logs == null || logs.isEmpty()) {
                    System.out.println("No data of this category in the specified time range. ");
                    return null;
                }
                System.out.println(listLogs(logs));

            }else {
                List<TSDRMetricRecord> metrics = persistenceService.getTSDRMetricRecords(category, startDate, endDate);
                if (metrics == null || metrics.isEmpty()) {
                    System.out.println("No data of this category in the specified time range. ");
                    return null;
                }
                System.out.println(listMetrics(metrics));
            }
        } else {
            log.warn("ListMetricsCommand: persistence service is found to be null.");
        }
        return null;
    }

    abstract protected String listMetrics (List<TSDRMetricRecord> metrics);
    abstract protected String listLogs(List<TSDRLogRecord> logs);
}
