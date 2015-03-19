/*
 * Copyright (c) 2015 Cisco Systems Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.util;

import org.opendaylight.tsdr.model.TSDRConstants;
import org.opendaylight.tsdr.persistence.spi.TsdrPersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * This is done for the dependency injection
 * of the persistence implementation based on the persistence feature
 * activation.
 *
 * @author <a href="mailto:syedbahm@cisco.com">Basheeruddin Ahmed</a>
 */
public class TsdrPersistenceServiceUtil {
    private static final Logger
        log = LoggerFactory.getLogger(TsdrPersistenceServiceUtil.class);
    static private TsdrPersistenceService tsdrPersistenceService;
    static public TsdrPersistenceService getTsdrPersistenceService (){
          return tsdrPersistenceService;
    }
    static public void setTsdrPersistenceService (TsdrPersistenceService service){
        log.info("setTsdrPersistenceService: called " + new Date());
          if(tsdrPersistenceService != null){
              tsdrPersistenceService.stop(
                  TSDRConstants.STOP_PERSISTENCE_SERVICE_TIMEOUT);
          }
          tsdrPersistenceService = service;

          if(tsdrPersistenceService !=null) {

              tsdrPersistenceService
                  .start(TSDRConstants.START_PERSISTENCE_SERVICE_TIMEOUT);
          }
    }

}
