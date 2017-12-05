/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hbase;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class creates HBase Data Store.
 *
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 * @author <a href="mailto:hariharan_sethuraman@dell.com">Hariharan Sethuraman</a>
 */
public class HBaseDataStoreFactory {
    private static final Logger LOG = LoggerFactory.getLogger(HBaseDataStoreFactory.class);
    private static HBaseDataStore datastore;
    private static String hbasePropsFilename = "tsdr-persistence-hbase.properties";

    /**
     * Default constructor.
     */
    private HBaseDataStoreFactory() {
    }

    /**
     * To obtain or create the HBase Data Store.
     * @return HBaseDataStore
     */
    public static HBaseDataStore getHBaseDataStore() {
        //load XML and initialize HBase data store
        if (datastore == null) {
            HBaseDataStoreContext context = initializeDatastoreContext();
            datastore = new HBaseDataStore(context);
        }
        return datastore;
    }

    /*
     * Setter for UT purpose which is invoked from the factory
     */
    static void setHBaseDataStoreIfAbsent(HBaseDataStore hbaseDataStore) {
        if (datastore == null) {
            initializeDatastoreContext();// just for UT purpose
            datastore = hbaseDataStore;
        }
    }

    /**
     * Initialize the data store context by reading from an XML
     * configuration file.
     * @return HBaseDataStoreContext
    */
    private static HBaseDataStoreContext initializeDatastoreContext() {
        HBaseDataStoreContext context = new HBaseDataStoreContext();
        Properties properties = new Properties();
        InputStream inputStream = null;

        try {
            String fileFullPath = System.getProperty("karaf.etc") + "/" + hbasePropsFilename;
            File file = new File(fileFullPath);
            if (file.exists()) {
                LOG.info("Loading properties from " + fileFullPath);
                inputStream = new FileInputStream(file);
                properties.load(inputStream);
            } else {
                LOG.error("Property file " + fileFullPath + " missing");
            }
        } catch (IOException e) {
            LOG.error("Exception while loading the hbase-configuration.properties stream", e);
        }

        try {
            if (inputStream == null || !properties.propertyNames().hasMoreElements()) {
                LOG.error("Properties stream is null or properties failed to load, check the file=" + hbasePropsFilename
                        + " exists in classpath");
                LOG.warn("Initializing HbaseDataStoreContext default values");
                context.setPoolSize(20);
                context.setZookeeperClientport("2181");
                context.setZookeeperQuorum("localhost");
                context.setAutoFlush(false);
                context.setWriteBufferSize(512);
                HBaseDataStoreContext.addProperty(HBaseDataStoreContext.HBASE_COMMON_PROP_CREATE_TABLE_RETRY_INTERVAL,
                        300L);
                return context;
            }

            LOG.info("Updating properties onto context");

            context.setPoolSize(Integer.valueOf(properties.getProperty("poolsize")));
            context.setZookeeperClientport(properties.getProperty("zoo.keeper.client.port"));
            context.setZookeeperQuorum(properties.getProperty("zoo.keeper.quorum"));
            context.setAutoFlush(Boolean.valueOf(properties.getProperty("autoflush")));
            context.setWriteBufferSize(Integer.valueOf(properties.getProperty("writebuffersize")));
            HBaseDataStoreContext.addProperty(HBaseDataStoreContext.HBASE_COMMON_PROP_CREATE_TABLE_RETRY_INTERVAL,
                    Long.valueOf(properties.getProperty("createTableRetryInterval")));

        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    LOG.error("Exception while closing the stream", e);
                }
            }
        }
        return context;
    }
}
