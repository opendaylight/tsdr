package org.opendaylight.tsdr.command.completer;
/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */


import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.opendaylight.tsdr.model.TSDRConstants;

import java.util.List;

/**
 * This command is provided to get a list of metrics based on arguments passed
 *
 * @author <a href="mailto:syedbahm@cisco.com">Basheeruddin Ahmed</a>
 *
 */
public class ListMetricsCommandCompleter implements Completer {



    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {

            StringsCompleter completer = new StringsCompleter();
            completer.getStrings().add(TSDRConstants.FLOW_STATS_CATEGORY_NAME);
            completer.getStrings().add(TSDRConstants.FLOW_TABLE_STATS_CATEGORY_NAME);
            completer.getStrings().add(TSDRConstants.PORT_STATS_CATEGORY_NAME);
            completer.getStrings().add(TSDRConstants.QUEUE_STATS_CATEGORY_NAME);
            completer.getStrings().add(TSDRConstants.FLOW_GROUP_STATS_CATEGORY_NAME);
            completer.getStrings().add(TSDRConstants.FLOW_METER_STATS_CATEGORY_NAME);
            return completer.complete(buffer, cursor, candidates);
        }
}
