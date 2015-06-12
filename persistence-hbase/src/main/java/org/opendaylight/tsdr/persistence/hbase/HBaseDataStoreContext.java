/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hbase;
/**
 * The context of HBase Data Store.
 *
 * The context will be loaded from an HBase configuration file.
 *
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 *
 * Created: Feb 24, 2015
 *
 *
 */

import java.util.Map;
import java.util.HashMap;

public class HBaseDataStoreContext {
    /**
     * This parameter indicates the host name of the server(Zookeeper node)
     * that HBase client communicates with.
     */
    private  String zookeeperQuorum = "localhost";
    /**
     * This parameter indicates the port number for the HBase
     * client to communicate with the server(Zookeeper node).
     */
    private  String zookeeperClientport = "2181";
    /**
     * This parameter indicates the size of the pool for the HBase
     * Client to connect with the server(Zookeeper node).
     */
    private  int poolSize = 5;

    private int writeBufferSize = 512;

    private boolean autoFlush = false;

    private static Map<String,Object> commonHbasePropertiesMap = new HashMap<String,Object>();
    public static String HBASE_COMMON_PROP_CREATE_TABLE_RETRY_INTERVAL = "hbase-common-prop-create-table-retry-interval";

    public  String getZookeeperQuorum() {
        return zookeeperQuorum;
    }
    public void setZookeeperQuorum(String zookeeperQuorum) {
        this.zookeeperQuorum = zookeeperQuorum;
    }
    public String getZookeeperClientport() {
        return zookeeperClientport;
    }
    public void setZookeeperClientport(String zookeeperClientport) {
        this.zookeeperClientport = zookeeperClientport;
    }
    public int getPoolSize() {
        return poolSize;
    }
    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }
    public int getWriteBufferSize() {
        return writeBufferSize;
    }
    public void setWriteBufferSize(int writeBufferSize) {
        this.writeBufferSize = writeBufferSize;
    }
    public void setAutoFlush(boolean autoFlush) {
        this.autoFlush = autoFlush;
    }
    public boolean getAutoFlush() {
        return this.autoFlush;
    }

    public static void addProperty(String property, long createTableRetryInterval){
        commonHbasePropertiesMap.put(property, createTableRetryInterval);
    }

    public static Long getPropertyInLong(String property){
        return (Long)commonHbasePropertiesMap.get(property);
    }
}
