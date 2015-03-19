/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.service.impl;

import com.google.common.base.Preconditions;
import org.opendaylight.tsdr.entity.Metric;
import org.opendaylight.tsdr.persistence.spi.TsdrPersistenceService;
import org.opendaylight.tsdr.util.TsdrPersistenceServiceUtil;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

/**
 * Implementation of the TSDR Persistence SPI utilizing JPA based store
 *
 * Note; The JPA service is injected via blueprint framework
 *
 *
 * @author <a href="mailto:syedbahm@cisco.com">Basheeruddin Ahmed</a>
 *
 */

public class TsdrH2PersistenceServiceImpl implements
    TsdrPersistenceService {

    private final static Logger log = LoggerFactory.getLogger(TsdrH2PersistenceServiceImpl.class);
    private static final String SEPARATOR = "_";

    private  TsdrJpaServiceImpl jpaService;

    public TsdrH2PersistenceServiceImpl(){
        TsdrPersistenceServiceUtil.setTsdrPersistenceService(this);
    }

    /**
     * Store TSDRMetrics.
     */
    @Override
    public void store(TSDRMetricRecord metrics){
        Preconditions.checkArgument(metrics != null);
        DataCategory dc =metrics.getTSDRDataCategory();
        Preconditions.checkArgument( dc != null);
        Metric metric = getEntityFromModel(metrics);
        jpaService.add(metric);
     }

    /**
     * Store a list of TSDRMetrics.
    */
    @Override
    public void store(List<TSDRMetricRecord> metricList){
        Preconditions.checkArgument(metricList != null);
        if ( metricList != null && metricList.size() != 0){
            for(TSDRMetricRecord metric: metricList){
                store(metric);
            }
        }
    }

    @Override public void start(int timeout) {

    }

    @Override public void stop(int timeout) {
        if(jpaService != null){
            jpaService.close();
            jpaService = null;
        }
        TsdrPersistenceServiceUtil.setTsdrPersistenceService(null);


    }

    /**
     * Get persistence entry from TSDRMetric object.
     *
     * @param data
     * @return <code>Metric</code> persitence entity populated
     * @throws <code>IllegalArgumentException</code> if any of preconditions fails
     */
    public Metric getEntityFromModel(TSDRMetricRecord data){
        Preconditions.checkArgument(data != null,"getEntityFromModel found metric data = null");
        Preconditions.checkArgument(data.getNodeID() != null,"getEntityFromModel found metric data nodeId = null" );
        Preconditions.checkArgument(data.getMetricName() != null,"getEntityFromModel found metric name = null" );
        Preconditions.checkArgument(data.getMetricValue() != null,"getEntityFromModel found metric value = null");
        Preconditions.checkArgument(data.getTSDRDataCategory()!=null,"getEntityFromModel found timestamp of metric = null");

        Metric metric = new Metric();


        metric.setNodeId(data.getNodeID());
        metric.setMetricName(data.getMetricName());
        metric.setMetricValue(data.getMetricValue().getValue()
            .doubleValue());
        metric.setMetricCategory(data.getTSDRDataCategory().name());
        Date timeStamp = new Date(data.getTimeStamp().longValue());
        metric.setMetricTimeStamp(timeStamp);
        String detail = getDetailInfoFromModel(data);
        if(null != detail && !detail.isEmpty()) {
            metric.setInfo(detail);
        }

        return metric;
    }


    /**
     * Gets detail info from TSDRMetric data.
     * @param metricData
     * @return
     */
    private static String getDetailInfoFromModel(TSDRMetricRecord metricData){
        StringBuffer keyString = new StringBuffer();
        List<RecordKeys> recordKeys = metricData.getRecordKeys();
        if ( recordKeys != null && recordKeys.size() != 0){
            for(RecordKeys key: recordKeys){
                if (key.getKeyName() != null){

                    keyString.append(key.getKeyName())
                        .append(SEPARATOR)
                        .append(key.getKeyValue())
                        .append(SEPARATOR);
                }else{
                    log.warn("getDetailInfoFromModel: metric data contained null key name");
                }
            }
        }
        return keyString.toString();
    }


    public  TsdrJpaServiceImpl getJpaService() {
        return jpaService;
    }

    public  void setJpaService(TsdrJpaServiceImpl jpaService) {
        this.jpaService = jpaService;
    }

}
