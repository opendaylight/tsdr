/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.netflow.parser.v5;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import org.opendaylight.tsdr.netflow.parser.AbstractNetflowPacketParser;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.tsdrlog.RecordAttributes;

/**
 * Netflow version 5 packet parser - see http://netflow.caligare.com/netflow_v5.htm.
 *
 * @author Thomas Pantelis
 */
public class NetflowV5PacketParser extends AbstractNetflowPacketParser {
    private static final int FLOW_SIZE = 48;

    public NetflowV5PacketParser(byte[] data, int initialPosition) {
        super(data, 5, initialPosition);

        addHeaderAttribute("unix_nsecs", parseIntString());
        addHeaderAttribute("flow_sequence", parseIntString());
        addHeaderAttribute("engine_type", parseByteString());
        addHeaderAttribute("engine_id", parseByteString());

        // sampling interval is 14 bits
        addHeaderAttribute("sampling_interval", Integer.toString(parseShort() & 0x3ff));
    }

    @Override
    public void parseRecords(BiConsumer<List<RecordAttributes>, String> callback) {
        for (int i = 0; i < totalRecordCount(); i++) {
            parseNextRecord(callback);
        }
    }

    private void parseNextRecord(BiConsumer<List<RecordAttributes>, String> callback) {
        final int start = position();

        List<RecordAttributes> recordAttrs = new ArrayList<>(headerAttributes());
        recordAttrs.add(newRecordAttributes("srcaddr", parseIPv4Address()));
        recordAttrs.add(newRecordAttributes("dstaddr", parseIPv4Address()));
        recordAttrs.add(newRecordAttributes("nexthop", parseIPv4Address()));
        recordAttrs.add(newRecordAttributes("input", parseShortString()));
        recordAttrs.add(newRecordAttributes("output", parseShortString()));
        recordAttrs.add(newRecordAttributes("dPkts", parseIntString()));
        recordAttrs.add(newRecordAttributes("dOctets", parseIntString()));

        long first = parseInt();
        recordAttrs.add(newRecordAttributes("first", Long.toString(first)));
        long last = parseInt();
        recordAttrs.add(newRecordAttributes("last", Long.toString(last)));

        recordAttrs.add(newRecordAttributes("srcport", parseShortString()));
        recordAttrs.add(newRecordAttributes("dstport", parseShortString()));

        skip(1);
        recordAttrs.add(newRecordAttributes("tcp_flags", parseByteString()));
        recordAttrs.add(newRecordAttributes("prot", parseByteString()));
        recordAttrs.add(newRecordAttributes("tos", parseByteString()));
        recordAttrs.add(newRecordAttributes("src_as", parseShortString()));
        recordAttrs.add(newRecordAttributes("dst_as", parseShortString()));
        recordAttrs.add(newRecordAttributes("src_mask", parseByteString()));
        recordAttrs.add(newRecordAttributes("dst_mask", parseByteString()));
        recordAttrs.add(newRecordAttributes("flow_duration", Long.toString(last - first)));

        skip(FLOW_SIZE - (position() - start));

        callback.accept(recordAttrs, "Flow record");
    }
}
