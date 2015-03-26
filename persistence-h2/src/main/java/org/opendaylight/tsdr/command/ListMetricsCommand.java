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
 * This command is provided to get 1000 recent metrics
 * collected in the JPA store.
 *
 * @author <a href="mailto:syedbahm@cisco.com">Basheeruddin Ahmed</a>
 *
 */
@Command(scope = "metric", name = "list", description = "Lists recent 1000 metrics")
public class ListMetricsCommand extends OsgiCommandSupport {
    private final Logger
        log = LoggerFactory.getLogger(ListMetricsCommand.class);
    private TsdrJpaService persistenceService;

    public void setPersistenceService(TsdrJpaService persistenceService) {
        this.persistenceService = persistenceService;
    }


    @Override protected Object doExecute() throws Exception {
        if(persistenceService !=null) {
            List<Metric> metrics = persistenceService.getAll(TSDRConstants.MAX_RESULTS_FROM_LIST_METRICS_COMMAND);
            StringBuffer buffer = null;
            for (Metric metric : metrics) {
                buffer = new StringBuffer();
                buffer.append(metric.getId())
                    .append(" | ")
                    .append(metric.getMetricName())
                    .append(" | ")
                    .append(metric.getMetricValue())
                    .append(" | ")
                    .append(metric.getMetricTimeStamp())
                    .append("|")
                    .append(metric.getNodeId())
                    .append("|")
                    .append(metric.getMetricCategory())
                    .append("|")
                    .append(metric.getInfo());
                //this will output to Karaf console hence requires doing output std output.
                System.out.println(buffer.toString());

            }
        }else{
            log.warn("ListMetricsCommand: persistence service is found to be null.");
        }

        return null;
    }
}
