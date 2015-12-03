/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 * Copyright (c) 2015 xFlow Research Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.netflow;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrlog.RecordAttributes;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrlog.RecordAttributesBuilder;
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
 *
 * Created: December 1, 2015
 *
 */
public class TSDRNetflowCollectorImpl extends Thread{
    private TsdrCollectorSpiService collectorSPIService = null;
    private DatagramSocket socket = null;
    private boolean running = true;
    private static final Logger logger = LoggerFactory.getLogger(TSDRNetflowCollectorImpl.class);
    private LinkedList<DatagramPacket> incomingNetFlow;
    private LinkedList<TSDRLogRecord> netFlowQueue = new LinkedList<TSDRLogRecord>();
    private long lastPersisted = System.currentTimeMillis();
    private static final long PERSIST_CHECK_INTERVAL_IN_MILLISECONDS = 5000;
    private static final long INCOMING_QUEUE_WAIT_INTERVAL_IN_MILLISECONDS = 2000;

    /**
     * Constructor
     * @param _collectorSPIService
     */
    public TSDRNetflowCollectorImpl(TsdrCollectorSpiService _collectorSPIService){
        super("TSDR NetFlow Listener");
        this.collectorSPIService = _collectorSPIService;
        incomingNetFlow = new LinkedList<DatagramPacket>();
        try
        {
            socket = new DatagramSocket(2055);
            logger.info("NetFlow collector started listening on port 2055");
            logger.info("NetFlow incoming queue initialized. Size: " + incomingNetFlow.size());
            this.start();
            new NetFlowProcessor();
        }
        catch(Exception e){
            logger.error("NetFlow collector failed to bind to port 2055, possibly due to controller not running on root user.");
            try{
                socket = new DatagramSocket(2055);
                logger.info("NetFlow collector started on listening on port 2055, please make sure you have port forwarding from 514 to this port setup");
                this.start();
                new NetFlowProcessor();
            }
            catch(Exception err){
                logger.error("Failed to bind to port 2055, netflow collector will shutdonw.",err);
            }
        }
    }

    public void run(){
        while(running){
            byte data[] = new byte[1024];
            DatagramPacket packet = new DatagramPacket(data,data.length);
            try{
                socket.receive(packet);
                handleNetFlow(packet);
            }
            catch(IOException e){
                logger.error("Error while handleling netflow packets.",e);
            }
        }
    }

    public void close(){
        running = false;
        socket.close();
    }

    public void handleNetFlow(DatagramPacket packet){
        synchronized(incomingNetFlow){
            incomingNetFlow.add(packet);
            incomingNetFlow.notifyAll();
        }
    }

