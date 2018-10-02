/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.netflow.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.tsdrlog.RecordAttributes;

/**
 * Netflow version 5 packet parser - see http://netflow.caligare.com/netflow_v5.htm.
 *
 * @author Thomas Pantelis
 */
class NetflowV5PacketParser extends AbstractNetflowPacketParser {
    private static final int FLOW_SIZE = 48;

    NetflowV5PacketParser(byte[] data, int initialPosition) {
        super(data, 5, initialPosition);

        addHeaderAttribute("unix_nsecs", parseIntString());
        addHeaderAttribute("flow_sequence", parseIntString());
        addHeaderAttribute("engine_type", parseByteString());
        addHeaderAttribute("engine_id", parseByteString());

        // sampling interval is 14 bits
        addHeaderAttribute("sampling_interval", Integer.toString(parseShort() & 0x3ff));
    }

    @Override
    public void parseRecords(Consumer<List<RecordAttributes>> callback) {
        for (int i = 0; i < totalRecordCount(); i++) {
            parseNextRecord(callback);
        }
    }

    private void parseNextRecord(Consumer<List<RecordAttributes>> callback) {
        final int start = position();

        List<RecordAttributes> record = new ArrayList<>(headerAttributes());
        record.add(newRecordAttributes("srcaddr", parseIPv4Address()));
        record.add(newRecordAttributes("dstaddr", parseIPv4Address()));
        record.add(newRecordAttributes("nexthop", parseIPv4Address()));
        record.add(newRecordAttributes("input", parseShortString()));
        record.add(newRecordAttributes("output", parseShortString()));
        record.add(newRecordAttributes("dPkts", parseIntString()));
        record.add(newRecordAttributes("dOctets", parseIntString()));

        long first = parseInt();
        record.add(newRecordAttributes("first", Long.toString(first)));
        long last = parseInt();
        record.add(newRecordAttributes("last", Long.toString(last)));

        record.add(newRecordAttributes("srcport", parseShortString()));
        record.add(newRecordAttributes("dstport", parseShortString()));

        skip(1);
        record.add(newRecordAttributes("tcp_flags", parseByteString()));
        record.add(newRecordAttributes("prot", parseByteString()));
        record.add(newRecordAttributes("tos", parseByteString()));
        record.add(newRecordAttributes("src_as", parseShortString()));
        record.add(newRecordAttributes("dst_as", parseShortString()));
        record.add(newRecordAttributes("src_mask", parseByteString()));
        record.add(newRecordAttributes("dst_mask", parseByteString()));
        record.add(newRecordAttributes("flow_duration", Long.toString(last - first)));

        skip(FLOW_SIZE - (position() - start));

        callback.accept(record);
    }
}
