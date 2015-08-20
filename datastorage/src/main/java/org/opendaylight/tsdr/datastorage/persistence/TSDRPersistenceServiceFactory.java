/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.datastorage.persistence;


import org.opendaylight.tsdr.spi.model.TSDRConstants;
import org.opendaylight.tsdr.spi.persistence.TsdrPersistenceService;
import org.opendaylight.tsdr.spi.util.TsdrPersistenceServiceUtil;
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
 * Revision: March 05, 2015
 * @author <a href="mailto:syedbahm@cisco.com">Basheeruddin Ahmed</a>
 *
 */
public class TSDRPersistenceServiceFactory {
    private static final Logger log = LoggerFactory.getLogger(TSDRPersistenceServiceFactory.class);

    private static TsdrPersistenceService persistenceService = null;

    /**
     * Default constructor
    */
    private TSDRPersistenceServiceFactory(){
        super();
    }


    /**
     * Obtain the TSDR Persistence Data Store
     * @return
     */
    public static TsdrPersistenceService getTSDRPersistenceDataStore( ){
        log.debug("Entering getTSDRPersistenceDataStore()");
        if(persistenceService== null){
            persistenceService = TsdrPersistenceServiceUtil.getTsdrPersistenceService();
            if(persistenceService == null) {
                log.error("persistenceService is found to be null");
            }
        }

        log.debug("Exiting getTSDRPersistenceDataStore()");
        return persistenceService;
    }

}
