/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.persistence.hsqldb.command;

import java.util.List;
import org.apache.karaf.shell.commands.Command;
import org.opendaylight.tsdr.command.AbstractListMetricsCommand;
import org.opendaylight.tsdr.persistence.hsqldb.FormatUtil;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;

/*
 * Updated by Sharon Aicler (saichler@cisco.com) for hsqldb impl.
 */
/**
*This class implement the functionality of the tsdr:list command from Karaf
*console.
*
*It takes the arguments in the command input and query the hsqldb data store
*for the records that satisfy the criteria.
*
*Since there is no paging support, the maximum number of records would be 1000.
*
* @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
* @author <a href="mailto:saichler@cisco.com">Sharon Aicler</a>
* Created: Aug, 2015
**/
@Command(scope = "tsdr", name = "list", description = "Lists recent 1000 metrics(default) or returns time specified metrics")
public class ListMetricsCommand  extends AbstractListMetricsCommand {
    /**
     * Format and print out the result of the metrics on Karaf console.
     */
    @Override
    protected String listMetrics(List<?> metrics) {
        StringBuilder buffer = new StringBuilder();
        for (Object m : metrics) {
            TSDRMetricRecord metric = (TSDRMetricRecord)m;
            buffer.append(FormatUtil.getTSDRMetricKeyWithTimeStamp(metric));
            buffer.append("[").append(metric.getMetricValue()).append("]\n");
        }
        return buffer.toString();
    }
}
