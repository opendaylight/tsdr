/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.datastorage.persistence;


import org.opendaylight.tsdr.persistence.DataStoreType;
import org.opendaylight.tsdr.persistence.TSDRPersistenceService;
import org.opendaylight.tsdr.persistence.hbase.TSDRHBasePersistenceServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to create a TSDRPersistence Service with a specified or configured
 * Persistence data store.
 *
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 *
 * Created: Feb 24, 2015
 *
 */
public class TSDRPersistenceServiceFactory {
    private static final Logger log = LoggerFactory.getLogger(TSDRPersistenceServiceFactory.class);

    private static TSDRPersistenceService data_store = null;
    //To do: will change this to a configuration file look up
    // to decide what data staore we need to configure
    private static Enum<?> data_store_type = DataStoreType.HBASE;

    public static Enum<?> getData_store_type() {
        return data_store_type;
    }

    public static void setData_store_type(Enum<?> data_store_type) {
        TSDRPersistenceServiceFactory.data_store_type = data_store_type;
    }

    /**
     * Default constructor
    */
    private TSDRPersistenceServiceFactory(){
        super();
    }

    /**
     * Obtain the TSDR Persistence Data Store based on specified data store type.
     * @param data_store_type
     * @return
     */
    public static TSDRPersistenceService getTSDRPersistenceDataStore(Enum<?> data_store_type){
        log.debug("Entering getTSDRPersistenceDataStore(data_store_type)");
        if ( data_store == null && data_store_type == DataStoreType.HBASE){
            data_store = new TSDRHBasePersistenceServiceImpl();
        }
        log.debug("Exiting getTSDRPersistenceDataStore(data_store_type)");
        return data_store;
    }

    /**
     * Obtain the TSDR Persistence Data Store
     * @return
     */
    public static TSDRPersistenceService getTSDRPersistenceDataStore( ){
        log.debug("Entering getTSDRPersistenceDataStore()");
        if ( data_store == null && data_store_type == DataStoreType.HBASE){
            data_store = new TSDRHBasePersistenceServiceImpl();
        }
        log.debug("Exiting getTSDRPersistenceDataStore()");
        return data_store;
    }

}
