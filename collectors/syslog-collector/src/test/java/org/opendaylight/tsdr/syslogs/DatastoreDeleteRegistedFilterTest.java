/*
 * Copyright (c) 2016 TethrNet Technology Co.Ltd and others.  All rights reserved.
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.syslogs;

import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.tsdr.syslogs.server.datastore.RegisteredListener;
import org.opendaylight.tsdr.syslogs.server.datastore.SyslogDatastoreManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.*;
import org.opendaylight.yangtools.yang.common.RpcResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This is the test of DatastoreDeleteRegistedFilter.
 * @author Wei Lai(weilai@tetthrnet.com)
 */
public class DatastoreDeleteRegistedFilterTest {

    private int coreThreadPoolSize = 5;
    private int maxThreadPoolSize = 10;
    private long keepAliveTime = 10L;
    private int queueSize = 10;
    private SyslogDatastoreManager manager = SyslogDatastoreManager.getInstance(coreThreadPoolSize, maxThreadPoolSize, keepAliveTime, queueSize);
    private DataBroker dataBroker = mock(DataBroker.class);
    private WriteTransaction writeTransaction = mock(WriteTransaction.class);
    private CheckedFuture<Void, TransactionCommitFailedException> checkedFuture = mock(CheckedFuture.class);
    private Map<String, String> registerMap = new HashMap<>();
    private Map<String, RegisteredListener> listenerMap = new HashMap<>();
    private RegisteredListener newRegisteredListener = mock(RegisteredListener.class);
    private String filterID = UUID.randomUUID().toString();
    private String listenerID = UUID.randomUUID().toString();
    private DeleteRegisteredFilterInput input;

    @Before
    public void mockSetUp() {
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        when(writeTransaction.submit()).thenReturn(checkedFuture);
        manager.setDataBroker(dataBroker);
        input = new DeleteRegisteredFilterInputBuilder()
                .setFilterId(filterID)
                .build();
        registerMap.put(filterID,listenerID);
        listenerMap.put(listenerID,newRegisteredListener);
        when(newRegisteredListener.close()).thenReturn(false);
        manager.setRegisterMap(registerMap);
        manager.setListenerMap(listenerMap);

    }

    @Test
    public void testDeleteRegistedFilterWithCloseRegistrationException() {

        when(newRegisteredListener.close()).thenReturn(false);
        Future<RpcResult<DeleteRegisteredFilterOutput>> future =manager.deleteRegisteredFilter(input);

        try {
            Assert.assertEquals("listener registration close failed",future.get().getResult().getResult());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDeleteRegistedFilterSuccessful() {

        when(newRegisteredListener.close()).thenReturn(true);

        Future<RpcResult<DeleteRegisteredFilterOutput>> future =manager.deleteRegisteredFilter(input);

        try {
            Assert.assertEquals("filter delete successfully",future.get().getResult().getResult());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }

}
