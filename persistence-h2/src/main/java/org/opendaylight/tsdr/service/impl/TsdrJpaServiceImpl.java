/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.service.impl;

import com.google.common.base.Preconditions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.opendaylight.tsdr.entity.Metric;
import org.opendaylight.tsdr.service.TsdrJpaService;
import org.opendaylight.tsdr.spi.model.TSDRConstants;
import org.opendaylight.tsdr.spi.util.FormatUtil;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                .append("' order by metrictimestamp desc LIMIT " + maxResults);
        log.info("getMetricsFilteredByCategory: query being sent is "+ sb.toString());
        Query nativeQuery = em.createNativeQuery(sb.toString(),Metric.class);
        return nativeQuery.getResultList();


    }

    @Override
    public List<TSDRMetricRecord> getMetricsFilteredByCategory(String tsdrMetricKey, long startDateTime, long endDateTime) {
        Preconditions.checkArgument(em != null, "EntityManager found to be null");
        log.info("getMetricsFilteredByCateory:called with category={},startDateTime={},endDateTime ={}",
                tsdrMetricKey, startDateTime, endDateTime);

        List<TSDRMetricRecord> results = new ArrayList<>();

        if(!FormatUtil.isDataCategory(tsdrMetricKey)){

            if(!FormatUtil.isValidTSDRKey(tsdrMetricKey)){
                log.error("TSDR Metric Key {} is not in the correct format",tsdrMetricKey);
                return results;
            }

            String dataCategory = FormatUtil.getDataCategoryFromTSDRKey(tsdrMetricKey);

            if(!FormatUtil.isDataCategory(dataCategory)){
                log.error("Data Category is unknown {}",dataCategory);
                return results;
            }

            StringBuffer sb = new StringBuffer();
            sb.append("select * from Metric where metrictimestamp ")
              .append("between '")
              .append(startDateTime)
              .append("' and '")
              .append(endDateTime)
              .append("' and metricdetails = '")
              .append(tsdrMetricKey)
              .append("'")
              .append(" order by metrictimestamp");
            log.info("getMetricsFilteredByCategory with start date and end date: query being sent is "+ sb.toString());
            Query nativeQuery = em.createNativeQuery(sb.toString(),Metric.class);
            List<Metric> metrics = nativeQuery.getResultList();
            for(Metric m:metrics){
                results.add(getTSDRMetricRecord(m));
            }
            return results;
        }else{
            StringBuffer sb = new StringBuffer();
            sb.append("select * from Metric where metrictimestamp ")
              .append("between '")
              .append(startDateTime)
              .append("' and '")
              .append(endDateTime)
              .append("' and metriccategory = '")
              .append(tsdrMetricKey.toUpperCase())
              .append("'")
              .append(" order by metrictimestamp desc");
            log.info("getMetricsFilteredByCategory with start date and end date: query being sent is "+ sb.toString());
            Query nativeQuery = em.createNativeQuery(sb.toString(),Metric.class);
            List<Metric> metrics = nativeQuery.getResultList();
            for(Metric m:metrics){
                results.add(getTSDRMetricRecord(m));
            }
            return results;
        }
    }

    private static final TSDRMetricRecord getTSDRMetricRecord(Metric entry){
        TSDRMetricRecordBuilder rb = new TSDRMetricRecordBuilder();
        rb.setMetricName(entry.getMetricName());
        rb.setMetricValue(new BigDecimal(entry.getMetricValue()));
        rb.setNodeID(entry.getNodeId());
        rb.setRecordKeys(FormatUtil.getRecordKeysFromTSDRKey(entry.getMetricDetails()));
        rb.setTimeStamp(entry.getMetricTimeStamp());
        rb.setTSDRDataCategory(DataCategory.valueOf(entry.getMetricCategory()));
        return rb.build();
    }

    @Override public void close() {
        try{
            em.close();
            em.clear();
        }catch(Exception e){
            log.warn("Exception occurred when closing the persistence store",e);
        }
    }

    @Override public void purge(DataCategory category, long retentionTime) {
        Preconditions.checkArgument(em != null, "EntityManager found to be null");
        try {
            StringBuffer query = new StringBuffer();
            query.append("delete from Metric where metrictimestamp")
                 .append(" <= '")
                 .append(FormatUtil.getFormattedTimeStamp(new Date(retentionTime).getTime(),
                     FormatUtil.QUERY_TIMESTAMP))
                 .append("' and metriccategory = '")
                 .append(category.toString())
                 .append("'");



            em.createQuery(query.toString()).executeUpdate();
            em.flush();

        }catch(Exception e){
            log.error("TsdrJpaServiceImpl:purge", e);
        }

    }

    @Override public void purgeAll(long retentionTime) {
        try {
            StringBuffer query = new StringBuffer();
            query.append("delete from Metric where metrictimestamp")
                .append(" <= '")
                .append(FormatUtil.getFormattedTimeStamp(new Date(retentionTime).getTime(),
                    FormatUtil.QUERY_TIMESTAMP))
                .append("'");

            em.createQuery(query.toString()).executeUpdate();
            em.flush();

        }catch(Exception e){
            log.error("TsdrJpaServiceImpl:purgeAll", e);
        }


    }

    public void setEntityManager(EntityManager em) {
        this.em = em;
    }

}
