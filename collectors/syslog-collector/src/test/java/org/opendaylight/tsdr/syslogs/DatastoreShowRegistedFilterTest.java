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
import org.opendaylight.tsdr.syslogs.server.datastore.SyslogDatastoreManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.SyslogFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.SyslogFilterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.syslog.filter.FilterEntity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.syslog.filter.FilterEntityBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This is the test of DatastoreShowRegistedFilter.
 * @author Wei Lai(weilai@tetthrnet.com)
 */
public class DatastoreShowRegistedFilterTest {

    private int coreThreadPoolSize = 5;
    private int maxThreadPoolSize = 10;
    private long keepAliveTime = 10L;
    private int queueSize = 10;
    private SyslogDatastoreManager manager = SyslogDatastoreManager.getInstance(coreThreadPoolSize, maxThreadPoolSize, keepAliveTime, queueSize);
    private DataBroker dataBroker = mock(DataBroker.class);
    private WriteTransaction writeTransaction = mock(WriteTransaction.class);
    private CheckedFuture<Void, TransactionCommitFailedException> checkedFuture = mock(CheckedFuture.class);
    private ReadOnlyTransaction readOnlyTransaction = mock(ReadOnlyTransaction.class);
    private InstanceIdentifier<SyslogDispatcher> iid =
            InstanceIdentifier.create(SyslogDispatcher.class);
    private CheckedFuture<Optional<SyslogDispatcher>, ReadFailedException> checkedReadFuture = mock(CheckedFuture.class);
    private SyslogDispatcher syslogDispatcher = mock(SyslogDispatcher.class);
    private List<SyslogFilter> syslogFilters = new ArrayList<>();
    private Optional<SyslogDispatcher> optional = Optional.of(syslogDispatcher);
    private FilterEntity filterEntity = new FilterEntityBuilder()
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
        manager.setDataBroker(dataBroker);
        when(syslogDispatcher.getSyslogFilter()).thenReturn(syslogFilters);

    }

    @Test
    public void testShowRegisterFilter() {

        SyslogFilter syslogFilter = new SyslogFilterBuilder()
                .setFilterId("123")
                .setFilterEntity(filterEntity)
                .setCallbackUrl("http://localhost:9001/server")
                .build();
        syslogFilters.add(syslogFilter);

        try {
            when(checkedReadFuture.checkedGet()).thenReturn(optional);
        } catch (ReadFailedException e) {
            e.printStackTrace();
        }

        Future<RpcResult<ShowRegisterFilterOutput>> future = manager.showRegisterFilter();
        try {
            Assert.assertTrue(future.get().isSuccessful());
            Assert.assertNotNull(future.get().getResult().getRegisteredSyslogFilter());
            Assert.assertEquals("registered filters are:",future.get().getResult().getResult());

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testShowRegisterFilterWithNoExistingRegisteredFilter() {

        try {
            when(checkedReadFuture.checkedGet()).thenReturn(optional);
        } catch (ReadFailedException e) {
            e.printStackTrace();
        }
        Future<RpcResult<ShowRegisterFilterOutput>> future = manager.showRegisterFilter();

        try {
            Assert.assertTrue(future.get().isSuccessful());
            Assert.assertNull(future.get().getResult().getRegisteredSyslogFilter());
            Assert.assertEquals("no registered filter",future.get().getResult().getResult());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testShowRegisterFilterWithException() throws ReadFailedException {

        ReadFailedException readFailedException = mock(ReadFailedException.class);
        when(checkedReadFuture.checkedGet()).thenThrow(readFailedException);
        Future<RpcResult<ShowRegisterFilterOutput>> future = manager.showRegisterFilter();

        try {
            Assert.assertEquals("Reading Filter failed", future.get().getResult().getResult());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
