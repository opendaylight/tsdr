/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.syslogs;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.LinkedList;

import org.opendaylight.tsdr.syslogs.filters.SyslogFilterManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRLogRecordInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class TSDRSyslogCollectorImpl extends Thread{
    private TsdrCollectorSpiService collectorSPIService = null;
    private DatagramSocket socket = null;
    private boolean running = true;
    private Logger logger = LoggerFactory.getLogger(TSDRSyslogCollectorImpl.class);
    private LinkedList<DatagramPacket> incomingSyslogs = new LinkedList<DatagramPacket>();
    private LinkedList<TSDRLogRecord> syslogQueue = new LinkedList<TSDRLogRecord>();
    private SyslogFilterManager filterManager = new SyslogFilterManager();
    private long lastPersisted = System.currentTimeMillis();

    public TSDRSyslogCollectorImpl(TsdrCollectorSpiService _collectorSPIService){
        super("TSDR Syslog Listener");
        this.collectorSPIService = _collectorSPIService;
        try{
            socket = new DatagramSocket(514);
            logger.info("Syslog collector started on listening on port 514");
            this.start();
            new SyslogProcessor().start();
        }catch(Exception e){
            logger.error("Syslog collector failed to bind to port 514, possibly due to controller not running on root user.");
            try{
                socket = new DatagramSocket(1514);
                logger.info("Syslog collector started on listening on port 1514, please make sure you have port forwarding from 514 to this port setup");
                this.start();
                new SyslogProcessor().start();
            }catch(Exception err){
                logger.error("Failed to bind to port 514 & 1514, syslog collector will shutdonw.",err);
            }
        }
    }
    public void run(){
        while(running){
            byte data[] = new byte[1024];
            DatagramPacket packet = new DatagramPacket(data,data.length);
            try {
                socket.receive(packet);
                handleSyslog(packet);
            } catch (IOException e) {
                logger.error("Error while handleling syslog",e);
            }
        }
    }

    public void close(){
        running = false;
        socket.close();
    }

    public void handleSyslog(DatagramPacket packet){
        synchronized(this.incomingSyslogs){
            this.incomingSyslogs.add(packet);
            this.incomingSyslogs.notifyAll();
        }
    }

    private class SyslogProcessor extends Thread {
        public SyslogProcessor(){
            super("TSDR Syslog Processor");
        }
        public void run(){
            DatagramPacket packet = null;
            while(running){
                synchronized(incomingSyslogs){
                    if(incomingSyslogs.isEmpty()){
                        try{incomingSyslogs.wait(2000);}catch(InterruptedException e){}
                    }
                    if(!incomingSyslogs.isEmpty()){
                        packet = incomingSyslogs.removeFirst();
                    }
                }
                TSDRLogRecord logRecord = filterManager.applyFilters(packet);
                if(logRecord!=null){
                    syslogQueue.add(logRecord);
                }
                if(System.currentTimeMillis()-lastPersisted>5000 && !syslogQueue.isEmpty()){
                    LinkedList<TSDRLogRecord> queue = null;
                    synchronized(filterManager){
                        if(System.currentTimeMillis()-lastPersisted>5000 && !syslogQueue.isEmpty()){
                            lastPersisted = System.currentTimeMillis();
                            queue = syslogQueue;
                            syslogQueue = new LinkedList<TSDRLogRecord>();
                        }
                    }
                    if(queue!=null){
                        store(queue);
                    }
                }
                packet = null;
            }
        }
    }

    private void store(LinkedList<TSDRLogRecord> queue){
        InsertTSDRLogRecordInputBuilder input = new InsertTSDRLogRecordInputBuilder();
        input.setTSDRLogRecord(queue);
        input.setCollectorCodeName("SyslogCollector");
        collectorSPIService.insertTSDRLogRecord(input.build());
    }
}
