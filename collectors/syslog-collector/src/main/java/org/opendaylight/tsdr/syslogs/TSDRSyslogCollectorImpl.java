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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.tsdr.collector.spi.logger.AbstractBatchingLogCollector;
import org.opendaylight.tsdr.spi.scheduler.SchedulerService;
import org.opendaylight.tsdr.syslogs.filters.SyslogFilterManager;
import org.opendaylight.tsdr.syslogs.server.SyslogTCPServer;
import org.opendaylight.tsdr.syslogs.server.SyslogUDPServer;
import org.opendaylight.tsdr.syslogs.server.datastore.SyslogDatastoreManager;
import org.opendaylight.tsdr.syslogs.server.decoder.Message;
import org.opendaylight.tsdr.syslogs.server.decoder.MessageQueue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.SyslogCollectorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Class start both TCP and UDP servers
 * to receive syslog messages and claim how
 * TSDR database handle the messages.
 *
 * @author Sharon Aicler(saichler@gmail.com)
 * @author Kun Chen(kunch@tethrnet.com)
 */
@Singleton
public class TSDRSyslogCollectorImpl implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(TSDRSyslogCollectorImpl.class);

    private final SyslogTCPServer tcpServer;
    private final SyslogUDPServer udpServer;
    private final int tcpPort;

    private final SyslogFilterManager filterManager = new SyslogFilterManager();
    private final AtomicBoolean running = new AtomicBoolean();

    private final AbstractBatchingLogCollector<Message> logCollector;

    private int udpPort;

    @Inject
    public TSDRSyslogCollectorImpl(TsdrCollectorSpiService collectorSPIService, SyslogDatastoreManager manager,
            SchedulerService schedulerService, SyslogCollectorConfig collectorConfig) {
        this.udpPort = collectorConfig.getUdpport();
        this.tcpPort = collectorConfig.getTcpport();

        logCollector = new AbstractBatchingLogCollector<Message>(collectorSPIService, schedulerService,
                "SyslogCollector", collectorConfig.getStoreFlushInterval().intValue()) {
            @Override
            protected List<TSDRLogRecord> transform(List<Message> from) {
                return from.stream().flatMap(msg -> filter(msg)).collect(Collectors.toList());
            }
        };

        MessageQueue messageQueue = message -> {
            logCollector.enqueue(message);
            manager.execute(message);
        };

        tcpServer = new SyslogTCPServer(messageQueue);
        udpServer = new SyslogUDPServer(messageQueue);
    }

    public boolean isRunning() {
        return this.running.get();
    }

    @PostConstruct
    public void init() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        LOG.info("Syslog Collector Session Initiated");

        //Test If port is available
        boolean udpPortAvailable = false;
        try {
            DatagramSocket socket = new DatagramSocket(udpPort);
            socket.close();
            udpPortAvailable = true;
        } catch (SocketException e) {
            LOG.warn("Port {} is not available for UDP, trying backup...", udpPort, e);
            try {
                udpPort += 1000;
                DatagramSocket socket = new DatagramSocket(udpPort);
                socket.close();
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

            //Start TCP syslog server
            LOG.info("Start TCP server");
            try {
                tcpServer.startServer(tcpPort);
                LOG.info("TCP server started at port: {}", tcpPort);
            } catch (InterruptedException e) {
                LOG.error("Error starting TCP srver on port {}", tcpPort, e);
                this.close();
            }
        }
    }

    @Override
    @PreDestroy
    public void close() {
        if (running.compareAndSet(true, false)) {
            try {
                tcpServer.stopServer();
                udpServer.stopServer();
            } catch (InterruptedException e) {
                LOG.debug("Interrupted stopping server", e);
            }

            logCollector.close();
        }
    }

    private Stream<TSDRLogRecord> filter(Message message) {
        TSDRLogRecord logRecord = filterManager.applyFilters(message);
        return logRecord != null ? Stream.of(logRecord) : Stream.empty();
    }

    public int getUdpPort() {
        return this.udpPort;
    }

    public int getTcpPort() {
        return this.tcpPort;
    }
}
