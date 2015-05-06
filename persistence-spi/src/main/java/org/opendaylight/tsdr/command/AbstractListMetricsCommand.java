/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.command;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.tsdr.persistence.spi.TsdrPersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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


    protected Date getDate(String dateTime){
        if(dateTime == null){
            return null;
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
        return date;
    }


    @Override
    protected Object doExecute() throws Exception {

        Date startDate = getDate(startDateTime);
        Date endDate = getDate(endDateTime);

        if ((startDate != null) && (endDate != null)) {
            if (startDate.getTime() >= endDate.getTime()) {
                System.out.println("StatDateTime value cannot be greater or equal to EndDateTime");
                return null;
            }
        }
        if (persistenceService != null) {
            List<?> metrics = persistenceService.getMetrics(category, startDate, endDate);
            if (metrics == null || metrics.isEmpty()) {
                System.out.println("Either the category is not supported or no data of this category in the specified time range. ");
                return null;
            }
            System.out.println(listMetrics(metrics));
        } else {
            log.warn("ListMetricsCommand: persistence service is found to be null.");
        }
        return null;
    }

    abstract protected String listMetrics (List<?> metrics);
}
