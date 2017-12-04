/*
 * Copyright (c) 2016 TethrNet Technology Co.Ltd and others.  All rights reserved.
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.syslogs;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.tsdr.syslogs.server.datastore.RegisteredListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.SyslogDispatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.SyslogListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.SyslogListenerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.SyslogListenerKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Test for RegisteredListener.
 * @author Wei Lai(weilai@tethrnet.com)
 */
public class RegisteredListenerTest {
    private final DataBroker dataBroker = mock(DataBroker.class);

    private final ListenerRegistration<?> listener = mock(ListenerRegistration.class);
    InstanceIdentifier<SyslogListener> iid = InstanceIdentifier.create(SyslogDispatcher.class)
            .child(SyslogListener.class, new SyslogListenerKey("123"));

    @Before
    public void mockSetUp() {
    }

    @Test
    public void testCloseSuccessfully() {
        RegisteredListener registeredListener = new RegisteredListener(dataBroker,"123","http://localhost:9001/server");

        doReturn(listener).when(dataBroker).registerDataTreeChangeListener(new DataTreeIdentifier<>(
                LogicalDatastoreType.OPERATIONAL, iid), registeredListener);

        doNothing().when(listener).close();

        registeredListener.listen();
        Assert.assertEquals(true,registeredListener.close());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOndataChangedWithNoCallbackURL() throws IOException {
        RegisteredListener registeredListener = new RegisteredListener(dataBroker,"123", null);

        doReturn(listener).when(dataBroker).registerDataTreeChangeListener(new DataTreeIdentifier<>(
                LogicalDatastoreType.OPERATIONAL, iid), registeredListener);

        DataTreeModification<SyslogListener> mockDataTreeModification = mock(DataTreeModification.class);
        DataObjectModification<SyslogListener> mockModification = mock(DataObjectModification.class);
        doReturn(mockModification).when(mockDataTreeModification).getRootNode();

        SyslogListener syslogListener = new SyslogListenerBuilder()
                .setListenerId("321")
                .setSyslogMessage("cisco")
                .build();

        doReturn(syslogListener).when(mockModification).getDataAfter();
        doReturn(DataObjectModification.ModificationType.WRITE).when(mockModification).getModificationType();

        registeredListener.onDataTreeChanged(Collections.singleton(mockDataTreeModification));
    }
}
