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
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;

/**
 * Utilities for handling RPC Futures.
 *
 * @author Thomas Pantelis
 */
public final class RPCFutures {
    private RPCFutures() {
    }

    public static <T> void logResult(ListenableFuture<RpcResult<T>> future, String rpc, Logger logger) {
        Futures.addCallback(future, new FutureCallback<RpcResult<T>>() {
            @Override
            public void onSuccess(RpcResult<T> result) {
                logger.debug("RPC {} returned result {}", rpc, result);
            }

            @Override
            public void onFailure(Throwable ex) {
                logger.error("RPC {} failed", rpc, ex);
            }
        }, MoreExecutors.directExecutor());
    }
}
