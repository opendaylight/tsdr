/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hbase.command;

import java.util.Date;
import java.util.List;

import org.apache.karaf.shell.commands.Command;
import org.opendaylight.tsdr.command.AbstractListMetricsCommand;
import org.opendaylight.tsdr.persistence.hbase.TSDRHBaseDataStoreConstants;
import org.opendaylight.tsdr.persistence.hbase.TSDRHBasePersistenceServiceImpl;
import org.opendaylight.tsdr.persistence.spi.TsdrPersistenceService;
import org.opendaylight.tsdr.util.TsdrPersistenceServiceUtil;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
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

    private TsdrPersistenceService persistenceService;

/**
 * Format and print out the result of the metrics on Karaf console.
 */
@Override
protected String listMetrics(List<?> metrics) {
    List<String> metricList = (List<String>) metrics;
    StringBuffer buffer = null;
    buffer = new StringBuffer();
    for (String record : metricList) {
        buffer.append(record)
                .append("\n");
    }
    return buffer.toString();
}
/**
 * Execution logic of the command
 */

@Override
protected Object doExecute() throws Exception {
     //first validate the input arguments
     if (!isCategoryValid(metricsCategory)){
        System.out.println("Metrics Category is invalid");
        return null;
     }
     Date startTime = getDate(startDateTime);
     Date endTime = getDate(endDateTime);
     if ( startTime == null ){
         System.out.println("Start Time is invalid.");
         return null;
     }else if ( endTime == null){
         System.out.println("End Time is invalid.");
         return null;
     }
    persistenceService = TsdrPersistenceServiceUtil.getTsdrPersistenceService();

    if(persistenceService !=null && persistenceService instanceof TSDRHBasePersistenceServiceImpl) {

        List<?> metrics = persistenceService.getMetrics(metricsCategory, startTime, endTime);
        if ( metrics == null || metrics.isEmpty()){
            System.out.println("No data of this category in the specified time range.");
        }
        System.out.println(listMetrics(metrics));
    }else{
        log.warn("ListMetricsCommand: persistence service is found to be null.");
    }

    return null;
}

private boolean isCategoryValid(String metricsCategory){
    if (metricsCategory.equalsIgnoreCase(TSDRHBaseDataStoreConstants.FLOW_STATS_TABLE_NAME)
      || metricsCategory.equalsIgnoreCase(TSDRHBaseDataStoreConstants.FLOW_TABLE_STATS_TABLE_NAME)
      || metricsCategory.equalsIgnoreCase(TSDRHBaseDataStoreConstants.INTERFACE_METRICS_TABLE_NAME)
      || metricsCategory.equalsIgnoreCase(TSDRHBaseDataStoreConstants.QUEUE_METRICS_TABLE_NAME)
      || metricsCategory.equalsIgnoreCase(TSDRHBaseDataStoreConstants.GROUP_METRICS_TABLE_NAME)){
        return true;
      }
    return false;
}
}
