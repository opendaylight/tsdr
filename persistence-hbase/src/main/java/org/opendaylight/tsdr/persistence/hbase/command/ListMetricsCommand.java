/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hbase.command;

import java.util.List;

import org.apache.karaf.shell.commands.Command;
import org.opendaylight.tsdr.command.AbstractListMetricsCommand;
import org.opendaylight.tsdr.persistence.spi.TsdrPersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
*This class implement the functionality of the tsdr:list command from Karaf
*console.
*
*It takes the arguments in the command input and query the hbase data store
*for the records that satisfy the criteria.
*
*Since there is no paging support, the maximum number of records would be 1000.
*
* @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
*
* Created: April, 2015
**/
@Command(scope = "tsdr", name = "list", description = "Lists recent 1000 metrics(default) or returns time specified metrics")
public class ListMetricsCommand  extends AbstractListMetricsCommand {
    private final Logger
    log = LoggerFactory.getLogger(ListMetricsCommand.class);

/**
 * Format and print out the result of the metrics on Karaf console.
 */
@Override
protected String listMetrics(List<?> metrics) {
    log.debug("Entering ListMetrics");
    List<String> metricList = (List<String>) metrics;
    StringBuffer buffer = null;
    buffer = new StringBuffer();
    for (String record : metricList) {
        buffer.append(record)
                .append("\n");
    }
    log.debug("Exiting ListMetrics");
    return buffer.toString();
}


}
