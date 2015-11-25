/*
 * Copyright (c) 2015 Cisco Systems Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.spi.util;

import java.util.Date;

import org.opendaylight.tsdr.spi.model.TSDRConstants;
import org.opendaylight.tsdr.spi.persistence.TsdrPersistenceService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
/**
 * Return String type category name from Category Enum.
 * @param category
 * @return
 */
    public static String getCategoryNameFrom(DataCategory category){
        if ( category == DataCategory.EXTERNAL ){
            return TSDRConstants.EXTERNAL;
        }else  if ( category == DataCategory.FLOWGROUPSTATS ){
            return TSDRConstants.FLOW_GROUP_STATS_CATEGORY_NAME;
        }else if ( category == DataCategory.FLOWMETERSTATS){
                return TSDRConstants.FLOW_METER_STATS_CATEGORY_NAME;
        }else if ( category == DataCategory.FLOWSTATS){
                return TSDRConstants.FLOW_STATS_CATEGORY_NAME;
        }else if ( category == DataCategory.FLOWTABLESTATS){
                return TSDRConstants.FLOW_TABLE_STATS_CATEGORY_NAME;
        }else if ( category == DataCategory.NETFLOW){
                return TSDRConstants.NETFLOW_CATEGORY_NAME;
        }else if ( category == DataCategory.SYSLOG){
                return TSDRConstants.SYSLOG_CATEGORY_NAME;
        }else if ( category == DataCategory.PORTSTATS){
                return TSDRConstants.PORT_STATS_CATEGORY_NAME;
        }else if ( category == DataCategory.QUEUESTATS){
            return TSDRConstants.QUEUE_STATS_CATEGORY_NAME;
        }else if ( category == DataCategory.SNMPINTERFACES){
            return TSDRConstants.SNMPINTERFACE_CATEGORY_NAME;
        }else{
            log.warn("The category is not supported", category);
            return null;
        }
    }
/**
 * Return DataCategory Enum from String category name.
 * @param category
 * @return
 */
    public static DataCategory getCategoryFrom(String category){
        if ( category.equalsIgnoreCase(TSDRConstants.EXTERNAL)){
            return DataCategory.EXTERNAL;
        }else  if ( category.equalsIgnoreCase(TSDRConstants.FLOW_GROUP_STATS_CATEGORY_NAME )){
            return DataCategory.FLOWGROUPSTATS;
        }else if ( category.equalsIgnoreCase(TSDRConstants.FLOW_METER_STATS_CATEGORY_NAME)){
                return DataCategory.FLOWMETERSTATS;
        }else if ( category.equalsIgnoreCase(TSDRConstants.FLOW_STATS_CATEGORY_NAME)){
                return DataCategory.FLOWSTATS;
        }else if ( category.equalsIgnoreCase(TSDRConstants.FLOW_TABLE_STATS_CATEGORY_NAME)){
                return DataCategory.FLOWTABLESTATS;
        }else if ( category.equalsIgnoreCase(TSDRConstants.NETFLOW_CATEGORY_NAME)){
                return DataCategory.NETFLOW;
        }else if ( category.equalsIgnoreCase(TSDRConstants.SYSLOG_CATEGORY_NAME)){
                return DataCategory.SYSLOG;
        }else if ( category.equalsIgnoreCase(TSDRConstants.PORT_STATS_CATEGORY_NAME)){
                return DataCategory.PORTSTATS;
        }else if ( category.equalsIgnoreCase(TSDRConstants.QUEUE_STATS_CATEGORY_NAME)){
            return DataCategory.QUEUESTATS;
        }else if ( category.equalsIgnoreCase(TSDRConstants.SNMPINTERFACE_CATEGORY_NAME)){
            return DataCategory.SNMPINTERFACES;
        }else{
            log.warn("The category is not supported", category);
            return null;
        }
    }
}
