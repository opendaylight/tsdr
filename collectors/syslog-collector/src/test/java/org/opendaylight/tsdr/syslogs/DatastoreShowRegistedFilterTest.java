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
import java.util.concurrent.Future;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.ShowRegisterFilterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.SyslogDispatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.SyslogFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.SyslogFilterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.syslog.filter.FilterEntity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.syslog.filter.FilterEntityBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

/**
 * This is the test of DatastoreShowRegistedFilter.
 * @author Wei Lai(weilai@tetthrnet.com)
 */
public class DatastoreShowRegistedFilterTest {

    private final int coreThreadPoolSize = 5;
    private final int maxThreadPoolSize = 10;
    private final long keepAliveTime = 10L;
    private final int queueSize = 10;
    private final DataBroker dataBroker = mock(DataBroker.class);
    private final SyslogDatastoreManager manager = SyslogDatastoreManager.getInstance(dataBroker, coreThreadPoolSize,
            maxThreadPoolSize, keepAliveTime, queueSize);
    private final WriteTransaction writeTransaction = mock(WriteTransaction.class);
    private final CheckedFuture<Void, TransactionCommitFailedException> checkedFuture = mock(CheckedFuture.class);
    private final ReadOnlyTransaction readOnlyTransaction = mock(ReadOnlyTransaction.class);
    private final InstanceIdentifier<SyslogDispatcher> iid =
            InstanceIdentifier.create(SyslogDispatcher.class);
    private final CheckedFuture<Optional<SyslogDispatcher>, ReadFailedException> checkedReadFuture =
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

    @Before
    public void mockSetUp() {
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        when(writeTransaction.submit()).thenReturn(checkedFuture);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
        when(readOnlyTransaction.read(LogicalDatastoreType.CONFIGURATION,iid)).thenReturn(checkedReadFuture);
        when(syslogDispatcher.getSyslogFilter()).thenReturn(syslogFilters);

    }

    @Test
    public void testShowRegisterFilter() throws InterruptedException, ExecutionException {

        SyslogFilter syslogFilter = new SyslogFilterBuilder()
                .setFilterId("123")
                .setFilterEntity(filterEntity)
                .setCallbackUrl("http://localhost:9001/server")
                .build();
        syslogFilters.add(syslogFilter);

        when(checkedReadFuture.get()).thenReturn(optional);

        Future<RpcResult<ShowRegisterFilterOutput>> future = manager.showRegisterFilter();
        Assert.assertTrue(future.get().isSuccessful());
        Assert.assertNotNull(future.get().getResult().getRegisteredSyslogFilter());
        Assert.assertEquals("registered filters are:",future.get().getResult().getResult());
    }

    @Test
    public void testShowRegisterFilterWithNoExistingRegisteredFilter() throws InterruptedException, ExecutionException {

        when(checkedReadFuture.get()).thenReturn(optional);

        Future<RpcResult<ShowRegisterFilterOutput>> future = manager.showRegisterFilter();

        Assert.assertTrue(future.get().isSuccessful());
        Assert.assertNull(future.get().getResult().getRegisteredSyslogFilter());
        Assert.assertEquals("no registered filter",future.get().getResult().getResult());
    }

    @Test
    public void testShowRegisterFilterWithException() throws InterruptedException, ExecutionException {
        when(checkedReadFuture.get()).thenThrow(new ExecutionException("mock", new ReadFailedException("mock")));
        Future<RpcResult<ShowRegisterFilterOutput>> future = manager.showRegisterFilter();

        Assert.assertEquals("Reading Filter failed", future.get().getResult().getResult());
    }
}
