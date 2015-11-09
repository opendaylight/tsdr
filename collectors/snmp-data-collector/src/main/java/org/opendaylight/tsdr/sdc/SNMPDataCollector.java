/*
 * Copyright (c) 2015 Tata Consultancy Services, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.sdc;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.math.BigInteger;
import java.math.BigDecimal;
import java.util.concurrent.Future;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.GetInterfacesInputBuilder;

import org.opendaylight.controller.config.yang.config.tsdr.snmp.data.collector.TSDRSDCModule;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRMetricRecordInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRMetricRecordInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.snmp.data.collector.rev151013.SetPollingIntervalInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.snmp.data.collector.rev151013.TSDRSDCConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.snmp.data.collector.rev151013.TSDRSDCConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.snmp.data.collector.rev151013.TsdrSnmpDataCollectorService;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.smiv2._if.mib.rev000614.interfaces.group.IfEntry;
import org.opendaylight.snmp.plugin.internal.SNMPImpl;

import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.GetInterfacesOutput;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;


/**
 * @author Prajaya Talwar(prajaya.talwar@tcs.com)
 **/
public class SNMPDataCollector implements TsdrSnmpDataCollectorService {
    private static final Logger logger = LoggerFactory.getLogger(SNMPDataCollector.class);
    private TSDRSDCModule module = null;
    // A reference to the data broker
    private DataBroker dataBroker = null;
    private TSDRMetricRecordBuilderContainer container = new TSDRMetricRecordBuilderContainer();
    private boolean running = true;
    // The reference to the the RPC registry to store the data
    private RpcProviderRegistry rpcRegistry = null;
    private static boolean logToExternalFile = false;
    private TSDRSDCConfig config = null;
    protected Object pollerSyncObject = new Object();
    private TsdrCollectorSpiService collectorSPIService = null;
    private static final String COLLECTOR_CODE_NAME = SNMPDataCollector.class.getSimpleName();

    public SNMPDataCollector(DataBroker _dataBroker,
            RpcProviderRegistry _rpcRegistry) {
        log("SNMP Collector Started", INFO);
        this.dataBroker = _dataBroker;
        this.rpcRegistry = _rpcRegistry;

        TSDRSDCConfigBuilder b = new TSDRSDCConfigBuilder();
        b.setPollingInterval(15000l);
        b.setCommunity("mib2dev/if-mib");
        b.setIpAddress(new Ipv4Address("127.0.0.1"));
        this.config = b.build();
        new TSDRSNMPInterfacePoller(this);
        new StoringThread();
    }

    public void loadConfigData() {
        // try to load the configuration data from the configuration data store
        ReadOnlyTransaction rot = null;
        try {
            InstanceIdentifier<TSDRSDCConfig> cid = InstanceIdentifier
                    .create(TSDRSDCConfig.class);
            rot = this.dataBroker.newReadOnlyTransaction();
            CheckedFuture<Optional<TSDRSDCConfig>, ReadFailedException> read = rot
                    .read(LogicalDatastoreType.CONFIGURATION, cid);
            if (read != null && read.get() != null) {
                if (read.get().isPresent()) {
                    this.config = read.get().get();
                }
            }
        } catch (Exception err) {
            log("Failed to read TSDR Data Collection configuration from data store, using defaults.",
                    WARNING);
        } finally {
            if (rot != null) {
                rot.close();
            }
        }
    }

    public void loadGetInterfacesData() {
        // fetch data from getInterfaces

        SNMPImpl snmpImpl = new SNMPImpl(rpcRegistry);
        try 
        {
            //this.config.setCommunity("mib2dev/if-mib");
            //this.config.setIpAddress(new Ipv4Address("127.0.0.1"));
            Ipv4Address ip = this.config.getIpAddress();
            GetInterfacesInputBuilder input = new GetInterfacesInputBuilder();
            input.setCommunity("mib2dev/if-mib");
            input.setIpAddress(new Ipv4Address("127.0.0.1"));
            //System.out.println();
            RpcResult<GetInterfacesOutput> result = null;
            Future<RpcResult<GetInterfacesOutput>> resultFuture = snmpImpl.getInterfaces(input.build());
            result = resultFuture.get();
            result.isSuccessful();

            TSDRSDCConfigBuilder b = new TSDRSDCConfigBuilder();
            b.setCommunity(this.config.getCommunity());
            b.setIpAddress(this.config.getIpAddress());
            b.setPollingInterval(this.config.getPollingInterval());
            b.setIfNumber(result.getResult().getIfNumber());
            b.setIfEntry(result.getResult().getIfEntry());

            this.config = b.build();

        }
        catch (Exception err) {
            err.printStackTrace();
        }

    }

    public void insertInterfacesEnteries(){

        for(IfEntry entry : this.config.getIfEntry())
        {

        TSDRMetricRecordBuilder b = new TSDRMetricRecordBuilder();
        b.setMetricName(entry.getIfIndex().toString());
        b.setTSDRDataCategory(DataCategory.SNMPINTERFACES);
        b.setNodeID(this.config.getIpAddress().toString());
        
        RecordKeysBuilder recordKeyB = new RecordKeysBuilder();
        recordKeyB.setKeyName(COLLECTOR_CODE_NAME);
        recordKeyB.setKeyValue(entry.getIfIndex().toString());
        ArrayList<RecordKeys> list =new ArrayList<RecordKeys>();
        list.add(recordKeyB.build());
        b.setRecordKeys(list);

        b.setTimeStamp(System.currentTimeMillis());
        b.setMetricValue(new BigDecimal(entry.getIfMtu()));
        container.addBuilder(b);

        }
    }

