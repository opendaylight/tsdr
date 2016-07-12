/*
 * Copyright (c) 2016 TethrNet Technology Co.Ltd and others.  All rights reserved.
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.syslogs.server;

/**
 * This is the UDP server using io.netty to start
 * UDP service. And choose UDPMessageHandler to handle
 * receiving messages later.
 *
 * @author Kun Chen(kunch@tethrnet.com)
 * @author Wenbo Hu(wenbhu@tethrnet.com)
 */
public interface SyslogServer {
    /**
     * Start the syslog server
     */
    void startServer() throws InterruptedException;

    /**
     * Stop the syslog server
     */
    void stopServer() throws InterruptedException;

    /**
     * Check if the syslog server is running
     */
    boolean isRunning();

    /**
     * Set the port of syslog server
     *
     * @param port
     * @throws Exception when the server is running
     */
    void setPort(int port) throws Exception;

    /**
     * get the protocol used for syslog server
     */
    String getProtocol();
}
