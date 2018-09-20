/*
 * Copyright (c) 2016 TethrNet Technology Co.Ltd and others.  All rights reserved.
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.syslogs;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.tsdr.syslogs.server.datastore.MessageFilter;
import org.opendaylight.tsdr.syslogs.server.decoder.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.syslog.filter.Filter;

/**
 * Test of message filter.
 *
 * @author Kun Chen(kunch@tethrnet.com)
 */
public class MessageFilterTest {
    @Test
    public void testFilterMatch() {
        Filter entity = mock(Filter.class);
        when(entity.getApplication()).thenReturn(".*");
        when(entity.getContent()).thenReturn("cisco");
        when(entity.getFacility()).thenReturn(null);
        when(entity.getHost()).thenReturn(".*");
        when(entity.getPid()).thenReturn(".*");
        when(entity.getSeverity()).thenReturn(null);
        when(entity.getSid()).thenReturn(".*");

        MessageFilter filter = MessageFilter.from(entity);
        Message msg = Message.MessageBuilder.create()
                .facility(null)
                .severity(null)
                .hostname(".*")
                .applicationName(".*")
                .processId(".*")
                .sequenceId(".*")
                .content("cisco\n")
                .build();
        Assert.assertTrue(filter.matches(msg));

    }
}
