/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2016 TethrNet Technology Co.Ltd and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.syslogs;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.tsdr.syslogs.filters.SyslogFilterManager;
import org.opendaylight.tsdr.syslogs.server.SyslogTCPServer;
import org.opendaylight.tsdr.syslogs.server.SyslogUDPServer;
import org.opendaylight.tsdr.syslogs.server.datastore.SyslogDatastoreManager;
import org.opendaylight.tsdr.syslogs.server.decoder.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRLogRecordInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.TsdrSyslogCollectorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramSocket;
import java.util.LinkedList;
import java.util.List;

/**
 * This Class start both TCP and UDP servers
 * to receive syslog messages and claim how
 * TSDR database handle the messages.
 *
 * @author Sharon Aicler(saichler@gmail.com)
 * @author Kun Chen(kunch@tethrnet.com)
 **/
public class TSDRSyslogCollectorImpl implements BindingAwareProvider {
    public static final int UDP_PORT = 514;
    public static final int TCP_PORT = 6514;
    public static final long QUEUE_WAIT_INTERVAL = 2000;
    public static final long STORE_FLUSH_INTERVAL = 2500;

    SyslogTCPServer tcpServer;
    SyslogUDPServer udpServer;

    private TsdrCollectorSpiService collectorSPIService = null;
    private boolean running = true;
    private Logger logger = LoggerFactory.getLogger(TSDRSyslogCollectorImpl.class);
    private List<TSDRLogRecord> syslogQueue = new LinkedList<TSDRLogRecord>();
    private final List<Message> messageList = new LinkedList<Message>();
    private SyslogFilterManager filterManager = new SyslogFilterManager();
    private long lastPersisted = System.currentTimeMillis();
    private int udpPort = UDP_PORT;
    private int tcpPort = TCP_PORT;
    private int coreThreadPoolSize = 5;
    private int maxThreadPoolSize = 10;
    private long keepAliveTime = 10L;
    private int queueSize = 10;

    private DataBroker dataBroker;
    private SyslogDatastoreManager manager;
    private BindingAwareBroker.RpcRegistration<TsdrSyslogCollectorService> syslogsvrService;

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /***
     * constructor of collector
     *
     * @param _collectorSPIService invoke collector SPI service to implement tsdr data insertion
     */
    public TSDRSyslogCollectorImpl(TsdrCollectorSpiService _collectorSPIService) {
        this.collectorSPIService = _collectorSPIService;
        this.manager = SyslogDatastoreManager.getInstance(coreThreadPoolSize, maxThreadPoolSize, keepAliveTime, queueSize);
        new SyslogProcessor().start();
    }

    public void setUdpPort(int udpPort) {
        this.udpPort = udpPort;
    }

    public void setTcpPort(int tcpPort) {
        this.tcpPort = tcpPort;
    }

    public void setCoreThreadPoolSize(int coreThreadPoolSize) {
        this.coreThreadPoolSize = coreThreadPoolSize;
    }

    public void setKeepAliveTime(long keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    public void setMaxThreadPoolSize(int maxThreadPoolSize) {
        this.maxThreadPoolSize = maxThreadPoolSize;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }

    public void setManager(SyslogDatastoreManager manager) {
        this.manager = manager;
    }

    public boolean isRunning() {
        return this.running;
    }


    /**
     * initiated when the data binding broker is registered
     * in TSDRSyslogModule
     *
     * @param session binding aware broker's provider context
     */
    @Override
    public void onSessionInitiated(BindingAwareBroker.ProviderContext session) {
        this.dataBroker = session.getSALService(DataBroker.class);

        manager.setDataBroker(dataBroker);
        logger.info("Datastore Manager Setup Done");
        this.syslogsvrService = session.addRpcImplementation(TsdrSyslogCollectorService.class, manager);
        logger.info("Register SyslogsvrService to Session.");

        logger.info("Syslog Collector Session Initiated");

        //Start TCP syslog server
        logger.info("Start TCP server");
        try {
            tcpServer = new SyslogTCPServer(this.messageList);
            tcpServer.setPort(tcpPort);
            tcpServer.startServer();
            logger.info("TCP server started at port: " + tcpPort + ".");
        } catch (Exception e) {
            logger.error(e.getMessage());
            this.close();
        }


        //Test If port is available
        boolean udpPortAvailable = false;
        try {
            DatagramSocket socket = new DatagramSocket(udpPort);
            socket.close();
            socket = null;
            udpPortAvailable = true;
        } catch (Exception e) {
            logger.error("Port " + udpPort + " is not available for UDP, trying backup...");
            try {
                udpPort += 1000;
                DatagramSocket socket = new DatagramSocket(udpPort);
                socket.close();
                socket = null;
                udpPortAvailable = true;
            } catch (Exception err) {
                this.close();
                logger.error("Port " + udpPort + " is not available, not starting servers!");
            }
        }

        if (udpPortAvailable) {
            //Start UDP syslog server
            logger.info("Start UDP server");
            try {
                udpServer = new SyslogUDPServer(messageList);
                udpServer.setPort(udpPort);
                udpServer.startServer();
                logger.info("UDP server started at port: " + udpPort + ".");
            } catch (Exception e) {
                logger.error("Failed to start UDP server on port " + udpPort, e);
            }
        }
    }

    public void close() {
        running = false;
        try {
            if (tcpServer != null)
                tcpServer.stopServer();
            if (udpServer != null)
                udpServer.stopServer();
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }
    }

    private class SyslogProcessor extends Thread {

        public SyslogProcessor() {
            super("TSDR Syslog Processor");
            this.setDaemon(true);
        }

        public void run() {
            Message message = null;
            while (running) {
                synchronized (messageList) {
                    if (messageList.isEmpty()) {
                        try {
                            messageList.wait(QUEUE_WAIT_INTERVAL);
                        } catch (InterruptedException e) {
                        }
                    }
                    if (!messageList.isEmpty()) {
                        message = messageList.remove(0);
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
                        if (System.currentTimeMillis() - lastPersisted > STORE_FLUSH_INTERVAL && !syslogQueue.isEmpty()) {
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