    public void saveConfigData() {
        try {
            InstanceIdentifier<TSDRSDCConfig> cid = InstanceIdentifier
                    .create(TSDRSDCConfig.class);
            WriteTransaction wrt = this.dataBroker.newWriteOnlyTransaction();
            wrt.put(LogicalDatastoreType.CONFIGURATION, cid, this.config);
            wrt.submit();
        } catch (Exception err) {
            log("Failed to write TSDR Data Collection configuration  to data store.",
                    WARNING);
        }
    }

    public TSDRSDCConfig getConfigData() {
        return this.config;
    }

    public void shutdown() {
        this.running = false;
        synchronized(SNMPDataCollector.this.pollerSyncObject){
            SNMPDataCollector.this.pollerSyncObject.notifyAll();
        }
        synchronized(SNMPDataCollector.this){
            SNMPDataCollector.this.notifyAll();
        }
    }

    // This class is the storing thread, every 30 seconds it will wake up and
    // iterate over the builder container array and create
    // metric data list out of the container builders, wrap it up as input for
    // the RPC and invoke the storage RPC method.

    private class StoringThread extends Thread {
        public StoringThread() {
            this.setName("TSDR SNMP Storing Thread");
            this.start();
            log("SNMP Storing Thread Started", INFO);
        }

        public void run() {
            while (running) {
                synchronized (SNMPDataCollector.this) {
                    try {
                        /*
                         * We wait for 2x the polling interval just for the case
                         * where the polling thread is dead and there will be no
                         * thread to wake this thread up if we do "wait()", e.g.
                         * to avoid "stuck" thread. Disregarding the case where
                         * storing will take more than the polling interval, we
                         * have bigger issues in that case...:o)
                        */
                        SNMPDataCollector.this.wait(getConfigData()
                                .getPollingInterval() * 2);
                    } catch (InterruptedException err) {
                        log("SNMP Storing Thread Interrupted.", ERROR);
                    }
                }
                try {
                       // for (int i = 0; i < containers.length; i++) {
                    try {
                        InsertTSDRMetricRecordInputBuilder input = new InsertTSDRMetricRecordInputBuilder();
                        List<TSDRMetricRecord> list = new LinkedList<>();
                        TSDRMetricRecordBuilderContainer bc = container;
                        for (TSDRMetricRecordBuilder builder : bc.getBuilders()) {
                            list.add(builder.build());
                        }
                        input.setTSDRMetricRecord(list);
                        input.setCollectorCodeName(COLLECTOR_CODE_NAME);

                        store(input.build());
                        // store.storeTSDRMetricRecord(input.build());
                    } catch (Exception err) {
                        log("Fail to store data due to the following exception:",
                                ERROR);
                        log(err);
                    }

                } catch (Exception err) {
                    log("Fail to iterate over builder containers due to the following error:",
                            ERROR);
                    log(err);
                }
            }
        }
    }
    // Invoke the storage rpc method
    private void store(InsertTSDRMetricRecordInput input) {
        if(this.collectorSPIService==null){
            this.collectorSPIService = this.rpcRegistry
                    .getRpcService(TsdrCollectorSpiService.class);
        }
        this.collectorSPIService.insertTSDRMetricRecord(input);
        log("Data Storage Called from SNMP Collector", DEBUG);
    }

    public TsdrCollectorSpiService getTSDRService(){
        if(this.collectorSPIService==null){
            this.collectorSPIService = this.rpcRegistry
                    .getRpcService(TsdrCollectorSpiService.class);
        }
        return this.collectorSPIService;
    }

    // For debugging, enable the ability to output to a different file to avoid
    // looking for TSDR logs in the main log.
    public static PrintStream out = null;
    public static final int INFO = 1;
    public static final int DEBUG = 2;
    public static final int ERROR = 3;
    public static final int WARNING = 4;

    public static synchronized void log(Exception e) {
        if (logToExternalFile) {
            try {
                if (out == null) {
                    File f = new File("/tmp/tsdr.log");
                    out = new PrintStream(f);
                }
                e.printStackTrace(out);
                out.flush();
            } catch (Exception err) {
                err.printStackTrace();
            }
        } else {
            logger.error(e.getMessage(), e);
        }
    }

    public static synchronized void log(String str, int type) {
        if (logToExternalFile) {
            try {
                if (out == null) {
                    File f = new File("/tmp/tsdr.log");
                    out = new PrintStream(f);
                }
                out.println(str);
                out.flush();
            } catch (Exception err) {
                err.printStackTrace();
            }
        } else {
            switch (type) {
            case INFO:
                logger.info(str);
                break;
            case DEBUG:
                logger.debug(str);
                break;
            case ERROR:
                logger.error(str);
                break;
            case WARNING:
                logger.warn(str);
                break;
            default:
                logger.debug(str);
            }
        }
    }

    public DataBroker getDataBroker() {
        return this.dataBroker;
    }

    public boolean isRunning() {
        return this.running;
    }

    @Override
    public Future<RpcResult<Void>> setPollingInterval(
            SetPollingIntervalInput input) {
        TSDRSDCConfigBuilder builder = new TSDRSDCConfigBuilder();
        builder.setPollingInterval(input.getInterval());
        builder.setCommunity("mib2dev/if-mib");
        builder.setIpAddress(new Ipv4Address("127.0.0.1"));
        this.config = builder.build();
        saveConfigData();
        RpcResultBuilder<Void> rpc = RpcResultBuilder.success();
        return rpc.buildFuture();
    }
}
