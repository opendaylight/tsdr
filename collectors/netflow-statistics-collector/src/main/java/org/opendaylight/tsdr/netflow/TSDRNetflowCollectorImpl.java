/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2015 xFlow Research Inc. and others, All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.netflow;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Uninterruptibles;
import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.tsdr.collector.spi.logger.BatchingLogCollector;
import org.opendaylight.tsdr.netflow.parser.NetflowPacketParser;
import org.opendaylight.tsdr.netflow.parser.NetflowPacketParserFactory;
import org.opendaylight.tsdr.spi.scheduler.SchedulerService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecordBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NetFlow Collector to receive netflow packets and store into TSDR data store.
 * Currently only NetFlow Version 5 is supported.
 *
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 * @author <a href="mailto:muhammad.umair@xflowresearch.com">Umair Bhatti</a>
 * @author <a href="mailto:saichler@xgmail.com">Sharon Aicler</a>
 */
@Singleton
public class TSDRNetflowCollectorImpl implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(TSDRNetflowCollectorImpl.class);

    private static final long FLUSH_INTERVAL_IN_MILLISECONDS = 1000;

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final BatchingLogCollector logCollector;
    private final NetflowPacketParserFactory parserFactory = new NetflowPacketParserFactory();
    private DatagramSocket socket;

    /**
     * Constructor.
     */
    @Inject
    public TSDRNetflowCollectorImpl(TsdrCollectorSpiService collectorSPIService, SchedulerService schedulerService) {
        logCollector = new BatchingLogCollector(collectorSPIService, schedulerService, "TSDRNetFlowCollector",
                FLUSH_INTERVAL_IN_MILLISECONDS);
    }

    @PostConstruct
    public void init() {
        BindException lastEx = null;
        Stopwatch sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.SECONDS) <= 30) {
            try {
                this.socket = new DatagramSocket(2055);
                final Thread thread = new Thread(this::run, "TSDR NetFlow Listener");
                thread.setDaemon(true);
                thread.start();
                LOG.info("TSDRNetflowCollectorImpl initialized");
                return;
            } catch (BindException e) {
                // Address already in use - retry
                lastEx = e;
                Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
            } catch (SocketException e) {
                LOG.error("Error creating DatagramSocket on port 2055.", e);
                close();
                return;
            }
        }

        LOG.error("Collector service already running. Failed to bind it again on Port 2055.", lastEx);
        close();
    }

    @Override
    @PreDestroy
    public void close() {
        if (running.compareAndSet(true, false)) {
            if (socket != null) {
                socket.close();
            }

            logCollector.close();
        }
    }

    private void run() {
        while (running.get()) {
            byte[] data = new byte[1024];
            DatagramPacket packet = new DatagramPacket(data, data.length);
            try {
                socket.receive(packet);
                parseRecords(packet);
            } catch (IOException e) {
                if (!socket.isClosed()) {
                    LOG.error("Error while handleling netflow packets.", e);
                }
            }
        }
    }

    private void parseRecords(final DatagramPacket packet) {
        final byte[] data = packet.getData();
        String sourceIP = packet.getAddress().getHostAddress().trim();

        LOG.debug("Received packet - srcIp: {}, data length: {}", sourceIP, data.length);

        final TSDRLogRecordBuilder recordBuilder = new TSDRLogRecordBuilder().setNodeID(sourceIP)
                .setTSDRDataCategory(DataCategory.NETFLOW);

        NetflowPacketParser parser = parserFactory.newInstance(data, sourceIP, recordBuilder, builder -> {
            logCollector.insertLog(builder);
        });

        parser.parseRecords();
    }
}
