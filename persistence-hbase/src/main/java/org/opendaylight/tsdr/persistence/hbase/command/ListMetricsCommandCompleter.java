/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hbase.command;

import java.util.List;

import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.opendaylight.tsdr.persistence.hbase.TSDRHBaseDataStoreConstants;
/**
*This class provides the tab completer for tsdr:list command to list the metrics info
*from TSDR.
*
*
*
* @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
*
* Created: April, 2015
**/
public class ListMetricsCommandCompleter implements Completer{
    @Override
    public int complete(String buffer, int cursor, List candidates) {
        StringsCompleter delegate = new StringsCompleter();
        delegate.getStrings().add(TSDRHBaseDataStoreConstants.FLOW_STATS_CATEGORY_NAME);
        delegate.getStrings().add(TSDRHBaseDataStoreConstants.FLOW_TABLE_STATS_CATEGORY_NAME);
        delegate.getStrings().add(TSDRHBaseDataStoreConstants.INTERFACE_STATS_CATEGORY_NAME);
        delegate.getStrings().add(TSDRHBaseDataStoreConstants.QUEUE_STATS_CATEGORY_NAME);
        delegate.getStrings().add(TSDRHBaseDataStoreConstants.GROUP_STATS_CATEGORY_NAME);
        delegate.getStrings().add(TSDRHBaseDataStoreConstants.METER_STATS_CATEGORY_NAME);
        return delegate.complete(buffer, cursor, candidates);
    }
}
