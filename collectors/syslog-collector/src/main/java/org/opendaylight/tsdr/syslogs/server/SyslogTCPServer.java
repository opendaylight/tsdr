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
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.tsdr.syslogs.server.decoder.Message;
import org.opendaylight.tsdr.syslogs.server.decoder.MessageHandler;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This is the TCP server using io.netty to start
 * TCP service. And choose MessageHandler to handle
 * receiving messages later.
 *
 * @author Kun Chen(kunch@tethrnet.com)
 * @author Wenbo Hu(wenbhu@tethrnet.com)
 */
public class SyslogTCPServer implements SyslogServer {
    private AtomicInteger port;
    private final ServerBootstrap b;
    private final EventLoopGroup[] groups;
    private AtomicBoolean status;
    private DataBroker db;
    StringDecoder stringDecoder = null;
    MessageHandler messageHandler = null;

    public SyslogTCPServer(List<Message> messages) {
        port = new AtomicInteger(-1);
        b = new ServerBootstrap();
        groups = new EventLoopGroup[]{new NioEventLoopGroup(), new NioEventLoopGroup()};
        status = new AtomicBoolean(false);
        stringDecoder = new StringDecoder();
        messageHandler = new MessageHandler(messages);
        b.group(groups[0], groups[1])
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        final ChannelPipeline pipeline = socketChannel.pipeline();
                        pipeline.addLast("framer", new DelimiterBasedFrameDecoder(64 * 1024, Delimiters.lineDelimiter()));
                        pipeline.addLast("stringer", stringDecoder);
                        pipeline.addLast("handler", messageHandler);
                    }
                });
    }

    /**
     * setIncomingSyslogs() here is to pass the message list
     * to handler for and then return back to TSDRSyslogCollectorImpl
     * for being interted into TSDR database.
     *
     * @throws InterruptedException
     */
    @Override
    public void startServer() throws InterruptedException {
        b.bind(port.get()).sync();
        status.set(true);
    }

    @Override
    public void stopServer() throws InterruptedException {
        for (EventLoopGroup g : groups) {
            g.shutdownGracefully().sync();
        }
        status.set(false);
    }

    @Override
    public boolean isRunning() {
        return status.get();
    }

    @Override
    public void setPort(int port) throws Exception {
        if (isRunning()) {
            throw new Exception("TCP Server is running at port: " + port + ".");
        } else
            this.port.set(port);
    }

    @Override
    public String getProtocol() {
        return "TCP";
    }
}
