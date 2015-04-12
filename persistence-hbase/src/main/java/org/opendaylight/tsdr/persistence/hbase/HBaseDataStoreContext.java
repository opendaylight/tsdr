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

}
