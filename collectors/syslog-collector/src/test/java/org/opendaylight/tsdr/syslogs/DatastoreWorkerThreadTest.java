/*
 * Copyright (c) 2016 TethrNet Technology Co.Ltd and others.  All rights reserved.
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.syslogs;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.tsdr.syslogs.server.datastore.MessageFilter;
import org.opendaylight.tsdr.syslogs.server.datastore.SyslogDatastoreManager;
import org.opendaylight.tsdr.syslogs.server.decoder.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.SyslogDispatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.SyslogFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.SyslogFilterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.SyslogFilterKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.syslog.filter.FilterEntity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.syslog.filter.FilterEntityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.syslog.filter.Listener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.syslog.filter.ListenerBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This is the test of DatastoreWorkerThread.
 * @author Wei Lai(weilai@tetthrnet.com)
 */
public class DatastoreWorkerThreadTest {
    private int coreThreadPoolSize = 5;
    private int maxThreadPoolSize = 10;
    private long keepAliveTime = 10L;
    private int queueSize = 10;
    private SyslogDatastoreManager manager = SyslogDatastoreManager.getInstance(coreThreadPoolSize, maxThreadPoolSize, keepAliveTime, queueSize);
    private DataBroker dataBroker = mock(DataBroker.class);
    private WriteTransaction writeTransaction = mock(WriteTransaction.class);
    private CheckedFuture<Void, TransactionCommitFailedException> checkedFuture = mock(CheckedFuture.class);
    private Message message = Message.MessageBuilder.create()
            .applicationName(".*")
            .facility(null)
            .hostname(".*")
            .processId(".*")
            .severity(null)
            .sequenceId(".*")
            .content("cisco")
            .build();
    private ReadOnlyTransaction readOnlyTransaction = mock(ReadOnlyTransaction.class);
    private CheckedFuture<Optional<SyslogDispatcher>, ReadFailedException> readFuture = mock(CheckedFuture.class);;
    private SyslogDispatcher syslogDispatcher = mock(SyslogDispatcher.class);
    private List<SyslogFilter> syslogFilters = new ArrayList<>();
    private Optional<SyslogDispatcher> optional = Optional.of(syslogDispatcher);

    private ReadFailedException readFailedException = mock(ReadFailedException.class);
    private FilterEntity filterEntity = new FilterEntityBuilder()
            .setContent("cisco")
            .setApplication(".*")
            .setFacility(null)
            .setHost(".*")
            .setPid(".*")
            .setSeverity(null)
            .setSid(".*")
            .build();

    private MessageFilter messageFilter = mock(MessageFilter.class);
    private CheckedFuture<Optional<SyslogFilter>, ReadFailedException> checkedReadFilterFuture = mock(CheckedFuture.class);

    @Before
    public void mockSetUp() {
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        when(writeTransaction.submit()).thenReturn(checkedFuture);
        manager.setDataBroker(dataBroker);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
        InstanceIdentifier<SyslogDispatcher> iid =
                InstanceIdentifier.create(SyslogDispatcher.class);
        when(readOnlyTransaction.read(LogicalDatastoreType.CONFIGURATION, iid)).thenReturn(readFuture);
        when(syslogDispatcher.getSyslogFilter()).thenReturn(syslogFilters);


    }
    @Test
    public void testWorkerThreadGetFilterswithReadFailedException() throws IllegalAccessException, InstantiationException {

        try {
            when(readFuture.checkedGet()).thenThrow(readFailedException);
        } catch (ReadFailedException e) {
            e.printStackTrace();
        }
        manager.execute("10.0.0.1",message);


    }



    @Test
    public void testWorkerThread() {
        Listener listener = new ListenerBuilder().setListenerId("321").build();
        List<Listener> listeners = new ArrayList<>();
        listeners.add(listener);
        SyslogFilter syslogFilter = new SyslogFilterBuilder()
                .setFilterId("123")
                .setFilterEntity(filterEntity)
                .setCallbackUrl("http://localhost:9001/server")
                .setListener(listeners)
                .build();
        syslogFilters.add(syslogFilter);
        InstanceIdentifier<SyslogFilter> iid = InstanceIdentifier.create(SyslogDispatcher.class)
                .child(SyslogFilter.class, new SyslogFilterKey(syslogFilter.getFilterId()));


        when(readOnlyTransaction.read(LogicalDatastoreType.CONFIGURATION,iid) ).thenReturn(checkedReadFilterFuture);
        Optional<SyslogFilter> syslogFilterOptional = Optional.of(syslogFilter);
        try {
            when(readFuture.checkedGet()).thenReturn(optional);
        } catch (ReadFailedException e) {
            e.printStackTrace();
        }

        try {
            when(checkedReadFilterFuture.checkedGet()).thenReturn(syslogFilterOptional);
        } catch (ReadFailedException e) {
            e.printStackTrace();
        }



        manager.execute("10.0.0.1",message);

        Assert.assertEquals("321",syslogFilterOptional.get().getListener().get(0).getListenerId());

    }

}
