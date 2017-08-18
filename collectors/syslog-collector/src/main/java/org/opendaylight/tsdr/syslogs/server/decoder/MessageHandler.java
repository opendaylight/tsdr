/*
 * Copyright (c) 2016 TethrNet Technology Co.Ltd and others.  All rights reserved.
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.syslogs.server.decoder;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.net.InetSocketAddress;
import java.util.List;
import org.opendaylight.tsdr.syslogs.server.datastore.SyslogDatastoreManager;

/**
 * This TCP message handler handles the coming messages,
 * creates message list feedback to SyslogMessageCollectorImpl
 * and invokes a new thread to store message into ODL datastore
 * when filter matches.
 *
 * @author Kun Chen(kunch@tethrnet.com)
 * @author Wenbo Hu(wenbhu@tethrnet.com)
 */
public class MessageHandler extends SimpleChannelInboundHandler<String> {
    private final SyslogDatastoreManager manager;
    private final List<Message> incomingSyslogs;

    public MessageHandler(List<Message> incomingSyslogs, SyslogDatastoreManager manager) {
        super();
        this.incomingSyslogs = incomingSyslogs;
        this.manager = manager;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String msg) throws Exception {
        String ipaddress = ((InetSocketAddress) channelHandlerContext.channel().remoteAddress())
                .getAddress().getHostAddress();
        Message message = new Message.MessageBuilder().create()
                .hostname(ipaddress)
                .content(msg)
                .build();
        incomingSyslogs.add(message);
        synchronized (incomingSyslogs) {
            incomingSyslogs.notifyAll();
        }
        manager.execute(ipaddress, message);
    }
}
