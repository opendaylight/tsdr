/*
 * Copyright (c) 2016 TethrNet Technology Co.Ltd and others.  All rights reserved.
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.syslogs.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.opendaylight.tsdr.syslogs.server.decoder.MessageQueue;
import org.opendaylight.tsdr.syslogs.server.decoder.UDPMessageHandler;

/**
 * This is the UDP server using io.netty to start
 * UDP service. And choose UDPMessageHandler to handle
 * receiving messages later.
 *
 * @author Kun Chen(kunch@tethrnet.com)
 * @author Wenbo Hu(wenbhu@tethrnet.com)
 */
public class SyslogUDPServer implements SyslogServer {
    private final Bootstrap bootstrap;
    private final EventLoopGroup group;
    private final AtomicBoolean status = new AtomicBoolean(false);
    private final UDPMessageHandler udpMessageHandler;

    public SyslogUDPServer(MessageQueue messageQueue) {
        bootstrap = new Bootstrap();
        group = new NioEventLoopGroup();
        udpMessageHandler = new UDPMessageHandler(messageQueue);
        bootstrap.group(group)
                .channel(NioDatagramChannel.class)
                .handler(udpMessageHandler);
    }

    /**
     * setIncomingSyslogs() here is to pass the message list
     * to handler for and then return back to TSDRSyslogCollectorImpl
     * for being interted into TSDR database.
     */
    @Override
    public void startServer(int port) throws InterruptedException {
        if (status.compareAndSet(false, true)) {
            bootstrap.bind(port).sync();
        }
    }

    @Override
    public void stopServer() throws InterruptedException {
        if (status.compareAndSet(true, false)) {
            group.shutdownGracefully(500, 5000, TimeUnit.MILLISECONDS).sync();
        }
    }

    @Override
    public boolean isRunning() {
        return status.get();
    }

    @Override
    public String getProtocol() {
        return "UDP";
    }
}
