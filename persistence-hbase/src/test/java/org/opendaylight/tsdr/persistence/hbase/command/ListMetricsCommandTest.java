/*
 * Copyright (c) 2015 xFlow Research Inc. and others.  All rights reserved
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hbase.command;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.tsdr.persistence.hbase.CreateTableTask;
import org.opendaylight.tsdr.persistence.hbase.HBaseDataStore;
import org.opendaylight.tsdr.persistence.hbase.HBaseDataStoreFactory;
import org.opendaylight.tsdr.persistence.hbase.HBaseEntity;
import org.opendaylight.tsdr.spi.model.TSDRConstants;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRLog;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRMetric;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrlogrecord.input.TSDRLogRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;

import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import static org.mockito.Matchers.any;


/**
 * @author <a href="mailto:chaudhry.usama@xflowresearch.com">Chaudhry Muhammad Usama</a>
 */

public class ListMetricsCommandTest {
    public ListMetricsCommand listMetricsCommand = null;

    @Before
    public void setup() {
        listMetricsCommand = new ListMetricsCommand();
   }
    @Test
    public void testListMetrics() {
        String timeStamp = (new Long((new Date()).getTime())).toString();
        List<RecordKeys> recordKeys = new ArrayList<RecordKeys>();
        RecordKeys recordKey = new RecordKeysBuilder()
            .setKeyName(TSDRConstants.FLOW_TABLE_KEY_NAME)
            .setKeyValue("table1").build();
        recordKeys.add(recordKey);
        TSDRMetricRecordBuilder builder1 = new TSDRMetricRecordBuilder();
        TSDRMetric tsdrMetric1 =   builder1.setMetricName("PacketsMatched")
                .setMetricValue(new BigDecimal(Double.parseDouble("20000000")))
                .setNodeID("node1")
                .setRecordKeys(recordKeys)
                .setTSDRDataCategory(DataCategory.FLOWTABLESTATS)
                .setTimeStamp(new Long(timeStamp)).build();
        List<TSDRMetricRecord> metricslist = new ArrayList<TSDRMetricRecord>();
        metricslist.add((TSDRMetricRecord)tsdrMetric1);
        System.out.println(listMetricsCommand.listMetrics(metricslist));
        String buffer = listMetricsCommand.listMetrics(metricslist);
        Assert.assertTrue(buffer.contains("MN=PacketsMatched"));
        Assert.assertTrue(buffer.contains("NID=node1"));
        Assert.assertTrue(buffer.contains("DC=FLOWTABLESTATS"));
        Assert.assertTrue(buffer.contains("RK=TableID:table1"));
        Assert.assertTrue(buffer.contains("20000000"));
    }

    @Test
    public void testListLogs() {
        String timeStamp = (new Long((new Date()).getTime())).toString();
        List<RecordKeys> recordKeys = new ArrayList<RecordKeys>();
        RecordKeys recordKey1 = new RecordKeysBuilder()
            .setKeyName(DataCategory.SYSLOG.name())
            .setKeyValue("log1").build();
        recordKeys.add(recordKey1);
        TSDRLogRecordBuilder builder1 = new TSDRLogRecordBuilder();
        TSDRLog tsdrLog1 =   builder1.setIndex(1)
            .setRecordFullText("su root failed for lonvick")
            .setNodeID("node1.example.com")
            .setRecordKeys(recordKeys)
            .setTSDRDataCategory(DataCategory.SYSLOG)
            .setTimeStamp(new Long(timeStamp)).build();
        List<org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrlogrecord.input.TSDRLogRecord> logs = new ArrayList<TSDRLogRecord>();
        logs.add((TSDRLogRecord)tsdrLog1);
        System.out.println(listMetricsCommand.listLogs(logs));
        String buffer = listMetricsCommand.listLogs(logs);
        Assert.assertTrue(buffer.contains("su root failed for lonvick"));
        Assert.assertTrue(buffer.contains("NID=node1.example.com"));
        Assert.assertTrue(buffer.contains("RK=SYSLOG:log1"));
        Assert.assertTrue(buffer.contains("DC=SYSLOG"));
    }

    @After
    public void teardown(){
        listMetricsCommand = null;
    }
}