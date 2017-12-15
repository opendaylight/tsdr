/*
 * Copyright (c) 2017 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.collector.spi;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.Future;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;

/**
 * Utilities for handling RPC Futures.
 *
 * @author Thomas Pantelis
 */
public class RPCFutures {
    public static void logResult(Future<RpcResult<Void>> future, String rpc, Logger logger) {
        Futures.addCallback(JdkFutureAdapters.listenInPoolThread(future), new FutureCallback<RpcResult<Void>>() {
            @Override
            public void onSuccess(RpcResult<Void> result) {
                logger.debug("RPC {} returned result {}", rpc, result);
            }

            @Override
            public void onFailure(Throwable ex) {
                logger.error("RPC {} failed", rpc, ex);
            }
        }, MoreExecutors.directExecutor());
    }
}
