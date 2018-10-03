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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.concurrent.GuardedBy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.tsdr.collector.spi.RPCFutures;
import org.opendaylight.tsdr.netflow.parser.NetflowPacketParser;
import org.opendaylight.tsdr.netflow.parser.NetflowPacketParserFactory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRLogRecordInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRLogRecordInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecord;
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
public class TSDRNetflowCollectorImpl extends Thread implements AutoCloseable {

    private static final long PERSIST_CHECK_INTERVAL_IN_MILLISECONDS = 1000;
    private static final long INCOMING_QUEUE_WAIT_INTERVAL_IN_MILLISECONDS = 500;
    private static final Logger LOG = LoggerFactory.getLogger(TSDRNetflowCollectorImpl.class);

    private final TsdrCollectorSpiService collectorSPIService;
    private DatagramSocket socket;
    private final AtomicBoolean running = new AtomicBoolean(true);
    @GuardedBy("incomingNetFlow")
    private final LinkedList<DatagramPacket> incomingNetFlow = new LinkedList<>();

    private final NetflowPacketParserFactory parserFactory = new NetflowPacketParserFactory();

    /**
     * Constructor.
     */
    @Inject
    public TSDRNetflowCollectorImpl(TsdrCollectorSpiService collectorSPIService) {
        super("TSDR NetFlow Listener");
        this.setDaemon(true);
        this.collectorSPIService = collectorSPIService;
    }

    @PostConstruct
    public void init() {
        BindException lastEx = null;
        Stopwatch sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.SECONDS) <= 30) {
            try {
                this.socket = new DatagramSocket(2055);
                this.start();
                new NetFlowProcessor();
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
    public void run() {
        if (this.socket == null || this.socket.isClosed()) {
            close();
        } else {
            while (running.get()) {
                byte[] data = new byte[1024];
                DatagramPacket packet = new DatagramPacket(data, data.length);
                try {
                    socket.receive(packet);
                    handleNetFlow(packet);
                } catch (IOException e) {
                    if (!socket.isClosed()) {
                        LOG.error("Error while handleling netflow packets.", e);
                    }
                    close();
                }
            }
        }
    }

    @Override
    @PreDestroy
    public void close() {
        if (running.compareAndSet(true, false)) {
            if (socket != null) {
                socket.close();
            }
            synchronized (incomingNetFlow) {
                incomingNetFlow.notifyAll();
            }
        }
    }

    public void handleNetFlow(DatagramPacket packet) {
        synchronized (incomingNetFlow) {
            incomingNetFlow.add(packet);
            incomingNetFlow.notifyAll();
        }
    }

    private class NetFlowProcessor extends Thread {
        NetFlowProcessor() {
            super("TSDR NetFlow Processor");
            this.setDaemon(true);
            this.start();
            LOG.debug("NetFlow Processor thread initialized");
        }

        @Override
        public void run() {
            List<TSDRLogRecord> netFlowQueue = new LinkedList<>();
            long lastPersisted = System.currentTimeMillis();
            AtomicInteger counter = new AtomicInteger(1);
            while (running.get()) {
                DatagramPacket packet = null;
                synchronized (incomingNetFlow) {
                    if (incomingNetFlow.isEmpty()) {
                        LOG.debug("No Pkts in queue");
                        try {
                            incomingNetFlow.wait(INCOMING_QUEUE_WAIT_INTERVAL_IN_MILLISECONDS);
                        } catch (InterruptedException e) {
                            LOG.error("Interrupted while waiting on incoming queue", e);
                        }
                    }
                    if (!incomingNetFlow.isEmpty()) {
                        packet = incomingNetFlow.removeFirst();
                    }
                }

                if (packet != null) {
                    parseRecords(netFlowQueue, counter, packet);
                }

                if (System.currentTimeMillis() - lastPersisted > PERSIST_CHECK_INTERVAL_IN_MILLISECONDS
                        && !netFlowQueue.isEmpty()) {
                    List<TSDRLogRecord> queue = null;
                    if (System.currentTimeMillis() - lastPersisted > PERSIST_CHECK_INTERVAL_IN_MILLISECONDS
                            && !netFlowQueue.isEmpty()) {
                        lastPersisted = System.currentTimeMillis();
                        queue = netFlowQueue;
                        netFlowQueue = new LinkedList<>();
                    }
                    if (queue != null) {
                        store(queue);
                    }
                }
            }
        }

        private void parseRecords(final List<TSDRLogRecord> netFlowQueue, final AtomicInteger counter,
                final DatagramPacket packet) {
            final byte[] data = packet.getData();
            String srcIp = packet.getAddress().getHostAddress().trim();
            long currentTimeStamp = System.currentTimeMillis();

            LOG.debug("Received packet - srcIp: {}, data length: {}", srcIp, data.length);

            NetflowPacketParser parser = parserFactory.newInstance(data);
            parser.parseRecords(recordAttrs -> {
                final TSDRLogRecord record = new TSDRLogRecordBuilder().setNodeID(srcIp)
                        .setTimeStamp(currentTimeStamp).setIndex(counter.getAndIncrement())
                        .setTSDRDataCategory(DataCategory.NETFLOW).setRecordFullText("Netflow packet")
                        .setRecordAttributes(recordAttrs).build();
                netFlowQueue.add(record);
            });
        }
    }

   /**
     * Store the data into TSDR data store.
     *
     * @param queue the data to store
    */
    private void store(List<TSDRLogRecord> queue) {
        InsertTSDRLogRecordInput input = new InsertTSDRLogRecordInputBuilder().setTSDRLogRecord(queue)
                .setCollectorCodeName("TSDRNetFlowCollector").build();

        LOG.debug("Storing {} log records: {}", queue.size(), input);

        RPCFutures.logResult(collectorSPIService.insertTSDRLogRecord(input), "insertTSDRLogRecord", LOG);
    }
}
