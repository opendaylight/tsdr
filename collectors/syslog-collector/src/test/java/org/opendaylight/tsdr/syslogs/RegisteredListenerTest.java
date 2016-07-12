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
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.tsdr.syslogs.server.datastore.RegisteredListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.RegisterFilterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.RegisterFilterInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.RegisterFilterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.SyslogDispatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.SyslogListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.SyslogListenerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.SyslogListenerKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.*;

/**
 * Test for RegisteredListener.
 * @author Wei Lai(weilai@tethrnet.com)
 */
public class RegisteredListenerTest {
    private DataBroker dataBroker = mock(DataBroker.class);
    private RegisteredListener registeredListener = new RegisteredListener(dataBroker,"123","http://localhost:9001/server");
    private RegisteredListener registeredListener2 = new RegisteredListener(dataBroker,"123",null);

    private ListenerRegistration<DataChangeListener> listener = mock(ListenerRegistration.class);
    InstanceIdentifier<SyslogListener> iid = InstanceIdentifier.create(SyslogDispatcher.class)
            .child(SyslogListener.class, new SyslogListenerKey("123"));
    private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change = mock(AsyncDataChangeEvent.class);
    private OutputStreamWriter out = mock(OutputStreamWriter.class);
    private URLConnection urlConnection = mock(URLConnection.class);
    private OutputStream outputStram = mock(OutputStream.class );




    @Before
    public void mockSetUp() {

    }

    @Test
    public void testCloseWithException() {
        when(dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, iid,
                registeredListener, AsyncDataBroker.DataChangeScope.SUBTREE)).thenReturn(listener);
        RuntimeException closeException = mock(RuntimeException.class);
        doThrow(closeException).when(listener).close();

        registeredListener.listen();

        Assert.assertEquals(false,registeredListener.close());

    }


    @Test
    public void testCloseSuccessfully() {

        when(dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, iid,
                registeredListener, AsyncDataBroker.DataChangeScope.SUBTREE)).thenReturn(listener);

        doNothing().when(listener).close();

        registeredListener.listen();
        Assert.assertEquals(true,registeredListener.close());
    }

    @Test
    public void testOndataChangedWithNoCallbcakURL() throws IOException {

        when(dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, iid,
                registeredListener, AsyncDataBroker.DataChangeScope.SUBTREE)).thenReturn(listener);
        SyslogListener syslogListener = new SyslogListenerBuilder()
                .setListenerId("321")
                .setSyslogMessage("cisco")
                .build();
        Map<InstanceIdentifier<?>,DataObject> map = new HashMap<>();
        map.put(iid,syslogListener);
        when(change.getUpdatedData()).thenReturn(map);

        registeredListener2.onDataChanged(change);
        Assert.assertNotNull(syslogListener);
    }





}
