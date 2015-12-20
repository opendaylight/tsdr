/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.cassandra.command;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;

/**
 * @author <a href="mailto:saichler@gmail.com">Sharon Aicler</a>
 * Created by saichler on 12/16/15.
 */
public class CommandListTest {

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
    public void testListMetricCommand() throws Exception {
        ListMetricsCommand c = new ListMetricsCommand();
        List<TSDRMetricRecord> recs = new ArrayList<>();
        recs.add(createMetricRecord());
        c.listMetrics(recs);
    }
}