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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.snmp.plugin.internal.SNMPImpl;
import org.opendaylight.tsdr.collector.spi.RPCFutures;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
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
 * Collects SNMP data.
 *
 * @author Sharon Aicler(saichler@gmail.com)
 * @author Trapti Khandelwal(trapti.khandelwal@tcs.com)
 * @author Razi Ahmed(ahmed.razi@tcs.com)
 */
public class SNMPDataCollector implements TsdrSnmpDataCollectorService, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(SNMPDataCollector.class);
    private static final String COLLECTOR_CODE_NAME = SNMPDataCollector.class.getSimpleName();

    // The reference to the the RPC registry to store the data
    private final DataBroker dataBroker;
    private TSDRSnmpDataCollectorConfig config;
    private final TsdrCollectorSpiService collectorSPIService;
    private final SNMPConfig snmpConfig;
    @GuardedBy("tsdrMetricRecordList")
    private List<TSDRMetricRecord> tsdrMetricRecordList = new ArrayList<>();
    private SNMPInterfacePoller interfacePoller;
    private final long pollingInterval = 300000L;

    private volatile boolean running = true;

    public SNMPDataCollector(DataBroker dataBroker, TsdrCollectorSpiService collectorSPIService,
            SNMPConfig snmpConfig) {
        this.dataBroker = dataBroker;
        this.collectorSPIService = collectorSPIService;
        this.snmpConfig = snmpConfig;
    }

    public void init() {
        TSDRSnmpDataCollectorConfigBuilder builder = new TSDRSnmpDataCollectorConfigBuilder();
        builder.setPollingInterval(pollingInterval);
        this.config = builder.build();
        saveConfigData();

        interfacePoller = new SNMPInterfacePoller(this);
        interfacePoller.start();

        new StoringThread().start();

        LOG.info("TSDR SNMP Collector initialized");
    }

    SNMPConfig getSnmpConfig() {
        return snmpConfig;
    }

    public void loadConfigData() {
        // try to load the configuration data from the configuration data store
        final ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
        try {
            InstanceIdentifier<TSDRSnmpDataCollectorConfig> cid = InstanceIdentifier
                    .create(TSDRSnmpDataCollectorConfig.class);
            CheckedFuture<Optional<TSDRSnmpDataCollectorConfig>, ReadFailedException> read = readTx
                    .read(LogicalDatastoreType.CONFIGURATION, cid);
            if (read != null && read.get() != null) {
                if (read.get().isPresent()) {
                    this.config = read.get().get();
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to read TSDR Data Collection configuration from data store, using defaults", e);
        } finally {
            readTx.close();
        }
    }

    public RpcResult<GetInterfacesOutput> loadGetInterfacesData(Ipv4Address ip, String community) {
        // fetch data from getInterfaces
        try (SNMPImpl snmpImpl = new SNMPImpl()) {
            GetInterfacesInputBuilder input = new GetInterfacesInputBuilder();
            input.setCommunity(community);
            input.setIpAddress(ip);
            Future<RpcResult<GetInterfacesOutput>> resultFuture = snmpImpl.getInterfaces(input.build());
            RpcResult<GetInterfacesOutput> result = resultFuture.get();
            result.isSuccessful();

            return result;
        } catch (IOException | InterruptedException | ExecutionException e) {
            LOG.error("Failed to get interfaces data from SNMP", e);
            return null;
        }
    }

    public void insertInterfacesEntries(Ipv4Address ip, RpcResult<GetInterfacesOutput> result) {
        for (IfEntry entry : result.getResult().getIfEntry()) {
            for (SnmpMetric snmpMetric : SnmpMetric.values()) {
                TSDRMetricRecordBuilder builder = new TSDRMetricRecordBuilder();

                builder.setMetricName(snmpMetric.name());
                builder.setTSDRDataCategory(DataCategory.SNMPINTERFACES);
                builder.setNodeID(ip.getValue());
                ArrayList<RecordKeys> list = new ArrayList<>(3);

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

                builder.setRecordKeys(list);
                builder.setTimeStamp(System.currentTimeMillis());

                switch (snmpMetric) {
                // do not need to be stored as they do not change often
                /*    case MTU:
                        b.setMetricValue(new BigDecimal(entry.getIfMtu()));
                        break;
                    case IfSpeed:
                        b.setMetricValue(new BigDecimal(entry.getIfSpeed().getValue()));
                        break;*/
                    case IfInNUcastPkts:
                        builder.setMetricValue(new BigDecimal(entry.getIfInNUcastPkts().getValue()));
                        break;
                    case IfInDiscards:
                        builder.setMetricValue(new BigDecimal(entry.getIfInDiscards().getValue()));
                        break;
                    case IfInErrors:
                        builder.setMetricValue(new BigDecimal(entry.getIfInErrors().getValue()));
                        break;
                    case IfInOctets:
                        builder.setMetricValue(new BigDecimal(entry.getIfInOctets().getValue()));
                        break;
                    case IfInUnknownProtos:
                        builder.setMetricValue(new BigDecimal(entry.getIfInUnknownProtos().getValue()));
                        break;
                    case IfInUcastPkts:
                        builder.setMetricValue(new BigDecimal(entry.getIfInUcastPkts().getValue()));
                        break;
                    case IfOutQLen:
                        builder.setMetricValue(new BigDecimal(entry.getIfOutQLen().getValue()));
                        break;
                    case IfOutNUcastPkts:
                        builder.setMetricValue(new BigDecimal(entry.getIfOutNUcastPkts().getValue()));
                        break;
                    case IfOutErrors:
                        builder.setMetricValue(new BigDecimal(entry.getIfOutErrors().getValue()));
                        break;
                    case IfOutDiscards:
                        builder.setMetricValue(new BigDecimal(entry.getIfOutDiscards().getValue()));
                        break;
                    case IfOutUcastPkts:
                        builder.setMetricValue(new BigDecimal(entry.getIfOutUcastPkts().getValue()));
                        break;
                    case IfOutOctets:
                        builder.setMetricValue(new BigDecimal(entry.getIfOutOctets().getValue()));
                        break;
                    case IfOperStatus:
                        builder.setMetricValue(new BigDecimal(entry.getIfOperStatus().getIntValue()));
                        break;
                    case IfAdminStatus:
                        builder.setMetricValue(new BigDecimal(entry.getIfAdminStatus().getIntValue()));
                        break;
                    default:
                        break;
                }

                synchronized (tsdrMetricRecordList) {
                    tsdrMetricRecordList.add(builder.build());
                }
            }
        }
    }

    public void saveConfigData() {
        InstanceIdentifier<TSDRSnmpDataCollectorConfig> cid =
                InstanceIdentifier.create(TSDRSnmpDataCollectorConfig.class);
        WriteTransaction wrt = this.dataBroker.newWriteOnlyTransaction();
        wrt.put(LogicalDatastoreType.CONFIGURATION, cid, this.config);
        wrt.submit();
    }

    public TSDRSnmpDataCollectorConfig getConfigData() {
        return this.config;
    }

    @Override
    @SuppressFBWarnings("NN_NAKED_NOTIFY")
    public void close() {
        this.running = false;

        if (interfacePoller != null) {
            interfacePoller.close();
        }

        synchronized (SNMPDataCollector.this) {
            SNMPDataCollector.this.notifyAll();
        }
    }

    // This class is the storing thread, every 30 seconds it will wake up and
    // iterate over the builder container array and create
    // metric data list out of the container builders, wrap it up as input for
    // the RPC and invoke the storage RPC method.

    private class StoringThread extends Thread {
        StoringThread() {
            this.setName("TSDR SNMP Storing Thread");
            this.setDaemon(true);
            LOG.info("SNMP Storing Thread Started");
        }

        @Override
        @SuppressFBWarnings("UW_UNCOND_WAIT")
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
                    } catch (InterruptedException e) {
                        LOG.error("SNMP Storing Thread Interrupted.", e);
                    }
                }

                if (!running) {
                    break;
                }

                InsertTSDRMetricRecordInputBuilder input = new InsertTSDRMetricRecordInputBuilder();
                synchronized (tsdrMetricRecordList) {
                    List<TSDRMetricRecord> list = tsdrMetricRecordList;
                    tsdrMetricRecordList = new ArrayList<>();
                    input.setTSDRMetricRecord(list);
                }
                input.setCollectorCodeName(COLLECTOR_CODE_NAME);
                store(input.build());
            }
        }
    }

    // Invoke the storage rpc method
    private void store(InsertTSDRMetricRecordInput input) {
        RPCFutures.logResult(collectorSPIService.insertTSDRMetricRecord(input), "insertTSDRMetricRecord", LOG);
        LOG.debug("Data Storage Called from SNMP Collector");
    }

    public TsdrCollectorSpiService getTSDRService() {
        return this.collectorSPIService;
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

