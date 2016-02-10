/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2015 Tata Consultancy Services, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.sdc;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.snmp.plugin.internal.SNMPImpl;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.smiv2._if.mib.rev000614.interfaces.group.IfEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRMetricRecordInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRMetricRecordInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.snmp.data.collector.rev151013.SetPollingIntervalInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.snmp.data.collector.rev151013.SnmpMetric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.snmp.data.collector.rev151013.TSDRSnmpDataCollectorConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.snmp.data.collector.rev151013.TSDRSnmpDataCollectorConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.snmp.data.collector.rev151013.TsdrSnmpDataCollectorService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.GetInterfacesInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.GetInterfacesOutput;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * @author Sharon Aicler(saichler@gmail.com)
 * @author Trapti Khandelwal(trapti.khandelwal@tcs.com)
 * @author Razi Ahmed(ahmed.razi@tcs.com)
 **/
public class SNMPDataCollector implements TsdrSnmpDataCollectorService {
    private static final Logger logger = LoggerFactory.getLogger(SNMPDataCollector.class);
    private boolean running = true;
    // The reference to the the RPC registry to store the data
    private final DataBroker dataBroker;
    private final RpcProviderRegistry rpcRegistry;
    private TSDRSnmpDataCollectorConfig config = null;
    protected Object pollerSyncObject = new Object();
    private TsdrCollectorSpiService collectorSPIService = null;
    private static final String COLLECTOR_CODE_NAME = SNMPDataCollector.class.getSimpleName();
    private static final long pollingInterval=300000l;
    private List<TSDRMetricRecord> tsdrMetricRecordList = new LinkedList<>();

    public SNMPDataCollector(DataBroker dataBroker,RpcProviderRegistry rpcRegistry) {
        log("TSDR SNMP Collector Started", INFO);
        this.dataBroker = dataBroker;
        this.rpcRegistry = rpcRegistry;

        TSDRSnmpDataCollectorConfigBuilder b = new TSDRSnmpDataCollectorConfigBuilder();
        b.setPollingInterval(pollingInterval);
        this.config = b.build();
        saveConfigData();
        new TSDRSNMPInterfacePoller(this);
        new StoringThread();
    }

