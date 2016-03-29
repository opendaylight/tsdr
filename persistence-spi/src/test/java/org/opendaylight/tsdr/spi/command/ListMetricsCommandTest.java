/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.spi.command;

import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.tsdr.spi.persistence.TSDRLogPersistenceService;
import org.opendaylight.tsdr.spi.persistence.TSDRMetricPersistenceService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author saichler@gmail.com
 **/
public class ListMetricsCommandTest {

    public static TSDRLogRecord createLogRecord(){
        TSDRLogRecordBuilder b = new TSDRLogRecordBuilder();
        b.setNodeID("Test");
        b.setTimeStamp(System.currentTimeMillis());
        b.setTSDRDataCategory(DataCategory.EXTERNAL);
        b.setRecordFullText("Some syslog text");
        List<RecordKeys> recs = new ArrayList<>();
        RecordKeysBuilder rb = new RecordKeysBuilder();
        rb.setKeyValue("Test1");
        rb.setKeyName("Test2");
        recs.add(rb.build());
        b.setRecordKeys(recs);
        return b.build();
    }

    public static TSDRMetricRecord createMetricRecord(){
        TSDRMetricRecordBuilder b = new TSDRMetricRecordBuilder();
        b.setNodeID("Test");
        b.setTimeStamp(System.currentTimeMillis());
        b.setMetricName("Test");
        b.setMetricValue(new BigDecimal(11D));
        b.setTSDRDataCategory(DataCategory.EXTERNAL);
        List<RecordKeys> recs = new ArrayList<>();
        RecordKeysBuilder rb = new RecordKeysBuilder();
        rb.setKeyValue("Test1");
        rb.setKeyName("Test2");
        recs.add(rb.build());
        b.setRecordKeys(recs);
        return b.build();
    }


    @Test
    public void test() throws Exception {
        ListMetricsCommand cmd = new ListMetricsCommand();
        cmd.category = "EXTERNAL";
        cmd.doExecute();
        TSDRMetricPersistenceService metricService = Mockito.mock(TSDRMetricPersistenceService.class);
        TSDRLogPersistenceService logService = Mockito.mock(TSDRLogPersistenceService.class);
        ListMetricsCommand.metricService = metricService;
        ListMetricsCommand.logService = logService;
        cmd.doExecute();
        List<TSDRMetricRecord> metric = new ArrayList<>();
        metric.add(createMetricRecord());
        Mockito.when(metricService.getTSDRMetricRecords(Mockito.anyString(),Mockito.anyLong(),Mockito.anyLong())).thenReturn(metric);
        cmd.doExecute();

        List<TSDRLogRecord> logs = new ArrayList<>();
        logs.add(createLogRecord());
        Mockito.when(logService.getTSDRLogRecords(Mockito.anyString(),Mockito.anyLong(),Mockito.anyLong())).thenReturn(logs);
        cmd.category = DataCategory.LOGRECORDS.name();
        cmd.doExecute();
        cmd.startDateTime = "10/10/2010 22:22:22";
        cmd.endDateTime = "10/10/2010 22:23:22";
        cmd.doExecute();
        cmd.startDateTime = "10/10/2010 22:22:22";
        cmd.endDateTime = "10/10/2010 22:22:22";
        cmd.doExecute();
        cmd.getDate("10/10/2010 22:22:22");
        cmd.getDate(null);
        try {
            cmd.getDate("10/10/2010 22:2222");
        }catch(NullPointerException e){

        }
    }
}
