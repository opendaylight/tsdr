/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2015 Tata Consultancy Services, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.datacollection;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.tsdr.sdc.SNMPConfig;
import org.opendaylight.tsdr.sdc.SNMPDataCollector;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Counter32;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Gauge32;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.smiv2._if.mib.rev000614.InterfaceIndex;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.smiv2._if.mib.rev000614.interfaces.group.IfEntry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.smiv2._if.mib.rev000614.interfaces.group.IfEntry.IfAdminStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.smiv2._if.mib.rev000614.interfaces.group.IfEntry.IfOperStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.smiv2._if.mib.rev000614.interfaces.group.IfEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.smiv2.snmpv2.tc.rev990401.DisplayString;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.snmp.data.collector.rev151013.SetPollingIntervalInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.snmp.data.collector.rev151013.TSDRSnmpDataCollectorConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.snmp.data.collector.rev151013.TSDRSnmpDataCollectorConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.GetInterfacesOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.GetInterfacesOutputBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

/**
 * Unit tests for the SNMP collector.
 *
 * @author Trapti Khandelwal(trapti.khandelwal@tcs.com)
 * @author Razi Ahmed(ahmed.razi@tcs.com)
 */
public class SnmpCollectorTest {

    private static final String COMMUNITY = "mib2dev/if-mib";

    private ReadOnlyTransaction readTransaction;
    private WriteTransaction writeTransaction;
    private TSDRSnmpDataCollectorConfig collectorConfig = null;
    private final CheckedFuture<Optional<TSDRSnmpDataCollectorConfig>, ReadFailedException> checkedFuture =
            mock(CheckedFuture.class);
    private DataBroker dataBroker;
    private final InstanceIdentifier<TSDRSnmpDataCollectorConfig> id =
            InstanceIdentifier.create(TSDRSnmpDataCollectorConfig.class);
    private SNMPDataCollector collector;
    private final SNMPConfig tsdrSnmpConfigObj = new SNMPConfig();
    private final Ipv4Address ip = new Ipv4Address("127.0.0.1");
    private final Optional<TSDRSnmpDataCollectorConfig> optional = mock(Optional.class);

    @Before
    public void setUp() throws IOException, InterruptedException, ExecutionException {
        dataBroker = mock(DataBroker.class);
        readTransaction = mock(ReadOnlyTransaction.class);
        writeTransaction = mock(ReadWriteTransaction.class);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readTransaction);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        collectorConfig = buildNodes();
        writeTransaction.put(LogicalDatastoreType.CONFIGURATION, id, collectorConfig);
        readTransaction.read(LogicalDatastoreType.CONFIGURATION, id);
        when(readTransaction.read(LogicalDatastoreType.CONFIGURATION, id)).thenReturn(checkedFuture);
        when(checkedFuture.get()).thenReturn(optional);

        when(optional.get()).thenReturn(collectorConfig);
        collector = new SNMPDataCollector(this.dataBroker, mock(TsdrCollectorSpiService.class), tsdrSnmpConfigObj);
    }

    private TSDRSnmpDataCollectorConfig buildNodes() {
        TSDRSnmpDataCollectorConfigBuilder nb = new TSDRSnmpDataCollectorConfigBuilder();
        nb.setPollingInterval(15000L);
        return nb.build();
    }

    @Test
    public void testloadInterfacesData() throws Exception {
        RpcResult<GetInterfacesOutput> resultGetInterfacesOutput = collector.loadGetInterfacesData(ip, COMMUNITY);
        assertTrue(resultGetInterfacesOutput.isSuccessful());
    }

    @Test
    public void testinsertInterfacesData() throws Exception {
        final GetInterfacesOutputBuilder getInterfacesOutputBuilder = new GetInterfacesOutputBuilder();
        IfEntryBuilder ife = new IfEntryBuilder();
        ife.setIfAdminStatus(IfAdminStatus.Down);
        ife.setIfDescr(new DisplayString("ISO"));
        ife.setIfIndex(new InterfaceIndex(12345678));
        ife.setIfInDiscards(new Counter32(1500L));
        ife.setIfInErrors(new Counter32(2L));
        ife.setIfInNUcastPkts(new Counter32(2L));
        ife.setIfInOctets(new Counter32(2L));
        ife.setIfInUcastPkts(new Counter32(2L));
        ife.setIfMtu(111);
        ife.setIfOperStatus(IfOperStatus.Down);
        ife.setIfOutDiscards(new Counter32(2L));
        ife.setIfOutNUcastPkts(new Counter32(2L));
        ife.setIfOutOctets(new Counter32(2L));
        ife.setIfOutQLen(new Gauge32((long) 2));
        ife.setIfSpeed(new Gauge32((long) 2));
        ife.setIfInUnknownProtos(new Counter32(2L));
        ife.setIfOutErrors(new Counter32(2L));
        ife.setIfOutUcastPkts(new Counter32(2L));
        List<IfEntry> ifEntries = new ArrayList<>(1);
        IfEntryBuilder ifEntryBuilder = ife;
        ifEntries.add(ifEntryBuilder.build());
        getInterfacesOutputBuilder.setIfEntry(ifEntries).setIfNumber(1);
        RpcResultBuilder<GetInterfacesOutput> rpcResultBuilder =
                RpcResultBuilder.success(getInterfacesOutputBuilder.build());
        collector.insertInterfacesEntries(ip, rpcResultBuilder.build());
    }

    @Test
    public void testClose() throws Exception {
        collector.close();
    }

    @Test
    public void testupdatedDictionary() throws Exception {
        Dictionary<String, String> dict = new Hashtable<>();
        dict.put("credentials", "[172.21.182.143,mib2dev/if-mib]");
        tsdrSnmpConfigObj.updated(dict);
    }

    @Test
    public void testsetPollingInterval() throws Exception {
        SetPollingIntervalInputBuilder objSetPollingIntervalInputBuilder = new SetPollingIntervalInputBuilder();
        objSetPollingIntervalInputBuilder.setInterval(15000L);
        collector.setPollingInterval(objSetPollingIntervalInputBuilder.build());
    }

    @Test
    public void testgetConfigData() throws Exception {
        collector.getConfigData();
    }

    @Test
    public void testgetConfig() throws Exception {
        tsdrSnmpConfigObj.getConfig("credentials");
    }
}
