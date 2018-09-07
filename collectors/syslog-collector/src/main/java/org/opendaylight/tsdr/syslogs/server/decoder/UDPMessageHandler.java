/*
 * Copyright (c) 2016 TethrNet Technology Co.Ltd and others.  All rights reserved.
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.syslogs.server.decoder;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This UDP message handler handles the incoming messages,
 * creates message list feedback to SyslogMessageCollectorImpl
 * and invokes a new thread to store message into ODL datastore
 * when filter matches.
 *
 * @author Kun Chen(kunch@tethrnet.com)
 * @author Wenbo Hu(wenbhu@tethrnet.com)
 */
@ChannelHandler.Sharable
public class UDPMessageHandler extends SimpleChannelInboundHandler<DatagramPacket> {
    private static final Logger LOG = LoggerFactory.getLogger(UDPMessageHandler.class);

    private final MessageQueue messageQueue;

    public UDPMessageHandler(MessageQueue messageQueue) {
        this.messageQueue = messageQueue;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
        String str = msg.content().toString(CharsetUtil.UTF_8);
        LOG.trace("UDP Syslog Received {}", str);
        String ipaddress = msg.sender().getAddress().getHostAddress();
        Message message;
        if (MessageDecoder.matches(str)) {
            message = MessageDecoder.decode(str);
        } else {
            message = Message.MessageBuilder.create()
                    .content(str)
                    .hostname(ipaddress)
                    .build();
        }

        messageQueue.enqueue(message);
    }
}
