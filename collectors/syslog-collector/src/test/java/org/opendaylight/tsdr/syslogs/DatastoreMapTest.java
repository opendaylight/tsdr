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
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.tsdr.syslogs.server.datastore.RegisteredListener;
import org.opendaylight.tsdr.syslogs.server.datastore.SyslogDatastoreManager;

/**
 * This is the test of DatastoreMap.
 * @author Wei Lai(weilai@tetthrnet.com)
 */
public class DatastoreMapTest {
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
    public void testDatastoreManagerMap() {

        Map<String, String> registerMap = manager.getRegisterMap();
        Map<String, RegisteredListener> listenerMap = manager.getListenerMap();

        Assert.assertTrue(registerMap.isEmpty());
        Assert.assertTrue(listenerMap.isEmpty());
    }
}
