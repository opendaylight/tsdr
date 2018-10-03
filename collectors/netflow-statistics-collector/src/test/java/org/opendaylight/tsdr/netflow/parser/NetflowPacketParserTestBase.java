/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.netflow.parser;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.tsdrlog.RecordAttributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecordBuilder;

/**
 * Base class for netflow parsing tests.
 *
 * @author Thomas Pantelis
 */
public class NetflowPacketParserTestBase {
    protected final NetflowPacketParserFactory factory = new NetflowPacketParserFactory();

    protected List<TSDRLogRecord> parseRecords(byte[] data) {
        NetflowPacketParser parser = factory.newInstance(data);

        final List<TSDRLogRecord> records = new ArrayList<>();
        parser.parseRecords((attrs, text) -> records.add(new TSDRLogRecordBuilder().setRecordAttributes(attrs)
                .setRecordFullText(text).build()));
        return records;
    }

    protected static void assertEmpty(Map<String, String> attrs) {
        assertTrue("Unexpected record attributes: " + attrs, attrs.isEmpty());
    }

    protected static Map<String, String> toMap(List<RecordAttributes> attrs) {
        Map<String, String> map = new HashMap<>();
        attrs.forEach(rec -> map.put(rec.getName(), rec.getValue()));
        return map;
    }
}
