/*
 * Copyright (c) 2016 Frinx s.r.o. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.persistence.elasticsearch;

import com.google.common.collect.Lists;
import java.math.BigDecimal;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.binary.data.rev160325.storetsdrbinaryrecord.input.TSDRBinaryRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.binary.data.rev160325.storetsdrbinaryrecord.input.TSDRBinaryRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;

/**
 * A utility class for helping records creation.
 *
 * @author Lukas Beles(lbeles@frinx.io)
 */
final class TsdrRecordFactory {
    private TsdrRecordFactory() {
        throw new AssertionError();
    }

    /**
     * Create the fake {@link TSDRMetricRecord}.
     */
    static TSDRMetricRecord createMetricRecord() {
        return new TSDRMetricRecordBuilder()
                .setNodeID("TestNodeID")
                .setTimeStamp(1257894000000000000L)
                .setTSDRDataCategory(DataCategory.EXTERNAL)
                .setMetricName("TestName")
                .setMetricValue(new BigDecimal(8128))
                .setRecordKeys(Lists.newArrayList(new RecordKeysBuilder()
                        .setKeyName("TestRKName")
                        .setKeyValue("TestRKValue")
                        .build()))
                .build();
    }

    /**
     * Create the fake {@link TSDRLogRecord}.
     */
    static TSDRLogRecord createLogRecord() {
        return new TSDRLogRecordBuilder()
                .setNodeID("TestNodeID")
                .setTimeStamp(1257894000000000000L)
                .setTSDRDataCategory(DataCategory.EXTERNAL)
                .setRecordFullText("Test Text")
                .setRecordKeys(Lists.newArrayList(new RecordKeysBuilder()
                        .setKeyName("TestRKName")
                        .setKeyValue("TestRKValue")
                        .build()))
                .build();
    }

    /**
     * Create the fake {@link TSDRBinaryRecord}.
     */
    static TSDRBinaryRecord createBinaryRecord() {
        return new TSDRBinaryRecordBuilder()
                .setNodeID("TestNodeID")
                .setTimeStamp(1257894000000000000L)
                .setTSDRDataCategory(DataCategory.EXTERNAL)
                .setData("Test Data".getBytes())
                .setRecordKeys(Lists.newArrayList(new RecordKeysBuilder()
                        .setKeyName("TestRKName")
                        .setKeyValue("TestRKValue")
                        .build()))
                .build();
    }
}
