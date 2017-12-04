/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2016 TethrNet Technology Co.Ltd and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.syslogs;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import org.opendaylight.tsdr.syslogs.filters.SyslogFilterManager;
import org.opendaylight.tsdr.syslogs.server.SyslogTCPServer;
import org.opendaylight.tsdr.syslogs.server.SyslogUDPServer;
import org.opendaylight.tsdr.syslogs.server.datastore.SyslogDatastoreManager;
import org.opendaylight.tsdr.syslogs.server.decoder.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRLogRecordInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Class start both TCP and UDP servers
 * to receive syslog messages and claim how
 * TSDR database handle the messages.
 *
 * @author Sharon Aicler(saichler@gmail.com)
 * @author Kun Chen(kunch@tethrnet.com)
 **/
public class TSDRSyslogCollectorImpl {
    public static final long QUEUE_WAIT_INTERVAL = 2000;
    public static final long STORE_FLUSH_INTERVAL = 2500;

    private static final Logger LOG = LoggerFactory.getLogger(TSDRSyslogCollectorImpl.class);

    private final SyslogTCPServer tcpServer;
    private final SyslogUDPServer udpServer;
    private int udpPort;
    private final int tcpPort;

    private final TsdrCollectorSpiService collectorSPIService;
    private final SyslogFilterManager filterManager = new SyslogFilterManager();
    private final SyslogDatastoreManager manager;

    private final Deque<Message> messageList = new LinkedList<>();

    private volatile boolean running = true;

    public TSDRSyslogCollectorImpl(TsdrCollectorSpiService collectorSPIService, SyslogDatastoreManager manager,
            int udpPort, int tcpPort) {
        this.collectorSPIService = collectorSPIService;
        this.manager = manager;

        this.udpPort = udpPort;
        this.tcpPort = tcpPort;

        tcpServer = new SyslogTCPServer(messageList, this.manager);
        udpServer = new SyslogUDPServer(messageList, this.manager);
    }

    public boolean isRunning() {
        return this.running;
    }

    public void init() {
        new SyslogProcessor().start();

        LOG.info("Syslog Collector Session Initiated");

        //Start TCP syslog server
        LOG.info("Start TCP server");
        try {
            tcpServer.startServer(tcpPort);
            LOG.info("TCP server started at port: {}", tcpPort);
        } catch (InterruptedException e) {
            LOG.error("Error starting TCP srver on port {}", tcpPort, e);
            this.close();
        }

        //Test If port is available
        boolean udpPortAvailable = false;
        try {
            DatagramSocket socket = new DatagramSocket(udpPort);
            socket.close();
            socket = null;
            udpPortAvailable = true;
        } catch (SocketException e) {
            LOG.warn("Port {} is not available for UDP, trying backup...", udpPort, e);
            try {
                udpPort += 1000;
                DatagramSocket socket = new DatagramSocket(udpPort);
                socket.close();
                socket = null;
                udpPortAvailable = true;
            } catch (SocketException e1) {
                this.close();
                LOG.error("Port {} is not available, not starting servers!", udpPort, e1);
            }
        }

        if (udpPortAvailable) {
            //Start UDP syslog server
            LOG.info("Start UDP server");
            try {
                udpServer.startServer(udpPort);
                LOG.info("UDP server started at port {}", udpPort);
            } catch (InterruptedException e) {
                LOG.error("Failed to start UDP server on port {}", udpPort, e);
            }
        }
    }

    public void close() {
        running = false;
        try {
            if (tcpServer != null) {
                tcpServer.stopServer();
            }
            if (udpServer != null) {
                udpServer.stopServer();
            }
        } catch (InterruptedException e) {
            LOG.error(e.getMessage());
        }
    }

    private class SyslogProcessor extends Thread {
        SyslogProcessor() {
            super("TSDR Syslog Processor");
            this.setDaemon(true);
        }

        @Override
        public void run() {
            List<TSDRLogRecord> syslogQueue = new ArrayList<>();
            long lastPersisted = System.currentTimeMillis();
            Message message = null;
            while (running) {
                synchronized (messageList) {
                    if (messageList.isEmpty()) {
                        try {
                            messageList.wait(QUEUE_WAIT_INTERVAL);
                        } catch (InterruptedException e) {
                            // no-op
                        }
                    }
                    if (!messageList.isEmpty()) {
                        message = messageList.removeFirst();
                    }
                }

                if (message != null) {
                    TSDRLogRecord logRecord = filterManager.applyFilters(message);
                    if (logRecord != null) {
                        syslogQueue.add(logRecord);
                    }
                }

                if (System.currentTimeMillis() - lastPersisted > STORE_FLUSH_INTERVAL && !syslogQueue.isEmpty()) {
                    List<TSDRLogRecord> queue = null;
                    synchronized (filterManager) {
                        //Currently there is only one SyslogProcessor thread so this check seems meaningless
                        //If the future if we decide to have a few of those we need to make sure the queue
                        //has something and the interval has passed inside the synchronize block.
                        if (System.currentTimeMillis() - lastPersisted > STORE_FLUSH_INTERVAL
                                && !syslogQueue.isEmpty()) {
                            lastPersisted = System.currentTimeMillis();
                            queue = syslogQueue;
                            syslogQueue = new LinkedList<>();
                        }
                    }
                    if (queue != null) {
                        store(queue);
                    }
                }

                message = null;
            }
        }
    }

    private void store(List<TSDRLogRecord> queue) {
        InsertTSDRLogRecordInputBuilder input = new InsertTSDRLogRecordInputBuilder();
        input.setTSDRLogRecord(queue);
        input.setCollectorCodeName("SyslogCollector");
        collectorSPIService.insertTSDRLogRecord(input.build());
    }

    public int getUdpPort() {
        return this.udpPort;
    }

    public int getTcpPort() {
        return this.tcpPort;
    }
}
