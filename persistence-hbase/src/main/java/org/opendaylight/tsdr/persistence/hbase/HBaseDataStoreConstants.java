/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hbase;

/**
 * This class is to store the constants used in generic HBase data store.
 *
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 *
 * Created: Feb 24, 2015
 */
public class HBaseDataStoreConstants {
    /**
     * Zookeeper constants for hbase clients to connect to the Zookeeper node.
     */
     public static final String ZOOKEEPER_QUORUM = "hbase.zookeeper.quorum";
     public static final String ZOOKEEPER_CLIENTPORT = "hbase.zookeeper.property.clientPort";


}
