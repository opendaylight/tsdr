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
 */
public interface TsdrHBaseDataStoreConstants {
    /**
     * Other constants used in HBase tables for OpenFlow statistics data.
     */
    String ROWKEY_SPLIT = "_";
    String COLUMN_FAMILY_NAME = "c1";
    String COLUMN_QUALIFIER_NAME = "raw";
    String LOGRECORD_FULL_TEXT = "RecordFullText";

    /**
     * Constants related to ListMetricsCommand.
     */
    int MAX_QUERY_RECORDS = 1000;
}
