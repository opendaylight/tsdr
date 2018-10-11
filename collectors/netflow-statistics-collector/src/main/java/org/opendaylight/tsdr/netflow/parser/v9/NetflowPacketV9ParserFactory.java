/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.netflow.parser.v9;

import java.util.function.Consumer;
import org.opendaylight.tsdr.netflow.parser.MissingTemplateCache;
import org.opendaylight.tsdr.netflow.parser.NetflowPacketParser;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecordBuilder;

/**
 * Factory for creating NetflowPacketParser v9 instances.
 *
 * @author Thomas Pantelis
 */
public class NetflowPacketV9ParserFactory {
    private final FlowsetTemplateCache flowsetTemplateCache = new FlowsetTemplateCache();
    private final OptionsTemplateCache optionsTemplateCache = new OptionsTemplateCache();
    private final MissingTemplateCache missingTemplateCache = new MissingTemplateCache(
        templateKey -> flowsetTemplateCache.contains(templateKey) || optionsTemplateCache.contains(templateKey));

    public NetflowPacketParser newInstance(final byte[] bytes, String sourceIP, TSDRLogRecordBuilder recordBuilder,
            Consumer<TSDRLogRecordBuilder> callback) {
        return new NetflowV9PacketParser(bytes, 2, sourceIP, flowsetTemplateCache, optionsTemplateCache,
                missingTemplateCache, recordBuilder, callback);
    }
}
