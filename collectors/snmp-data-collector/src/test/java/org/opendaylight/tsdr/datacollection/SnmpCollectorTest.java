/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2015 Tata Consultancy Services, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.datacollection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

import com.google.common.util.concurrent.Uninterruptibles;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.test.ConstantSchemaAbstractDataBrokerTest;
import org.opendaylight.tsdr.sdc.SNMPConfig;
import org.opendaylight.tsdr.sdc.SNMPDataCollector;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Counter32;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Gauge32;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.smiv2._if.mib.rev000614.InterfaceIndex;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.smiv2._if.mib.rev000614.interfaces.group.IfEntry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.smiv2._if.mib.rev000614.interfaces.group.IfEntry.IfAdminStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.smiv2._if.mib.rev000614.interfaces.group.IfEntry.IfOperStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.smiv2._if.mib.rev000614.interfaces.group.IfEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.smiv2.ianaiftype.mib.rev100211.IANAifType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.smiv2.snmpv2.tc.rev990401.DisplayString;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRMetricRecordInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.snmp.data.collector.rev151013.SnmpMetric;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.snmp.data.collector.rev151013.TSDRSnmpDataCollectorConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.GetInterfacesInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.GetInterfacesOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.GetInterfacesOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.SnmpService;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.osgi.service.cm.ConfigurationException;

/**
 * Unit tests for the SNMP collector.
 *
 * @author Trapti Khandelwal(trapti.khandelwal@tcs.com)
 * @author Razi Ahmed(ahmed.razi@tcs.com)
 */
public class SnmpCollectorTest extends ConstantSchemaAbstractDataBrokerTest {
    private final TsdrCollectorSpiService collectorSPIService = mock(TsdrCollectorSpiService.class);
    private final SnmpService snmpService = mock(SnmpService.class);
    private SNMPDataCollector collector;
    private final SNMPConfig snmpConfig = new SNMPConfig();
    private final Map<String, PropertyDescriptor> ifEntryPropDescriptors = new HashMap<>();
    private final Map<Class<?>, Method> getValueMethods = new HashMap<>();

    @Before
    public void setUp() throws ConfigurationException, IntrospectionException {
        Hashtable<String, String> properties = new Hashtable<>();
        properties.put("credentials", "[1.2.3.4,public],[5.6.7.8,public]");
        snmpConfig.updated(properties);

        Set<Class<?>> propTypes = new HashSet<>();
        BeanInfo beanInfo = Introspector.getBeanInfo(IfEntry.class);
        for (PropertyDescriptor desc: beanInfo.getPropertyDescriptors()) {
            ifEntryPropDescriptors.put(desc.getName(), desc);
            propTypes.add(desc.getPropertyType());
        }

        for (Class<?> type: propTypes) {
            try {
                Method method = type.getMethod("getValue");
                if (Number.class.isAssignableFrom(method.getReturnType())) {
                    getValueMethods.put(type, method);
                }
            } catch (NoSuchMethodException e) {
                // don't care
            }
        }

        doReturn(RpcResultBuilder.<Void>success().buildFuture()).when(collectorSPIService)
                .insertTSDRMetricRecord(any());

        collector = new SNMPDataCollector(snmpService, collectorSPIService, snmpConfig,
                new TSDRSnmpDataCollectorConfigBuilder().setPollingInterval(500L)
                        .setStoreMetricsExecutorQueueSize(1000L).build());
    }

    @After
    public void tearDown() {
        collector.close();
    }

    @Test
    public void testPolling() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Map<Ipv4Address, GetInterfacesOutput> interfaceData = new HashMap<>();
        interfaceData.put(new Ipv4Address("1.2.3.4"), new GetInterfacesOutputBuilder()
                .setIfEntry(Arrays.asList(ifEntry(10, IANAifType.Bridge, IfAdminStatus.Up, IfOperStatus.Up),
                        ifEntry(20, IANAifType.EthernetCsmacd, IfAdminStatus.Down, IfOperStatus.Dormant))).build());

        interfaceData.put(new Ipv4Address("5.6.7.8"), new GetInterfacesOutputBuilder()
                .setIfEntry(Arrays.asList(ifEntry(30, IANAifType.FastEther, IfAdminStatus.Testing,
                        IfOperStatus.LowerLayerDown))).build());

        doAnswer(invocation -> {
            GetInterfacesInput input = invocation.getArgumentAt(0, GetInterfacesInput.class);
            return RpcResultBuilder.success(interfaceData.get(input.getIpAddress())).buildFuture();
        }).when(snmpService).getInterfaces(any());

        Collection<TSDRMetricRecord> metricRecords = Collections.synchronizedList(new ArrayList<>());
        AtomicReference<CountDownLatch> storeMetricsLatchRef = new AtomicReference<>(new CountDownLatch(2));
        AtomicBoolean storeMetricsContinue = new AtomicBoolean();
        doAnswer(invocation -> {
            metricRecords.addAll(invocation.getArgumentAt(0, InsertTSDRMetricRecordInput.class).getTSDRMetricRecord());
            CountDownLatch storeMetricsLatch = storeMetricsLatchRef.get();
            storeMetricsLatch.countDown();
            if (storeMetricsLatch.getCount() == 0) {
                synchronized (storeMetricsContinue) {
                    while (!storeMetricsContinue.get()) {
                        storeMetricsContinue.wait();
                    }

                    storeMetricsContinue.set(false);
                }
            }
            return RpcResultBuilder.<Void>success().buildFuture();
        }).when(collectorSPIService).insertTSDRMetricRecord(any());

