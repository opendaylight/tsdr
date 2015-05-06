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
import org.opendaylight.tsdr.model.TSDRConstants;
import org.opendaylight.tsdr.service.TsdrJpaService;
import org.opendaylight.tsdr.util.FormatUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Date;
import java.util.List;

/**
 * JPA Service implementation helping in performing the
 * JPA store operations.
 *
 * The <code>javax.persistence.EntityManager</code> is injected
 * using blueprint framework - the definition of which
 * can be found in resources/blueprint.xml
 *
 * @author <a href="mailto:syedbahm@cisco.com">Basheeruddin Ahmed</a>
 */

public class TsdrJpaServiceImpl implements TsdrJpaService {

    private EntityManager em;
    private static final Logger
        log = LoggerFactory.getLogger(TsdrJpaServiceImpl.class);


    @Override public void add(Metric metric) {
        Preconditions.checkArgument(em != null, "EntityManager found to be null");
        Preconditions.checkArgument(metric != null,"add metric called with null metric");
        try {
            em.persist(metric);
            em.flush();

        }catch(Exception e){
            log.error("TsdrJpaServiceImpl:add", e);
        }
    }

    @Override public void deleteAll() {
        Preconditions.checkArgument(em != null, "EntityManager found to be null");
        try {

            em.createQuery("delete from Metric").executeUpdate();
            em.flush();

        }catch(Exception e){
            log.error("TsdrJpaServiceImpl:delete", e);
        }
    }

    @Override
    public List<Metric> getMetricsFilteredByCategory(String category,int maxResults) {
        Preconditions.checkArgument(em != null, "EntityManager found to be null");
        //default to 1000 results
        if(maxResults <=0){
            maxResults = TSDRConstants.MAX_RESULTS_FROM_LIST_METRICS_COMMAND;
        }
        StringBuffer sb = new StringBuffer();
        sb.append("select * from Metric where metriccategory = '")
                .append(category.toUpperCase())
                .append("' order by metrictimestamp desc");
        log.info("getMetricsFilteredByCategory: query being sent is "+ sb.toString());
        Query nativeQuery = em.createNativeQuery(sb.toString(),Metric.class);
        return nativeQuery.getResultList();


    }

    @Override
    public List<Metric> getMetricsFilteredByCategory(String category, Date startDateTime, Date endDateTime) {
        Preconditions.checkArgument(em != null, "EntityManager found to be null");
        log.info("getMetricsFilteredByCateory:called with category={},startDateTime={},endDateTime ={}",
                category, startDateTime, endDateTime);

        if((startDateTime == null )||(endDateTime == null)){
            return getMetricsFilteredByCategory(category,TSDRConstants.MAX_RESULTS_FROM_LIST_METRICS_COMMAND);
        }else{
            StringBuffer sb = new StringBuffer();
            sb.append("select * from Metric where metrictimestamp ")
              .append("between '")
              .append(FormatUtil.getFormattedTimeStamp(startDateTime.getTime(),
                      FormatUtil.QUERY_TIMESTAMP))
              .append("' and '")
              .append(FormatUtil.getFormattedTimeStamp(endDateTime.getTime(),
                            FormatUtil.QUERY_TIMESTAMP))
              .append("' and metriccategory = '")
              .append(category.toUpperCase())
              .append("'")
              .append(" order by metrictimestamp desc");
            log.info("getMetricsFilteredByCategory with start date and end date: query being sent is "+ sb.toString());
            Query nativeQuery = em.createNativeQuery(sb.toString(),Metric.class);
            return nativeQuery.getResultList();
        }

    }


    @Override public void close() {
        try{
            em.close();
            em.clear();
        }catch(Exception e){
            log.warn("Exception occurred when closing the persistence store",e);
        }
    }

    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

}
