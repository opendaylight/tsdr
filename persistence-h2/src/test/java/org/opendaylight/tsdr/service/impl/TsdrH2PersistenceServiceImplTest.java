/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.service.impl;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.tsdr.entity.Metric;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Counter64;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Tests the persistence of the TSDR Model in default JPA store
 * @author <a href="mailto:syedbahm@cisco.com">Basheeruddin Ahmed</a>
 */

public class TsdrH2PersistenceServiceImplTest {

    @Test
    public void testStore() throws Exception {


        TsdrJpaServiceImpl tsdrJpaService = new TsdrJpaServiceImpl();
        TsdrH2PersistenceServiceImpl tsdrH2PersistenceService = new TsdrH2PersistenceServiceImpl();
        //we need to create the EntityManager ourselves and pass it to the service
        EntityManagerFactory emf = Persistence
            .createEntityManagerFactory("metric");
        EntityManager em = emf.createEntityManager();
        tsdrJpaService.setEntityManager(em);
        tsdrH2PersistenceService.setJpaService(tsdrJpaService);

        em.getTransaction().begin();

        RecordKeys recordKeys = new RecordKeysBuilder()
            .setKeyName("recordKeyName")
            .setKeyValue("recordKeyValue").build();

        List<RecordKeys> recordKeysList= new ArrayList<RecordKeys>();
        recordKeysList.add(recordKeys);
        String timeStamp = (new Long((new Date()).getTime())).toString();

        TSDRMetricRecordBuilder tsdrMetricBuilder = new TSDRMetricRecordBuilder();
        TSDRMetricRecord tsdrMetrics = tsdrMetricBuilder.setMetricName("METRIC_NAME")
            .setMetricValue(new Counter64(new BigInteger("64")))
            .setNodeID("openflow:dummy")
            .setRecordKeys(recordKeysList)
            .setTimeStamp(new BigInteger(timeStamp))
            .setTSDRDataCategory(DataCategory.FLOWSTATS).build();

        tsdrH2PersistenceService.store(
            tsdrMetrics);
        em.getTransaction().commit();

        //now let us try to get the saved metric
        List<Metric>metricList = tsdrJpaService.getAll(1000);
        Assert.assertEquals(1, metricList.size());
        Assert.assertEquals("METRIC_NAME", metricList.get(0).getMetricName());
        Assert.assertEquals(64.0,metricList.get(0).getMetricValue(),0.02);
        Assert.assertEquals("openflow:dummy",metricList.get(0).getNodeId());
        Assert.assertEquals("recordKeyName_recordKeyValue_",metricList.get(0).getInfo());;
        Assert.assertEquals(new Date(new BigInteger(timeStamp).longValue()).toString(),metricList.get(0).getMetricTimeStamp().toString());
        Assert.assertEquals(DataCategory.FLOWSTATS.toString(),metricList.get(0).getMetricCategory());

    }


}
