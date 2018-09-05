/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hbase;

import java.util.Properties;

/**
 * The context of HBase Data Store.
 * The context will be loaded from an HBase configuration file.
 *
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 */
public class HBaseDataStoreContext {
    static final String ZOO_KEEPER_QUORUM_PROP = "zoo.keeper.quorum";
    static final String ZOO_KEEPER_CLIENT_PORT_PROP = "zoo.keeper.client.port";
    static final String POOL_SIZE_PROP = "poolsize";
    static final String WRITE_BUFFER_SIZE_PROP = "writebuffersize";
    static final String AUTO_FLUSH_PROP = "autoflush";
    static final String CREATE_TABLE_RETRY_INTERVAL_PROP = "createTableRetryInterval";

    /*
     * This parameter indicates the host name of the server(Zookeeper node)
     * that HBase client communicates with.
     */
    private final String zookeeperQuorum;

    /*
     * This parameter indicates the port number for the HBase
     * client to communicate with the server(Zookeeper node).
     */
    private final String zookeeperClientport;

    /*
     * This parameter indicates the size of the pool for the HBase
     * Client to connect with the server(Zookeeper node).
     */
    private final int poolSize;

    private final int writeBufferSize;

    private final boolean autoFlush;

    private final long createTableRetryInterval;

    HBaseDataStoreContext() {
        this(new Properties());
    }

    HBaseDataStoreContext(Properties from) {
        zookeeperQuorum = from.getProperty(ZOO_KEEPER_QUORUM_PROP, "localhost");
        zookeeperClientport = from.getProperty(ZOO_KEEPER_CLIENT_PORT_PROP, "2181");
        poolSize = Integer.parseInt(from.getProperty(POOL_SIZE_PROP, "20"));
        writeBufferSize = Integer.parseInt(from.getProperty(WRITE_BUFFER_SIZE_PROP, "512"));
        autoFlush = Boolean.valueOf(from.getProperty(AUTO_FLUSH_PROP, "false"));
        createTableRetryInterval = Long.parseLong(from.getProperty("createTableRetryInterval", "300"));
    }

    public String getZookeeperQuorum() {
        return zookeeperQuorum;
    }

    public String getZookeeperClientport() {
        return zookeeperClientport;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public int getWriteBufferSize() {
        return writeBufferSize;
    }

    public boolean getAutoFlush() {
        return this.autoFlush;
    }

    public long getCreateTableRetryInterval() {
        return createTableRetryInterval;
    }
}
