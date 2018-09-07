/*
 * Copyright (c) 2016 TethrNet Technology Co.Ltd and others.  All rights reserved.
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.syslogs.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.opendaylight.tsdr.syslogs.server.datastore.SyslogDatastoreManager;
import org.opendaylight.tsdr.syslogs.server.decoder.Message;
import org.opendaylight.tsdr.syslogs.server.decoder.MessageHandler;

/**
 * This is the TCP server using io.netty to start
 * TCP service. And choose MessageHandler to handle
 * receiving messages later.
 *
 * @author Kun Chen(kunch@tethrnet.com)
 * @author Wenbo Hu(wenbhu@tethrnet.com)
 */
public class SyslogTCPServer implements SyslogServer {
    private final ServerBootstrap serverBootstrap;
    private final EventLoopGroup[] groups;
    private final AtomicBoolean status = new AtomicBoolean(false);
    private final StringDecoder stringDecoder;
    private final MessageHandler messageHandler;

    public SyslogTCPServer(Deque<Message> messages, SyslogDatastoreManager manager) {
        serverBootstrap = new ServerBootstrap();
        groups = new EventLoopGroup[]{new NioEventLoopGroup(), new NioEventLoopGroup()};
        stringDecoder = new StringDecoder();
        messageHandler = new MessageHandler(messages, manager);
        serverBootstrap.group(groups[0], groups[1])
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        final ChannelPipeline pipeline = socketChannel.pipeline();
                        pipeline.addLast("framer", new DelimiterBasedFrameDecoder(64 * 1024,
                                Delimiters.lineDelimiter()));
                        pipeline.addLast("stringer", stringDecoder);
                        pipeline.addLast("handler", messageHandler);
                    }
                });
    }

    /**
     * setIncomingSyslogs() here is to pass the message list
     * to handler for and then return back to TSDRSyslogCollectorImpl
     * for being interted into TSDR database.
     */
    @Override
    public void startServer(int port) throws InterruptedException {
        if (status.compareAndSet(false, true)) {
            serverBootstrap.bind(port).sync();
        }
    }

    @Override
    public void stopServer() throws InterruptedException {
        if (status.compareAndSet(true, false)) {
            for (EventLoopGroup g : groups) {
                g.shutdownGracefully(500, 5000, TimeUnit.MILLISECONDS).sync();
            }
        }
    }

    @Override
    public boolean isRunning() {
        return status.get();
    }

    @Override
    public String getProtocol() {
        return "TCP";
    }
}
