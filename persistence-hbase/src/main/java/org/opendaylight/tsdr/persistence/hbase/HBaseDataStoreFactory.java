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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class creates HBase Data Store.
 *
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 * @author <a href="mailto:hariharan_sethuraman@dell.com">Hariharan Sethuraman</a>
 */
@Singleton
public class HBaseDataStoreFactory {
    private static final Logger LOG = LoggerFactory.getLogger(HBaseDataStoreFactory.class);
    private static final String HBASE_PROPS_FILENAME = "tsdr-persistence-hbase.properties";

    private final HBaseDataStore datastore;
    private final HBaseDataStoreContext dataStoreContext;

    @Inject
    public HBaseDataStoreFactory() {
        dataStoreContext = initializeDatastoreContext();
        datastore = new HBaseDataStore(dataStoreContext);
    }

    /**
     * Obtains the HBase Data Store.
     * @return HBaseDataStore
     */
    public HBaseDataStore getHBaseDataStore() {
        return datastore;
    }

    public HBaseDataStoreContext getDataStoreContext() {
        return dataStoreContext;
    }

    /**
     * Initialize the data store context by reading from an XML
     * configuration file.
     * @return HBaseDataStoreContext
    */
    private static HBaseDataStoreContext initializeDatastoreContext() {
        File file = new File(System.getProperty("karaf.etc") + "/" + HBASE_PROPS_FILENAME);
        try (InputStream inputStream = new FileInputStream(file)) {
            LOG.info("Loading properties from {}", file);
            Properties properties = new Properties();
            properties.load(inputStream);
            return new HBaseDataStoreContext(properties);
        } catch (FileNotFoundException e) {
            LOG.error("Properties file {} is missing", file);
        } catch (IOException e) {
            LOG.error("Error loading properties from file {}", file, e);
        }

        LOG.warn("Using HbaseDataStoreContext default values");
        return new HBaseDataStoreContext();
    }
}
