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

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
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

/**
 * This is the test of DatastoreWorkerThread.
 * @author Wei Lai(weilai@tetthrnet.com)
 */
public class DatastoreWorkerThreadTest {
    private final int coreThreadPoolSize = 5;
    private final int maxThreadPoolSize = 10;
    private final long keepAliveTime = 10L;
    private final int queueSize = 10;
    private final DataBroker dataBroker = mock(DataBroker.class);
    private final SyslogDatastoreManager manager = SyslogDatastoreManager.getInstance(dataBroker, coreThreadPoolSize,
            maxThreadPoolSize, keepAliveTime, queueSize);
    private final WriteTransaction writeTransaction = mock(WriteTransaction.class);
    private final CheckedFuture<Void, TransactionCommitFailedException> checkedFuture = mock(CheckedFuture.class);
    private final Message message = Message.MessageBuilder.create()
            .applicationName(".*")
            .facility(null)
            .hostname(".*")
            .processId(".*")
            .severity(null)
            .sequenceId(".*")
            .content("cisco")
            .build();
    private final ReadOnlyTransaction readOnlyTransaction = mock(ReadOnlyTransaction.class);
    private final CheckedFuture<Optional<SyslogDispatcher>, ReadFailedException> readFuture =
            mock(CheckedFuture.class);
    private final SyslogDispatcher syslogDispatcher = mock(SyslogDispatcher.class);
    private final List<SyslogFilter> syslogFilters = new ArrayList<>();
    private final Optional<SyslogDispatcher> optional = Optional.of(syslogDispatcher);

    private final FilterEntity filterEntity = new FilterEntityBuilder()
            .setContent("cisco")
            .setApplication(".*")
            .setFacility(null)
            .setHost(".*")
            .setPid(".*")
            .setSeverity(null)
            .setSid(".*")
            .build();

    private final CheckedFuture<Optional<SyslogFilter>, ReadFailedException> checkedReadFilterFuture =
            mock(CheckedFuture.class);

    @Before
    public void mockSetUp() {
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        when(writeTransaction.submit()).thenReturn(checkedFuture);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
        InstanceIdentifier<SyslogDispatcher> iid =
                InstanceIdentifier.create(SyslogDispatcher.class);
        when(readOnlyTransaction.read(LogicalDatastoreType.CONFIGURATION, iid)).thenReturn(readFuture);
        when(syslogDispatcher.getSyslogFilter()).thenReturn(syslogFilters);


    }

    @Test
    public void testWorkerThreadGetFilterswithReadFailedException() throws InterruptedException, ExecutionException {
        when(readFuture.get()).thenThrow(new ExecutionException("mock", new ReadFailedException("mock")));
        manager.execute("10.0.0.1",message);
    }

    @Test
    public void testWorkerThread() throws InterruptedException, ExecutionException {
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


        when(readOnlyTransaction.read(LogicalDatastoreType.CONFIGURATION,iid)).thenReturn(checkedReadFilterFuture);
        Optional<SyslogFilter> syslogFilterOptional = Optional.of(syslogFilter);
        when(readFuture.get()).thenReturn(optional);

        when(checkedReadFilterFuture.get()).thenReturn(syslogFilterOptional);

        manager.execute("10.0.0.1",message);

        Assert.assertEquals("321",syslogFilterOptional.get().getListener().get(0).getListenerId());
    }
}
