/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.collectors.cmc;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

import org.datasand.codec.TypeDescriptorsContainer;
import org.datasand.codec.bytearray.ByteArrayEncodeDataContainer;
import org.datasand.codec.bytearray.ByteEncoder;
import org.opendaylight.tsdr.collectors.cmc.mdsal.Counter64Serializer;
import org.opendaylight.tsdr.collectors.cmc.mdsal.DataCategorySerializer;
import org.opendaylight.tsdr.collectors.cmc.mdsal.MDSALClassExtractor;
import org.opendaylight.tsdr.collectors.cmc.mdsal.MDSALMethodFilter;
import org.opendaylight.tsdr.collectors.cmc.mdsal.MDSALObjectTypeRule;
import org.opendaylight.tsdr.collectors.cmc.mdsal.MDSalAugmentationObserver;
import org.opendaylight.tsdr.collectors.cmc.mdsal.MDSalObjectChildRule;
import org.opendaylight.tsdr.collectors.cmc.mdsal.RecordKeysSerializer;
import org.opendaylight.tsdr.collectors.cmc.mdsal.TSDRMetricRecordSerializer;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Counter64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class CollectorService implements ICollectorService, Runnable{

    private static Logger log = LoggerFactory.getLogger(CollectorService.class);
    private static CollectorService instance = new CollectorService();
    private ICollectorService localCollectorService = null;
    private TypeDescriptorsContainer typeDescriptorContainer = new TypeDescriptorsContainer("./src/main/java/resources");
    private boolean wasMDSALInitialized = false;
    private ServerSocket socket = null;
    private boolean running = true;

    private CollectorService(){
    }

    public static CollectorService getInstance(){
        return instance;
    }

    public void setLocalCollectorService(ICollectorService localService){
        this.localCollectorService = localService;
    }

    @Override
    public void store(TSDRMetricRecord record) {
        if(this.localCollectorService!=null){
            this.localCollectorService.store(record);
        }else{
            if(!wasMDSALInitialized){
                initMDSALSerialization();
            }
            ByteArrayEncodeDataContainer ba = new ByteArrayEncodeDataContainer(new byte[256], typeDescriptorContainer.getTypeDescriptorByClass(TSDRMetricRecord.class));
            ba.getEncoder().encodeObject(record, ba);
            sendBytes(ba.getData());
        }
    }

    @Override
    public void store(List<TSDRMetricRecord> recordList) {
        if(this.localCollectorService!=null){
            this.localCollectorService.store(recordList);
        }else{
            if(!wasMDSALInitialized){
                initMDSALSerialization();
            }
            ByteArrayEncodeDataContainer ba = new ByteArrayEncodeDataContainer(new byte[256], typeDescriptorContainer.getTypeDescriptorByClass(TSDRMetricRecord.class));
            ba.getEncoder().encodeObject(recordList, ba);
            sendBytes(ba.getData());
        }
    }

    private void sendBytes(byte[] metricData){
    }

    public void initMDSALSerialization(){
        typeDescriptorContainer.clearChildAttributeObservers();
        typeDescriptorContainer.addChildAttributeObserver(new MDSalObjectChildRule());
        typeDescriptorContainer.addTypeAttributeObserver(new MDSALObjectTypeRule());
        typeDescriptorContainer.setClassExtractor(new MDSALClassExtractor());
        typeDescriptorContainer.addMethodFilterObserver(new MDSALMethodFilter());
        typeDescriptorContainer.setAugmentationObserver(new MDSalAugmentationObserver());
        ByteEncoder.registerSerializer(TSDRMetricRecord.class, new TSDRMetricRecordSerializer(), 900);
        ByteEncoder.registerSerializer(DataCategory.class, new DataCategorySerializer(), 901);
        ByteEncoder.registerSerializer(RecordKeys.class, new RecordKeysSerializer(), 902);
        ByteEncoder.registerSerializer(Counter64.class, new Counter64Serializer(), 903);
        wasMDSALInitialized = true;
    }

    protected void initAsServer(){
        try{
            socket = new ServerSocket(18080);
            new Thread(this,"TSDR MDSAL Server");
        }catch(Exception e){
            log.error("Failed to start collector service socket",e);
        }
    }

    public void run(){
        while(running){
            try{
                Socket s = socket.accept();
                new Connection(s);
            }catch(Exception err){
                log.error("Server Socket problem, exiting",err);
                break;
            }
        }
    }

    private class Connection extends Thread{
        private Socket s = null;
        private BufferedInputStream in = null;
        private BufferedOutputStream out = null;
        private ObjectProcessor obp = null;

        private Connection(Socket _s){
            super("TSDR Service Connection from "+_s.getInetAddress().getHostName()+":"+_s.getPort());
            this.s = _s;
            try{
                in = new BufferedInputStream(new DataInputStream(s.getInputStream()));
                out = new BufferedOutputStream(new DataOutputStream(s.getOutputStream()));
                obp = new ObjectProcessor(this);
            }catch(IOException e){
                log.error("Error opening Streams",e);
            }
            this.start();
        }

        public void run(){
            while(running){
                try{
                    byte dataSize[] = new byte[4];
                    in.read(dataSize);
                    int size = ByteEncoder.decodeInt32(dataSize,0);
                    byte objectData[] = new byte[size];
                    in.read(objectData);
                    obp.addObject(objectData);
                }catch(Exception err){
                    log.error("Socket Read Problem",err);
                }
            }
        }
    }

    private class ObjectProcessor extends Thread {
        private List<byte[]> queue = new LinkedList<byte[]>();
        public ObjectProcessor(Connection _conn){
            super("OP for "+_conn.getName());
            this.start();
        }

        public void addObject(byte[] obj){
            synchronized(queue){
                queue.add(obj);
                queue.notifyAll();
            }
        }

        public void run(){
            byte data[] = null;
            while(running){
                synchronized(queue){
                    if(queue.isEmpty()){
                        try{queue.wait(2000);}catch(InterruptedException e){}
                    }else{
                        data = queue.remove(0);
                    }
                }
                if(data!=null){
                    if(!wasMDSALInitialized){
                        initMDSALSerialization();
                    }
                    ByteArrayEncodeDataContainer container = new ByteArrayEncodeDataContainer(data, typeDescriptorContainer.getTypeDescriptorByClass(TSDRMetricRecord.class));
                    Object obj = container.getEncoder().decodeObject(container);
                    if(obj instanceof TSDRMetricRecord){
                        store((TSDRMetricRecord)obj);
                    }else{
                        store((List)obj);
                    }
                }
                data = null;
            }
        }
    }

    /**
     * A method to be used once to generate the serializers
     * Look @ the commeted code in the main method 
     **
    private void generate(){
        typeDescriptorContainer.getTypeDescriptorByClass(TSDRMetricRecord.class);
    }
    public static void main(String args[]){
       CollectorService.getInstance().initMDSALSerialization();
       CollectorService.getInstance().generate();
    }**/
}
