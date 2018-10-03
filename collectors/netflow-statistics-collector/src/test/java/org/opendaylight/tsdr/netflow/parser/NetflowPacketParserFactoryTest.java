/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.netflow.parser;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.junit.Test;

/**
 * Unit tests for NetflowPacketParserFactory.
 *
 * @author Thomas Pantelis
 */
public class NetflowPacketParserFactoryTest extends NetflowPacketParserTestBase {
    @Test
    public void testUnknownVersion() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bos);

        // Header
        out.writeShort(1);        // version
        out.writeShort(0);        // count
        out.writeInt(29115718);   // sys_uptime
        out.writeInt(1470119622); // unix_secs
        out.close();

        assertEquals(0, parseRecords(bos.toByteArray()).size());
    }
}