        collector.init();

        // Wait for first poll

        assertTrue("Timed out waiting for metrics to be stored",
                Uninterruptibles.awaitUninterruptibly(storeMetricsLatchRef.get(), 5, TimeUnit.SECONDS));
        storeMetricsLatchRef.set(new CountDownLatch(2));

        verifyStats(metricRecords, interfaceData.entrySet());
        metricRecords.clear();

        synchronized (storeMetricsContinue) {
            storeMetricsContinue.set(true);
            storeMetricsContinue.notifyAll();
        }

        // Wait for second poll

        assertTrue("Timed out waiting for metrics to be stored",
                Uninterruptibles.awaitUninterruptibly(storeMetricsLatchRef.get(), 5, TimeUnit.SECONDS));
        storeMetricsLatchRef.set(new CountDownLatch(1));

        verifyStats(metricRecords, interfaceData.entrySet());
        metricRecords.clear();

        reset(collectorSPIService);

        collector.close();

        synchronized (storeMetricsContinue) {
            storeMetricsContinue.set(true);
            storeMetricsContinue.notifyAll();
        }

        Mockito.verify(collectorSPIService, after(1000).never()).insertTSDRMetricRecord(any());
    }

    private void verifyStats(Collection<TSDRMetricRecord> metricRecords,
            Collection<Entry<Ipv4Address, GetInterfacesOutput>> expData)
                    throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Map<String, TSDRMetricRecord> map = new HashMap<>();
        for (TSDRMetricRecord rec: metricRecords) {
            StringBuilder builder = new StringBuilder(rec.getNodeID());
            for (RecordKeys key: rec.getRecordKeys()) {
                builder.append('.').append(key.getKeyName()).append(':').append(key.getKeyValue());
            }

            map.put(builder.toString(), rec);
        }

        for (Entry<Ipv4Address, GetInterfacesOutput> entry: expData) {
            Ipv4Address ip = entry.getKey();
            GetInterfacesOutput data = entry.getValue();
            for (IfEntry ifEntry: data.getIfEntry()) {
                String keyPrefix = ip.getValue() + ".ifIndex:" + ifEntry.getIfIndex().getValue() + ".ifName:"
                        + ifEntry.getIfType() + ".SnmpMetric:";

                for (SnmpMetric snmpMetric : SnmpMetric.values()) {
                    String key = keyPrefix + snmpMetric.name();
                    TSDRMetricRecord record = map.get(key);
                    assertNotNull("TSDRMetricRecord not found for " + key, record);
                    assertEquals("Metric DataCategory for " + key, DataCategory.SNMPINTERFACES,
                            record.getTSDRDataCategory());

                    PropertyDescriptor desc = ifEntryPropDescriptors.get(snmpMetric.getName());
                    assertNotNull("PropertyDescriptor not found for " + snmpMetric.getName(), desc);

                    Object valueObj = desc.getReadMethod().invoke(ifEntry);
                    assertNotNull("value is null for " + snmpMetric.getName(), desc);
                    BigDecimal expValue;
                    if (valueObj instanceof IfAdminStatus) {
                        expValue = BigDecimal.valueOf(((IfAdminStatus)valueObj).getIntValue());
                    } else if (valueObj instanceof IfOperStatus) {
                        expValue = BigDecimal.valueOf(((IfOperStatus)valueObj).getIntValue());
                    } else {
                        Method method = getValueMethods.get(valueObj.getClass());
                        if (method != null) {
                            expValue = BigDecimal.valueOf(((Number)method.invoke(valueObj)).longValue());
                        } else {
                            try {
                                expValue = new BigDecimal(valueObj.toString());
                            } catch (NumberFormatException e) {
                                throw new AssertionError("Error parsing BigDecimal from value " + valueObj + " for "
                                        + snmpMetric.getName(), e);
                            }
                        }
                    }

                    assertEquals("Metric value for " + key, expValue, record.getMetricValue());
                }
            }
        }
    }

    private static IfEntry ifEntry(int ifIndex, IANAifType type, IfAdminStatus adminStatus, IfOperStatus operStatus) {
        return new IfEntryBuilder()
            .setIfAdminStatus(adminStatus)
            .setIfDescr(new DisplayString("ISO-" + ifIndex))
            .setIfIndex(new InterfaceIndex(ifIndex))
            .setIfInDiscards(new Counter32(1500L + ifIndex))
            .setIfInErrors(new Counter32(1L + ifIndex))
            .setIfInNUcastPkts(new Counter32(2L + ifIndex))
            .setIfInOctets(new Counter32(3L + ifIndex))
            .setIfInUcastPkts(new Counter32(4L + ifIndex))
            .setIfMtu(5 + ifIndex)
            .setIfOperStatus(operStatus)
            .setIfOutDiscards(new Counter32(6L + ifIndex))
            .setIfOutNUcastPkts(new Counter32(7L + ifIndex))
            .setIfOutOctets(new Counter32(8L + ifIndex))
            .setIfOutQLen(new Gauge32(9L + ifIndex))
            .setIfSpeed(new Gauge32(10L + ifIndex))
            .setIfType(type)
            .setIfInUnknownProtos(new Counter32(11L + ifIndex))
            .setIfOutErrors(new Counter32(12L + ifIndex))
            .setIfOutUcastPkts(new Counter32(13L + ifIndex))
            .build();
    }
}