    public void loadConfigData() {
        // try to load the configuration data from the configuration data store
        ReadOnlyTransaction rot = null;
        try {
            InstanceIdentifier<TSDRSnmpDataCollectorConfig> cid = InstanceIdentifier
                    .create(TSDRSnmpDataCollectorConfig.class);
            rot = this.dataBroker.newReadOnlyTransaction();
            CheckedFuture<Optional<TSDRSnmpDataCollectorConfig>, ReadFailedException> read = rot
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

    public RpcResult<GetInterfacesOutput> loadGetInterfacesData(Ipv4Address ip, String community) {
        // fetch data from getInterfaces
        RpcResult<GetInterfacesOutput> result = null;
        SNMPImpl snmpImpl = new SNMPImpl(rpcRegistry);
        try
        {
            GetInterfacesInputBuilder input = new GetInterfacesInputBuilder();
            input.setCommunity(community);
            input.setIpAddress(ip);
            Future<RpcResult<GetInterfacesOutput>> resultFuture = snmpImpl.getInterfaces(input.build());
            result = resultFuture.get();
            result.isSuccessful();

            return result;
        }
        catch (Exception err) {
            log("Failed to get interfaces data from SNMP"+err.toString(), ERROR);
            return null;
        }
    }

    public void insertInterfacesEntries(Ipv4Address ip, RpcResult<GetInterfacesOutput> result){

        for(IfEntry entry : result.getResult().getIfEntry())
        {
            for(SnmpMetric snmpMetric:SnmpMetric.values()){
                TSDRMetricRecordBuilder b = new TSDRMetricRecordBuilder();

                b.setMetricName(snmpMetric.name());
                b.setTSDRDataCategory(DataCategory.SNMPINTERFACES);
                b.setNodeID(ip.getValue().toString());
                ArrayList<RecordKeys> list =new ArrayList<RecordKeys>(3);

                RecordKeysBuilder recordKeyB = new RecordKeysBuilder();
                recordKeyB.setKeyName("ifIndex");
                recordKeyB.setKeyValue(String.valueOf(entry.getIfIndex().getValue()));
                list.add(recordKeyB.build());

                recordKeyB.setKeyName("ifName");
                recordKeyB.setKeyValue(String.valueOf(entry.getIfType()));
                list.add(recordKeyB.build());

                recordKeyB.setKeyName("SnmpMetric");
                recordKeyB.setKeyValue(snmpMetric.name());
                list.add(recordKeyB.build());

                b.setRecordKeys(list);
                b.setTimeStamp(System.currentTimeMillis());

                switch(snmpMetric) {
                // do not need to be stored as they do not change often
                /*    case MTU:
                        b.setMetricValue(new BigDecimal(entry.getIfMtu()));
                        break;
                    case IfSpeed:
                        b.setMetricValue(new BigDecimal(entry.getIfSpeed().getValue()));
                        break;*/
                    case IfInNUcastPkts:
                        b.setMetricValue(new BigDecimal(entry.getIfInNUcastPkts().getValue()));
                        break;
                    case IfInDiscards:
                        b.setMetricValue(new BigDecimal(entry.getIfInDiscards().getValue()));
                        break;
                    case IfInErrors:
                        b.setMetricValue(new BigDecimal(entry.getIfInErrors().getValue()));
                        break;
                    case IfInOctets:
                        b.setMetricValue(new BigDecimal(entry.getIfInOctets().getValue()));
                        break;
                    case IfInUnknownProtos:
                        b.setMetricValue(new BigDecimal(entry.getIfInUnknownProtos().getValue()));
                        break;
                    case IfInUcastPkts:
                        b.setMetricValue(new BigDecimal(entry.getIfInUcastPkts().getValue()));
                        break;
                    case IfOutQLen:
                        b.setMetricValue(new BigDecimal(entry.getIfOutQLen().getValue()));
                        break;
                    case IfOutNUcastPkts:
                        b.setMetricValue(new BigDecimal(entry.getIfOutNUcastPkts().getValue()));
                        break;
                    case IfOutErrors:
                        b.setMetricValue(new BigDecimal(entry.getIfOutErrors().getValue()));
                        break;
                    case IfOutDiscards:
                        b.setMetricValue(new BigDecimal(entry.getIfOutDiscards().getValue()));
                        break;
                    case IfOutUcastPkts:
                        b.setMetricValue(new BigDecimal(entry.getIfOutUcastPkts().getValue()));
                        break;
                    case IfOutOctets:
                        b.setMetricValue(new BigDecimal(entry.getIfOutOctets().getValue()));
                        break;
                    case IfOperStatus:
                        b.setMetricValue(new BigDecimal(entry.getIfOperStatus().getIntValue()));
                        break;
                    case IfAdminStatus:
                        b.setMetricValue(new BigDecimal(entry.getIfAdminStatus().getIntValue()));
                        break;
                }
                synchronized(SNMPDataCollector.class) {
                    tsdrMetricRecordList.add(b.build());
                }
            }
        }
    }

    public void saveConfigData() {
        try {
            InstanceIdentifier<TSDRSnmpDataCollectorConfig> cid = InstanceIdentifier
                    .create(TSDRSnmpDataCollectorConfig.class);
            WriteTransaction wrt = this.dataBroker.newWriteOnlyTransaction();
            wrt.put(LogicalDatastoreType.CONFIGURATION, cid, this.config);
            wrt.submit();
        } catch (Exception err) {
            log("Failed to write TSDR Data Collection configuration  to data store.",
                    WARNING);
        }
    }

    public TSDRSnmpDataCollectorConfig getConfigData() {
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
            this.setDaemon(true);
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
                        SNMPDataCollector.this.wait(getConfigData().getPollingInterval() * 2);
                    } catch (InterruptedException err) {
                        log("SNMP Storing Thread Interrupted.", ERROR);
                    }
                }
                if(!running)
                {
                    break;
                }
                try {
                    try {
                        InsertTSDRMetricRecordInputBuilder input = new InsertTSDRMetricRecordInputBuilder();
                        synchronized (SNMPDataCollector.class) {
                            List<TSDRMetricRecord> list = tsdrMetricRecordList;
                            tsdrMetricRecordList = new LinkedList<>();
                            input.setTSDRMetricRecord(list);
                        }
                        input.setCollectorCodeName(COLLECTOR_CODE_NAME);
                        store(input.build());
                    } catch (Exception err) {
                        log("Fail to store data due to the following exception:", ERROR);
                        log(err);
                    }

                } catch (Exception err) {
                    log("Fail to iterate over builder containers due to the following error:", ERROR);
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
        logger.error(e.getMessage(), e);
    }

    public static synchronized void log(String str, int type) {
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

    public boolean isRunning() {
        return this.running;
    }

    @Override
    public Future<RpcResult<Void>> setPollingInterval(SetPollingIntervalInput input) {
        TSDRSnmpDataCollectorConfigBuilder builder = new TSDRSnmpDataCollectorConfigBuilder();
        builder.setPollingInterval(input.getInterval());
        this.config = builder.build();
        saveConfigData();
        RpcResultBuilder<Void> rpc = RpcResultBuilder.success();
        return rpc.buildFuture();
    }
}