    private class NetFlowProcessor extends Thread{
        public NetFlowProcessor(){
            super("TSDR NetFlow Processor");
            this.start();
        }
        public void run(){
            DatagramPacket packet = null;
            while(running){
                synchronized(incomingNetFlow){
                    if(incomingNetFlow.isEmpty()){
                        logger.debug("No Pkts in queue");
                        try{
                            incomingNetFlow.wait(INCOMING_QUEUE_WAIT_INTERVAL_IN_MILLISECONDS);
                        }
                        catch(InterruptedException e){
                    }

                    if(!incomingNetFlow.isEmpty()){
                        logger.debug("Pkts found");
                        packet = incomingNetFlow.removeFirst();
                        byte[] buff = packet.getData();
                        String srcIp = packet.getAddress().getHostAddress().trim();
                        String version =  convert(buff, 0, 2);
                        String sysUpTime = convert(buff, 4, 4);
                        String unix_secs = convert(buff, 8, 4);
                        String unix_nsecs = convert(buff, 12, 4);
                        String flow_sequence = convert(buff, 16, 4);
                        String engine_type = Byte.toString(buff[20]);
                        String engine_id = Byte.toString(buff[21]);
                        String srcAddr = convert(buff, 24, 4);
                        String dstAddr = convert(buff, 28, 4);
                        String nextHop = convert(buff, 32, 4);
                        String input = convert(buff, 36, 2);
                        String output = convert(buff, 38, 2);
                        String dPkts = convert(buff, 40, 4);
                        String dOctets = convert(buff, 44, 4);
                        String First = convert(buff, 48, 4);
                        String Last = convert(buff, 52, 4);
                        String srcPort = convert(buff, 56, 2);
                        String dstPort = convert(buff, 58, 2);
                        String tcpFlags = Byte.toString(buff[61]);
                        String protocol = Byte.toString(buff[62]);
                        String tos = Byte.toString(buff[63]);
                        String srcAS = convert(buff, 64, 2);
                        String dstAS = convert(buff, 66, 2);
                        String srcMask = Byte.toString(buff[68]);
                        String dstMask = Byte.toString(buff[69]);
                        String flowDuration = new Long(Long.parseLong(Last) - Long.parseLong(First)).toString();
                        /*Build the RecordFullText*/
                        String recordFullText = constructRecordFullText(version, sysUpTime, unix_secs,
                            unix_nsecs, flow_sequence, engine_type, engine_id, flowDuration, srcAddr, dstAddr, nextHop,
                            input, output, dPkts, dOctets, First, Last,srcPort,dstPort,tcpFlags, protocol,tos, srcAS, 
                            dstAS, srcMask, dstMask);

                        /*Fill up the RecordBuilder object*/
                        TSDRLogRecordBuilder recordbuilder = new TSDRLogRecordBuilder();
                        recordbuilder.setNodeID(srcIp);
                        recordbuilder.setTimeStamp(System.currentTimeMillis());
                        recordbuilder.setTSDRDataCategory(DataCategory.NETFLOW);
                        recordbuilder.setRecordFullText(recordFullText);
                        logger.debug("************************");
                        logger.debug(recordFullText);
                        logger.debug("************************");
                        /*List of Attributes (key/value pair) format*/
                        List<RecordAttributes> attributeList  = createAttributeList(srcIp, version,
                                flowDuration, srcAddr, dstAddr, nextHop,input, output, dPkts, dOctets,
                                tcpFlags, protocol);
                        recordbuilder.setRecordAttributes(attributeList);
                        TSDRLogRecord logRecord =  recordbuilder.build();

                        if(logRecord!=null){
                            netFlowQueue.add(logRecord);
                        }
                        if(System.currentTimeMillis()-lastPersisted>PERSIST_CHECK_INTERVAL_IN_MILLISECONDS
                            && !netFlowQueue.isEmpty()){
                            LinkedList<TSDRLogRecord> queue = null;
                            if(System.currentTimeMillis()-lastPersisted>PERSIST_CHECK_INTERVAL_IN_MILLISECONDS
                                && !netFlowQueue.isEmpty()){
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
    }
    /**
     * Construct a RecordFullText from netFlow attributes
     *
     * @return
     */
    private String constructRecordFullText(String version, String sysUpTime, String unix_secs,
        String unix_nsecs, String flow_sequence, String engine_type, String engine_id,
        String flowDuration, String srcAddr, String dstAddr, String nextHop,
        String input, String output, String dPkts, String dOctets, String First,
        String Last,String srcPort,String dstPort,String tcpFlags, String protocol,
        String tos, String srcAS, String dstAS, String srcMask, String dstMask){
        StringBuffer recordFullTextBuffer = new StringBuffer();
        recordFullTextBuffer.append("version=")
         .append(version)
         .append(",sys_uptime=")
         .append(sysUpTime)
         .append(",unix_secs=")
         .append(unix_secs)
         .append(",unix_nsecs=")
         .append(unix_nsecs)
         .append(",flow_sequence=")
         .append(flow_sequence)
         .append(",engine_type=")
         .append(engine_type)
         .append(",engine_id=")
         .append(engine_id)
         .append(",flow_duration=")
         .append(flowDuration)
         .append(",srcaddr=")
         .append(srcAddr)
         .append(",dstaddr=")
         .append(dstAddr)
         .append(",nexthop=")
         .append(nextHop)
         .append(",input=")
         .append(input)
         .append(",output=")
         .append(output)
         .append(",dPkts=")
         .append(dPkts)
         .append(",dOctets=")
         .append(dOctets)
         .append(",aFirst=")
         .append(First)
         .append(",aLast=")
         .append(Last)
         .append(",srcPort=")
         .append(srcPort)
         .append(",dstPort=")
         .append(dstPort)
         .append(",tcp_flags=")
         .append(tcpFlags)
         .append(",prot=")
         .append(protocol)
         .append(",tos=")
         .append(tos)
         .append(",srcAS=")
         .append(srcAS)
         .append(",dstAS=")
         .append(dstAS)
         .append(",srcMask=")
         .append(srcMask)
         .append(",dstMask=")
         .append(dstMask);
         return(recordFullTextBuffer.toString());
      }

    }

    /**
     * Create RecordAttribute list based on NetFlow attributes.
     * @param srcIp
     * @param version
     * @param flowDuration
     * @param srcAddr
     * @param dstAddr
     * @param nextHop
     * @param input
     * @param output
     * @param dPkts
     * @param dOctets
     * @param tcpFlags
     * @param protocol
     * @return
     */
    private List<RecordAttributes> createAttributeList(String srcIp, String version,
            String flowDuration, String srcAddr, String dstAddr, String nextHop,
            String input, String output, String dPkts, String dOctets,String tcpFlags, String protocol){
        List<RecordAttributes> attributeList = new ArrayList<RecordAttributes>();
        RecordAttributesBuilder attributeBuilder = new RecordAttributesBuilder();
        attributeBuilder.setName("exporterIP");
        attributeBuilder.setValue(srcIp);
        attributeList.add(attributeBuilder.build());
        attributeBuilder.setName("version");
        attributeBuilder.setValue(version);
        attributeList.add(attributeBuilder.build());
        attributeBuilder.setName("flowDuration");
        attributeBuilder.setValue(flowDuration);
        attributeList.add(attributeBuilder.build());
        attributeBuilder.setName("srcAddr");
        attributeBuilder.setValue(srcAddr);
        attributeList.add(attributeBuilder.build());
        attributeBuilder.setName("dstAddr");
        attributeBuilder.setValue(dstAddr);
        attributeList.add(attributeBuilder.build());
        attributeBuilder.setName("nextHop");
        attributeBuilder.setValue(nextHop);
        attributeList.add(attributeBuilder.build());
        attributeBuilder.setName("inputPort");
        attributeBuilder.setValue(input);
        attributeList.add(attributeBuilder.build());
        attributeBuilder.setName("outputPort");
        attributeBuilder.setValue(output);
        attributeList.add(attributeBuilder.build());
        attributeBuilder.setName("flowPkts");
        attributeBuilder.setValue(dPkts);
        attributeList.add(attributeBuilder.build());
        attributeBuilder.setName("flowBytes");
        attributeBuilder.setValue(dOctets);
        attributeList.add(attributeBuilder.build());
        attributeBuilder.setName("tcpFlags");
        attributeBuilder.setValue(tcpFlags);
        attributeList.add(attributeBuilder.build());
        attributeBuilder.setName("prot");
        attributeBuilder.setValue(protocol);
        attributeList.add(attributeBuilder.build());
        return attributeList;
    }

    private String convert(byte[] p, int off, int len){
        long ret = 0;
        int done = off + len;
        for (int i = off; i < done; i++){
            ret = ((ret << 8) & 0xffffffff) + (p[i] & 0xff);
        }
        return (new Long(ret)).toString();
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
