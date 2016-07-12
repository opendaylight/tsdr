/*
 * Copyright (c) 2016 TethrNet Technology Co.Ltd and others.  All rights reserved.
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.syslogs;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.tsdr.syslogs.server.decoder.Message;
import org.opendaylight.tsdr.syslogs.server.decoder.MessageDecoder;

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

        Assert.assertTrue(MessageDecoder.matches(str));

        Message message = MessageDecoder.decode(str);

        Assert.assertEquals("TestProcess",message.getApplicationName());
        Assert.assertEquals("1 : This is a test log of cisco.",message.getContent());
        Assert.assertEquals("1787",message.getProcessId());
        Assert.assertEquals("1",message.getSequenceId());
        Assert.assertEquals("May 24 12:22:25",message.getTimestamp());
        Assert.assertEquals("quentin",message.getHostname());
        Assert.assertEquals(Message.Facility.SYSTEM_DAEMON,message.getFacility());
        Assert.assertEquals(Message.Severity.INFORMATION,message.getSeverity());
        Assert.assertEquals("Message{facility=SYSTEM_DAEMON, " +
                "severity=INFORMATION, sequenceId='1', " +
                "timestamp='May 24 12:22:25', hostname='quentin', " +
                "applicationName='TestProcess', processId='1787', " +
                "content='1 : This is a test log of cisco.'}",message.toString());
    }

    /**
     * If the message is unrecognizable,
     * it should be detected.
     */
    @Test
    public void testUnrecognizableMessage() {
        String str = "Simple string.";
        MessageDecoder decoder = new MessageDecoder();

        Assert.assertTrue(!decoder.matches(str));
    }
}
