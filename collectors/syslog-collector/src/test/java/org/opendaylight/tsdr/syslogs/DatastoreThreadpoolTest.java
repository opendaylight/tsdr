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

import com.google.common.util.concurrent.CheckedFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.tsdr.syslogs.server.datastore.SyslogDatastoreManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.ConfigThreadpoolInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.ConfigThreadpoolInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.ConfigThreadpoolOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;

/**
 * Test of Datastore threadpool configuration.
 *
 * @author Kun Chen(kunch@tethrnet.com)
 */
public class DatastoreThreadpoolTest {

    private final int coreThreadPoolSize = 5;
    private final int maxThreadPoolSize = 10;
    private final long keepAliveTime = 10L;
    private final int queueSize = 10;
    private final DataBroker dataBroker = mock(DataBroker.class);
    private final SyslogDatastoreManager manager = SyslogDatastoreManager.getInstance(dataBroker, coreThreadPoolSize,
            maxThreadPoolSize, keepAliveTime, queueSize);
    private final WriteTransaction writeTransaction = mock(WriteTransaction.class);
    private final CheckedFuture<Void, TransactionCommitFailedException> checkedFuture = mock(CheckedFuture.class);

    @Before
    public void mockSetUp() {
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        when(writeTransaction.submit()).thenReturn(checkedFuture);
    }

    @Test
    public void testshowThreadpoolConfiguration() throws InterruptedException, ExecutionException {
        Assert.assertTrue(manager.showThreadpoolConfiguration().get().isSuccessful());
        Assert.assertEquals(5,(long)manager.showThreadpoolConfiguration().get().getResult().getCoreThreadNumber());
        Assert.assertEquals(10,(long)manager.showThreadpoolConfiguration().get().getResult().getMaxThreadNumber());
        Assert.assertEquals(10L,(long)manager.showThreadpoolConfiguration().get().getResult().getKeepAliveTime());
        Assert.assertEquals(10,manager.showThreadpoolConfiguration().get().getResult().getQueueUsedCapacity()
                + manager.showThreadpoolConfiguration().get().getResult().getQueueRemainingCapacity());
    }

    @Test
    public void testConfigThreadpool() throws InterruptedException, ExecutionException {
        ConfigThreadpoolInput input = new ConfigThreadpoolInputBuilder()
                .setCoreThreadNumber(10)
                .setMaxThreadNumber(20)
                .setKeepAliveTime(20)
                .build();
        Future<RpcResult<ConfigThreadpoolOutput>> future = manager.configThreadpool(input);
        Assert.assertTrue(future.get().isSuccessful());
        Assert.assertEquals("success",future.get().getResult().getResult());
        Assert.assertEquals(10,(long)manager.showThreadpoolConfiguration().get().getResult().getCoreThreadNumber());
        Assert.assertEquals(20,(long)manager.showThreadpoolConfiguration().get().getResult().getMaxThreadNumber());
        Assert.assertEquals(20L,(long)manager.showThreadpoolConfiguration().get().getResult().getKeepAliveTime());
    }
}
