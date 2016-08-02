/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2015 xFlow Research Inc. and others, All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.netflow;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;

import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRLogRecordInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecordBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NetFlow Collector to receive netflow packets and store into TSDR data store.
 *
 * Currently only NetFlow Version 5 is supported.
 *
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 * @author <a href="mailto:muhammad.umair@xflowresearch.com">Umair Bhatti</a>
 * @author <a href="mailto:saichler@xgmail.com">Sharon Aicler</a>
 *
 * Created: December 1, 2015
 * Modified: Aug 02, 2016
 */
public class TSDRNetflowCollectorImpl extends Thread{

    private static final long PERSIST_CHECK_INTERVAL_IN_MILLISECONDS = 5000;
    private static final long INCOMING_QUEUE_WAIT_INTERVAL_IN_MILLISECONDS = 2000;
    private static final int FLOW_SIZE_FOR_NETFLOW_PACKET = 48;
    private static final Logger logger = LoggerFactory.getLogger(TSDRNetflowCollectorImpl.class);
    private static byte packetsCountForTests = 0; //Just to test the counts of packet for test
    private final TsdrCollectorSpiService collectorSPIService;
    private DatagramSocket socket;
    private boolean running = true;
    private final LinkedList<DatagramPacket> incomingNetFlow = new LinkedList<>();
    private long lastPersisted = System.currentTimeMillis();
    private long lastTimeStamp = System.currentTimeMillis();
    private int logRecordIndex = 0;
    /**
     * Constructor
     * @param _collectorSPIService
     */
    public TSDRNetflowCollectorImpl(TsdrCollectorSpiService _collectorSPIService) throws IOException{
        super("TSDR NetFlow Listener");
        this.setDaemon(true);
        logRecordIndex = 0;
        this.collectorSPIService = _collectorSPIService;
        try{
            this.socket = new DatagramSocket(2055);
        }catch(Exception e){
            logger.error("Collector service already running. Failed to bind it again on Port 2055.");
            //Collector service already running, just passing the code for testing purpose.
            //this.socket = new DatagramSocket();
          shutdown();
        }
        this.start();
        new NetFlowProcessor();
    }
    public long getIncomingNetflowSize(){
        return incomingNetFlow.size();
    }
    public byte getPacketCount(){
        return packetsCountForTests;
    }
    public void run(){
        if(this.socket==null || this.socket.isClosed()){
            shutdown();
        }else {
            while (running) {
                byte data[] = new byte[1024];
                DatagramPacket packet = new DatagramPacket(data, data.length);
                try {
                    socket.receive(packet);
                    handleNetFlow(packet);
                } catch (IOException e) {
                    logger.error("Error while handleling netflow packets.", e);
                    shutdown();
                }
            }
        }
    }
    public void shutdown(){
        running = false;
        if(socket!=null){
            socket.close();
        }
        synchronized(incomingNetFlow){
            incomingNetFlow.notifyAll();
        }
    }

    public void handleNetFlow(DatagramPacket packet){
        synchronized(incomingNetFlow){
            incomingNetFlow.add(packet);
            packetsCountForTests = (byte) (((int)packetsCountForTests) + 1);
            incomingNetFlow.notifyAll();
        }
    }

