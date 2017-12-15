/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.spi.command;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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

public class ListMetricsCommandTest {

    public static TSDRLogRecord createLogRecord() {
        TSDRLogRecordBuilder builder = new TSDRLogRecordBuilder();
        builder.setNodeID("Test");
        builder.setTimeStamp(System.currentTimeMillis());
        builder.setTSDRDataCategory(DataCategory.EXTERNAL);
        builder.setRecordFullText("Some syslog text");
        List<RecordKeys> recs = new ArrayList<>();
        RecordKeysBuilder rb = new RecordKeysBuilder();
        rb.setKeyValue("Test1");
        rb.setKeyName("Test2");
        recs.add(rb.build());
        builder.setRecordKeys(recs);
        return builder.build();
    }

    public static TSDRMetricRecord createMetricRecord() {
        TSDRMetricRecordBuilder builder = new TSDRMetricRecordBuilder();
        builder.setNodeID("Test");
        builder.setTimeStamp(System.currentTimeMillis());
        builder.setMetricName("Test");
        builder.setMetricValue(new BigDecimal(11D));
        builder.setTSDRDataCategory(DataCategory.EXTERNAL);
        List<RecordKeys> recs = new ArrayList<>();
        RecordKeysBuilder rb = new RecordKeysBuilder();
        rb.setKeyValue("Test1");
        rb.setKeyName("Test2");
        recs.add(rb.build());
        builder.setRecordKeys(recs);
        return builder.build();
    }


    @Test
    public void test() throws Exception {
        TSDRMetricPersistenceService metricService = Mockito.mock(TSDRMetricPersistenceService.class);
        TSDRLogPersistenceService logService = Mockito.mock(TSDRLogPersistenceService.class);
        ListMetricsCommand cmd = new ListMetricsCommand(metricService, logService);
        cmd.category = "EXTERNAL";
        cmd.doExecute();
        cmd.doExecute();
        List<TSDRMetricRecord> metric = new ArrayList<>();
        metric.add(createMetricRecord());
        Mockito.when(metricService.getTSDRMetricRecords(Mockito.anyString(),Mockito.anyLong(),Mockito.anyLong()))
                .thenReturn(metric);
        cmd.doExecute();

        List<TSDRLogRecord> logs = new ArrayList<>();
        logs.add(createLogRecord());
        Mockito.when(logService.getTSDRLogRecords(Mockito.anyString(),Mockito.anyLong(),Mockito.anyLong()))
                .thenReturn(logs);
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
        cmd.getDate("10/10/2010 22:2222");
    }
}
