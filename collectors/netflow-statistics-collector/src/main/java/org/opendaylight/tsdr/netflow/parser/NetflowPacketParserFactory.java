/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.netflow.parser;

import java.util.function.Consumer;
import org.opendaylight.tsdr.netflow.parser.v5.NetflowV5PacketParser;
import org.opendaylight.tsdr.netflow.parser.v9.NetflowPacketV9ParserFactory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecordBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating NetflowPacketParser instances.
 *
 * @author Thomas Pantelis
 */
public class NetflowPacketParserFactory {
    private static final Logger LOG = LoggerFactory.getLogger(NetflowPacketParserFactory.class);

    private final NetflowPacketV9ParserFactory v9ParserFactory = new NetflowPacketV9ParserFactory();

    public NetflowPacketParser newInstance(final byte[] bytes, String sourceIP, TSDRLogRecordBuilder recordBuilder,
            Consumer<TSDRLogRecordBuilder> callback) {
        int version = (int) AbstractNetflowPacketParser.parseLong(bytes, 0, 2);
        switch (version) {
            case 5:
                return new NetflowV5PacketParser(bytes, 2, recordBuilder, callback);
            case 9:
                return v9ParserFactory.newInstance(bytes, sourceIP, recordBuilder, callback);
            default:
                LOG.warn("Received netflow packet with unknown/unsupported version {}", version);
                return () -> { };
        }
    }
}
