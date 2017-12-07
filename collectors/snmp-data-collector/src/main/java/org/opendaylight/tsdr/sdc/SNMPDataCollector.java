/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2015 Tata Consultancy Services, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.sdc;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.opendaylight.tsdr.collector.spi.RPCFutures;
import org.opendaylight.tsdr.spi.util.DataEncrypter;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.snmp.data.collector.rev151013.SnmpMetric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.snmp.data.collector.rev151013.TSDRSnmpDataCollectorConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.GetInterfacesInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.GetInterfacesOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.SnmpService;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collects SNMP data.
 *
 * @author Thomas Pantelis
 * @author Sharon Aicler(saichler@gmail.com)
 * @author Trapti Khandelwal(trapti.khandelwal@tcs.com)
 * @author Razi Ahmed(ahmed.razi@tcs.com)
 */
public class SNMPDataCollector implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(SNMPDataCollector.class);
    private static final String COLLECTOR_CODE_NAME = SNMPDataCollector.class.getSimpleName();

    private final SnmpService snmpService;
    private final TSDRSnmpDataCollectorConfig collectorConfig;
    private final TsdrCollectorSpiService collectorSPIService;
    private final SNMPConfig snmpConfig;

    private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(
            1, new ThreadFactoryBuilder().setNameFormat("TSDR SNMP Poller-%d").build());

    private final ExecutorService storeMetricsExecutor;

    public SNMPDataCollector(SnmpService snmpService, TsdrCollectorSpiService collectorSPIService,
            SNMPConfig snmpConfig, TSDRSnmpDataCollectorConfig collectorConfig) {
        this.snmpService = snmpService;
        this.collectorSPIService = collectorSPIService;
        this.snmpConfig = snmpConfig;
        this.collectorConfig = collectorConfig;

        storeMetricsExecutor = SpecialExecutors.newBoundedSingleThreadExecutor(
                collectorConfig.getStoreMetricsExecutorQueueSize().intValue(), "TSDR SNMP Storing Thread");
    }

    public void init() {
        long pollingInterval = collectorConfig.getPollingInterval();
        scheduledExecutor.scheduleWithFixedDelay(this::poll, pollingInterval, pollingInterval, TimeUnit.MILLISECONDS);

        LOG.info("TSDR SNMP Collector initialized");
    }

    @Override
    public void close() {
        scheduledExecutor.shutdownNow();
        storeMetricsExecutor.shutdown();
    }

    private void poll() {
        Dictionary<String, String[]> configuration = snmpConfig.getConfiguration();
        final String[] credentials = configuration.get(SNMPConfig.P_CREDENTIALS);
        for (int nodeConfigDetails = 0; nodeConfigDetails < credentials.length; nodeConfigDetails += 2) {
            Ipv4Address ip = new Ipv4Address(credentials[nodeConfigDetails]);
            String community = DataEncrypter.decrypt(credentials[nodeConfigDetails + 1]);

            Futures.addCallback(JdkFutureAdapters.listenInPoolThread(getInterfaces(ip, community)),
                new FutureCallback<RpcResult<GetInterfacesOutput>>() {
                    @Override
                    public void onSuccess(@Nonnull RpcResult<GetInterfacesOutput> result) {
                        LOG.debug("getInterfaces RPC for IP {} returned result - isSuccessful: {}, errors: {}",
                                ip.getValue(), result.isSuccessful(), result.getErrors());

                        GetInterfacesOutput interfacesOutput = result.getResult();
                        if (interfacesOutput != null) {
                            storeInterfaces(ip, interfacesOutput);
                        } else {
                            LOG.warn("getInterfaces RPC for IP {} returned null GetInterfacesOutput: {}",
                                    ip.getValue(), result);
                        }
                    }

                    @Override
                    public void onFailure(Throwable ex) {
                        LOG.error("getInterfaces RPC or IP {} failed", ip.getValue(), ex);
                    }
                }, MoreExecutors.directExecutor());
        }
    }

    public Future<RpcResult<GetInterfacesOutput>> getInterfaces(Ipv4Address ip, String community) {
        return snmpService.getInterfaces(new GetInterfacesInputBuilder().setCommunity(community)
                .setIpAddress(ip).build());
    }

    private void storeInterfaces(Ipv4Address ip, GetInterfacesOutput interfacesOutput) {
        try {
            storeMetricsExecutor.execute(() -> doStoreInterfaces(ip, interfacesOutput));
        } catch (RejectedExecutionException e) {
            if (!storeMetricsExecutor.isShutdown()) {
                LOG.error("Could not enqueue task to store interfaces for {}", ip, e);
            }
        }
    }

    private void doStoreInterfaces(Ipv4Address ip, GetInterfacesOutput interfacesOutput) {
        List<TSDRMetricRecord> tsdrMetricRecordList = new ArrayList<>();
        for (IfEntry entry : interfacesOutput.getIfEntry()) {
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

                tsdrMetricRecordList.add(builder.build());
            }
        }

        LOG.debug("Storing {} metric records for IP {}", tsdrMetricRecordList.size(), ip.getValue());

        store(new InsertTSDRMetricRecordInputBuilder().setTSDRMetricRecord(tsdrMetricRecordList)
                .setCollectorCodeName(COLLECTOR_CODE_NAME).build());
    }

    // Invoke the storage rpc method
    private void store(InsertTSDRMetricRecordInput input) {
        RPCFutures.logResult(collectorSPIService.insertTSDRMetricRecord(input), "insertTSDRMetricRecord", LOG);
    }
}

