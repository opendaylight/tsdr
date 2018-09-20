/*
 * Copyright (c) 2016 TethrNet Technology Co.Ltd and others.  All rights reserved.
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.syslogs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.opendaylight.tsdr.syslogs.server.decoder.Message;
import org.opendaylight.tsdr.syslogs.server.decoder.MessageDecoder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.Facility;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.Severity;

/**
 * Test of MessageDecoder.
 *
 * @author Kun Chen(kunch@tethrnet.com)
 */
public class MessgeDecoderTest {

    /**
     * Test whether the formatted message could be decoded
     * correctly.
     */
    @Test
    public void testMessageDecoder() {
        String str = "<30>1:quentin:May 24 12:22:25:TestProcess[1787]:%3-6-1:This is a test log of cisco.";

        assertTrue(MessageDecoder.matches(str));

        Message message = MessageDecoder.decode(str);

        assertEquals("TestProcess", message.getApplicationName());
        assertEquals("1 : This is a test log of cisco.", message.getContent());
        assertEquals("1787", message.getProcessId());
        assertEquals("1", message.getSequenceId());
        assertEquals("May 24 12:22:25", message.getTimestamp());
        assertEquals("quentin", message.getHostname());
        assertEquals(Facility.SYSTEMDAEMON, message.getFacility());
        assertEquals(Severity.INFORMATION, message.getSeverity());
    }

    /**
     * If the message is unrecognizable,
     * it should be detected.
     */
    @Test
    public void testUnrecognizableMessage() {
        String str = "Simple string.";
        assertTrue(!MessageDecoder.matches(str));
    }
}
