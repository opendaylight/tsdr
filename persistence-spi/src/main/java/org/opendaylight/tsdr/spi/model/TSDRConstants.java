/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.spi.model;

/**
 * Stores the constants that are shared among TSDR components.
 *
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 * @author <a href="mailto:syedbahm@cisco.com">Basheeruddin Ahmed</a>
 *
 */
public interface TSDRConstants {
    /**
     * Keys used in a TSDR Record.
     */
    String FLOW_TABLE_KEY_NAME = "TableID";
    String FLOW_KEY_NAME = "FlowID";
    String INTERNFACE_KEY_NAME = "PortID";
    String QUEUE_KEY_NAME = "QueueID";
    String GROUP_KEY_NAME = "GroupID";
    String METER_KEY_NAME = "MeterID";
    String BUCKET_KEY_NAME = "BucketID";
    String ID_SPLIT = ".";
    String ID_SPLIT_ARG = "_";

    /**
     * Default timeout for persistence service start and stop.
     */

    int STOP_PERSISTENCE_SERVICE_TIMEOUT = 1000;
    int START_PERSISTENCE_SERVICE_TIMEOUT = 1000;

    /**
     * Default max number of results to return from ListMetrics command.
     */
    int MAX_RESULTS_FROM_LIST_METRICS_COMMAND = 1000;


    /**
     * constants for metrics categories.
     */
    /*Why do we need this???? we have DataCategory.name() **
    String FLOW_STATS_CATEGORY_NAME = "FlowStats";
    String FLOW_TABLE_STATS_CATEGORY_NAME = "FlowTableStats";;
    String PORT_STATS_CATEGORY_NAME = "PortStats";
    String QUEUE_STATS_CATEGORY_NAME = DataCategory.QUEUESTATS.name();
    String FLOW_GROUP_STATS_CATEGORY_NAME = "FlowGroupStats";
    String FLOW_METER_STATS_CATEGORY_NAME = "FlowMeterStats";
    String SYSLOG_CATEGORY_NAME = "SysLog";
    String NETFLOW_CATEGORY_NAME = "NetFlow";
    String SNMPINTERFACE_CATEGORY_NAME = "SNMPInterfaces";
    String EXTERNAL = "External";
    */
    String ROWKEY_SPLIT = "_";
}
