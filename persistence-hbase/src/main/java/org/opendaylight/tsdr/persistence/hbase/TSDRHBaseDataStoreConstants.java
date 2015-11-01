/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hbase;
/**
 * This class stores the constants used in TSDR HBase Data Store.
 *
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 *
 * Created: Feb 24, 2015
 *
 */
public class TSDRHBaseDataStoreConstants {

    /**
     * HBase tables name for OpenFlow statistics data.
    */
    public static final String FLOW_STATS_TABLE_NAME = "FlowMetrics";
    public static final String FLOW_TABLE_STATS_TABLE_NAME = "FlowTableMetrics";
    public static final String INTERFACE_METRICS_TABLE_NAME = "InterfaceMetrics";
    public static final String QUEUE_METRICS_TABLE_NAME = "QueueMetrics";
    public static final String GROUP_METRICS_TABLE_NAME = "GroupMetrics";
    public static final String METER_METRICS_TABLE_NAME = "MeterMetrics";
    
    /**
     * HBase tables name for SysLog data.
     */
    public static final String SYSLOG_TABLE_NAME = "SysLog";
    
    /**
     * HBase tables name for NetFlow data.
     */
    public static final String NETFLOW_TABLE_NAME = "NetFlow";
    /**
     * Other constants used in HBase tables for OpenFlow statistics data.
    */
    public static final String ROWKEY_SPLIT = "_";
    public static final String COLUMN_FAMILY_NAME = "c1";
    public static final String COLUMN_QUALIFIER_NAME = "raw";
    public static final String LOGRECORD_FULL_TEXT = "RecordFullText";

    /**
     * Constants related to ListMetricsCommand
     */
    public static final int MAX_QUERY_RECORDS = 1000;
}