    private class NetFlowProcessor extends Thread{
        private TSDRLogRecordBuilder recordbuilder;
        public NetFlowProcessor(){
            super("TSDR NetFlow Processor");
            this.setDaemon(true);
            this.start();
            logger.debug("NetFlow Processor thread initialized");
        }
        public long getIncomingNetflowSize(){
            return incomingNetFlow.size();
        }
        public byte getPacketCount(){
            return packetsCountForTests;
        }
        public void run(){
            DatagramPacket packet = null;
            LinkedList<TSDRLogRecord> netFlowQueue = new LinkedList<TSDRLogRecord>();
            while(running){
                synchronized(incomingNetFlow) {
                    if (incomingNetFlow.isEmpty()) {
                        logger.debug("No Pkts in queue");
                        try {
                            incomingNetFlow.wait(INCOMING_QUEUE_WAIT_INTERVAL_IN_MILLISECONDS);
                        } catch (InterruptedException e) {
                            logger.error("Interrupted while waiting on incoming queue", e);
                        }
                    }
                    if (!incomingNetFlow.isEmpty()) {
                        packet = incomingNetFlow.removeFirst();
                    }
                }
                if(packet != null){
                    logger.debug("Pkts found");
                    byte[] buff = packet.getData();
                    String srcIp = packet.getAddress().getHostAddress().trim();
                    int netFlowVersion = new Integer(NetflowPacketParser.convert(buff, 0, 2)).intValue();
                    long currentTimeStamp = System.currentTimeMillis();
                    if(netFlowVersion == 9){
                        int flowCount = new Integer(NetflowPacketParser.convert(buff, 2, 2)).intValue();
                        int flowCounter = 1;
                        int dataBufferOffset = 20;
                        int flowsetid = Integer.parseInt(NetflowPacketParser.convert(buff, dataBufferOffset, 2));
                        int flowsetLength = Integer.parseInt(NetflowPacketParser.convert(buff, dataBufferOffset + 2, 2));
                        if(flowsetid == 0){
                            dataBufferOffset += 4;
                            NetflowPacketParser.fillFlowSetTemplateMap(buff, dataBufferOffset, flowCount);
                            dataBufferOffset += flowsetLength;
                            flowsetid = Integer.parseInt(NetflowPacketParser.convert(buff, dataBufferOffset, 2));
                            flowsetLength = Integer.parseInt(NetflowPacketParser.convert(buff, dataBufferOffset + 2, 2));
                            dataBufferOffset += 4;
                        }
                        int packetLength = (flowsetLength - 4) / flowCount;
                        while(flowCounter <= flowCount && (dataBufferOffset + packetLength) < buff.length){
                            recordbuilder = new TSDRLogRecordBuilder();
                            NetflowPacketParser parser = new NetflowPacketParser(buff);
                            parser.addFormat(buff, dataBufferOffset);
                            /*Fill up the RecordBuilder object*/
                            recordbuilder.setNodeID(srcIp);
                            recordbuilder.setTimeStamp(currentTimeStamp);
                            recordbuilder.setIndex(flowCounter);
                            recordbuilder.setTSDRDataCategory(DataCategory.NETFLOW);
                            recordbuilder.setRecordFullText(parser.toString());
                            logger.info(parser.toString());
                            if(logger.isDebugEnabled()) {
                                logger.debug(parser.toString());
                            }
                            recordbuilder.setRecordAttributes(parser.getRecordAttributes());
                            TSDRLogRecord logRecord =  recordbuilder.build();
                            if(logRecord!=null){
                                netFlowQueue.add(logRecord);
                            }
                            dataBufferOffset += packetLength;
                            flowCounter += 1;
                        }
                    }else{
                        int flowCount = new Integer(NetflowPacketParser.convert(buff, 2, 2)).intValue();
                        int flowCounter = 1;
                        int dataBufferOffset = 0;
                        while(flowCounter <= flowCount && (dataBufferOffset + FLOW_SIZE_FOR_NETFLOW_PACKET) < buff.length){
                            recordbuilder = new TSDRLogRecordBuilder();
                            NetflowPacketParser parser = new NetflowPacketParser(buff);
                            parser.addFormat(buff, dataBufferOffset);
                            dataBufferOffset += FLOW_SIZE_FOR_NETFLOW_PACKET;
                            /*Fill up the RecordBuilder object*/
                            recordbuilder.setNodeID(srcIp);
                            recordbuilder.setTimeStamp(currentTimeStamp);
                            recordbuilder.setIndex(flowCounter);
                            recordbuilder.setTSDRDataCategory(DataCategory.NETFLOW);
                            recordbuilder.setRecordFullText(parser.toString());
                            if(logger.isDebugEnabled()){
                                logger.debug(parser.toString());
                            }
                            recordbuilder.setRecordAttributes(parser.getRecordAttributes());
                            TSDRLogRecord logRecord =  recordbuilder.build();
                            if(logRecord!=null){
                                netFlowQueue.add(logRecord);
                            }
                            flowCounter += 1;
                        }
                    }
                    if(System.currentTimeMillis() - lastPersisted > PERSIST_CHECK_INTERVAL_IN_MILLISECONDS && !netFlowQueue.isEmpty() && packet != null){ 
                        LinkedList<TSDRLogRecord> queue = null;
                        if(System.currentTimeMillis() - lastPersisted > PERSIST_CHECK_INTERVAL_IN_MILLISECONDS && !netFlowQueue.isEmpty()){
                            lastPersisted = System.currentTimeMillis();
                            queue = netFlowQueue;
                            netFlowQueue = new LinkedList<TSDRLogRecord>();
                        }
                        if(queue!=null){
                            store(queue);
                        }
                    }
                    packet = null;
                }
            }
        }
    }
   /**
     * Store the data into TSDR data store
     * @param queue
    */
    private void store(List<TSDRLogRecord> queue){
        InsertTSDRLogRecordInputBuilder input = new InsertTSDRLogRecordInputBuilder();
        input.setTSDRLogRecord(queue);
        input.setCollectorCodeName("TSDRNetFlowCollector");
        collectorSPIService.insertTSDRLogRecord(input.build());
    }
}