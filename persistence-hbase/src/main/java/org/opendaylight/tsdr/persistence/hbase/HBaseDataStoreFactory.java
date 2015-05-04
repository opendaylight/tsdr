/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hbase;

/**
 *
 *
 * This class creates HBase Data Store.
 *
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 *
 * Created: Feb 24, 2015
 *
 *
 */
public class HBaseDataStoreFactory {

    private static HBaseDataStore datastore = null;

    /**
     * Default constructor
     */
    private HBaseDataStoreFactory(){
        super();
    }

    /**
     * To obtain or create the HBase Data Store.
     * @return HBaseDataStore
     */
    public static HBaseDataStore getHBaseDataStore(){
        if ( datastore == null){
            //load XML and initialize HBase data store
            HBaseDataStoreContext context = initialize_datastore_context();
            datastore = new HBaseDataStore(context);
        }
        return datastore;
    }

    /**
     * Initialize the data store context by reading from an XML
     * configuration file.
     * @return HBaseDataStoreContext
    */
    private static HBaseDataStoreContext initialize_datastore_context(){
        HBaseDataStoreContext context = new HBaseDataStoreContext();
        //To do: load xml files and set the context object
        context.setPoolSize(20);
        context.setZookeeperClientport("2181");
        context.setZookeeperQuorum("localhost");
        return context;
    }
}
