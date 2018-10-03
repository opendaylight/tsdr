/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.netflow.parser.v9;

import org.opendaylight.tsdr.netflow.parser.NetflowPacketParser;

/**
 * Factory for creating NetflowPacketParser v9 instances.
 *
 * @author Thomas Pantelis
 */
public class NetflowPacketV9ParserFactory {
    private final FlowsetTemplateCache flowsetTemplateCache = new FlowsetTemplateCache();
    private final OptionsTemplateCache optionsTemplateCache = new OptionsTemplateCache();

    public NetflowPacketParser newInstance(final byte[] bytes) {
        return new NetflowV9PacketParser(bytes, 2, flowsetTemplateCache, optionsTemplateCache);
    }
}
