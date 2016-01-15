/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2015 Tata Consultancy Services, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.datacollection;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Counter32;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Gauge32;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.smiv2._if.mib.rev000614.InterfaceIndex;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.smiv2._if.mib.rev000614.interfaces.group.IfEntry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.smiv2._if.mib.rev000614.interfaces.group.IfEntry.IfAdminStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.smiv2._if.mib.rev000614.interfaces.group.IfEntry.IfOperStatus;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.smiv2._if.mib.rev000614.interfaces.group.IfEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.*;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.smiv2.snmpv2.tc.rev990401.DisplayString;
import org.opendaylight.snmp.plugin.internal.MibTable;
import org.opendaylight.snmp.plugin.internal.SNMPImpl;
import org.opendaylight.tsdr.sdc.SNMPDataCollector;
import org.opendaylight.tsdr.sdc.TSDRSNMPConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.GetInterfacesOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRMetricRecordInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.snmp.data.collector.rev151013.SetPollingIntervalInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.snmp.data.collector.rev151013.TSDRSnmpDataCollectorConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.snmp.data.collector.rev151013.TSDRSnmpDataCollectorConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.snmp.data.collector.rev151013.TsdrSnmpDataCollectorService;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

/**
 * @author Trapti Khandelwal(trapti.khandelwal@tcs.com)
 * @author Razi Ahmed(ahmed.razi@tcs.com)
 */
public class SnmpCollectorTest {

    private static final String GET_IP_ADDRESS = "127.0.0.1";
    private static final Integer snmpListenPort = 161;
    private static final String COMMUNITY = "mib2dev/if-mib";
    private static Snmp mockSnmp = null;
    private static RpcProviderRegistry mockRpcReg = null;
    private ReadOnlyTransaction readTransaction = null;
    private WriteTransaction writeTransaction =null;
    private TSDRSnmpDataCollectorConfig collectorConfig = null;
    private CheckedFuture<Optional<TSDRSnmpDataCollectorConfig>, ReadFailedException> checkedFuture = mock(CheckedFuture.class);
    private static Future<RpcResult<SnmpGetOutput>> futureSnmpGetOutput = null;
    private DataBroker dataBroker = null;
    private RpcProviderRegistry rpcRegistry = null;
    private InstanceIdentifier<TSDRSnmpDataCollectorConfig> id= null;
    private SNMPDataCollector collector = null;
    private TSDRSNMPConfig tsdrSnmpConfigObj = null;
    private Ipv4Address ip=new Ipv4Address("127.0.0.1");
    private Optional<TSDRSnmpDataCollectorConfig> optional = mock(Optional.class);
    private List<TSDRMetricRecord> tsdrMetricRecordList = new LinkedList<>();

    @Before
    public void setUp() throws IOException {
        tsdrSnmpConfigObj=TSDRSNMPConfig.getInstance();  
        rpcRegistry = mock(RpcProviderRegistry.class);
        when(rpcRegistry.addRpcImplementation(eq(TsdrSnmpDataCollectorService.class), any(TsdrSnmpDataCollectorService.class))).thenReturn(null);
        dataBroker = mock(DataBroker.class);
        readTransaction = mock(ReadOnlyTransaction.class);
        writeTransaction = mock (ReadWriteTransaction.class);
        id = InstanceIdentifier.create(TSDRSnmpDataCollectorConfig.class);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readTransaction);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        collectorConfig = buildNodes();
        writeTransaction.put(LogicalDatastoreType.CONFIGURATION, id, collectorConfig);
        readTransaction.read(LogicalDatastoreType.CONFIGURATION, id);
        when(readTransaction.read(LogicalDatastoreType.CONFIGURATION, id)).thenReturn(checkedFuture);
        try {
            when(checkedFuture.get()).thenReturn(optional);
        } catch (InterruptedException | ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        when(optional.get()).thenReturn(collectorConfig);
        mockSnmp = mock(Snmp.class);
        collector = new SNMPDataCollector(this.dataBroker, rpcRegistry);
    }

    private TSDRSnmpDataCollectorConfig buildNodes(){
        TSDRSnmpDataCollectorConfigBuilder nb = new TSDRSnmpDataCollectorConfigBuilder();
        nb.setPollingInterval(15000l);
        return nb.build();
    }

    @Test
    public void testloadInterfacesData() throws Exception {
        RpcResult<GetInterfacesOutput> resultGetInterfacesOutput = collector.loadGetInterfacesData(ip, COMMUNITY);
        assertTrue(resultGetInterfacesOutput.isSuccessful());
    }

    @Test
    public void testinsertInterfacesData() throws Exception {
        GetInterfacesOutputBuilder getInterfacesOutputBuilder = new GetInterfacesOutputBuilder();
        IfEntryBuilder ife = new IfEntryBuilder();
        ife.setIfAdminStatus(IfAdminStatus.Down);
        ife.setIfDescr(new DisplayString("ISO"));
        ife.setIfIndex(new InterfaceIndex(12345678));
        ife.setIfInDiscards(new Counter32(1500l));
        ife.setIfInErrors(new Counter32(2l));
        ife.setIfInNUcastPkts(new Counter32(2l));
        ife.setIfInOctets(new Counter32(2l));
        ife.setIfInUcastPkts(new Counter32(2l));
        ife.setIfMtu(111);
        ife.setIfOperStatus(IfOperStatus.Down);
        ife.setIfOutDiscards(new Counter32(2l));
        ife.setIfOutNUcastPkts(new Counter32(2l));
        ife.setIfOutOctets(new Counter32(2l));
        ife.setIfOutQLen(new Gauge32((long) 2));
        ife.setIfSpeed(new Gauge32((long) 2));
        ife.setIfInUnknownProtos(new Counter32(2l));
        ife.setIfOutErrors(new Counter32(2l));
        ife.setIfOutUcastPkts(new Counter32(2l));
        List<IfEntry> ifEntries = new ArrayList<>(1);
        IfEntryBuilder ifEntryBuilder = ife;
        ifEntries.add(ifEntryBuilder.build());
        getInterfacesOutputBuilder.setIfEntry(ifEntries)
        .setIfNumber(1);
        RpcResultBuilder<GetInterfacesOutput> rpcResultBuilder = RpcResultBuilder.success(getInterfacesOutputBuilder.build());
        collector.insertInterfacesEntries(ip, rpcResultBuilder.build());
    }

    @Test
    public void testTSDRService() throws Exception {
        TsdrCollectorSpiService collectorSPIService=  collector.getTSDRService();
    }

    @Test
    public void testShutdown() throws Exception {
        collector.shutdown();
    }

    @Test
    public void testLog() throws Exception {

        SNMPDataCollector.log(new Exception());
        SNMPDataCollector.log("ERROR",1 );
        SNMPDataCollector.log("ERROR",2 );
        SNMPDataCollector.log("ERROR",3 );
        SNMPDataCollector.log("ERROR",4 );
        SNMPDataCollector.log("ERROR",5 );
    }

    @Test
    public void testupdatedDictionary() throws Exception {
        Dictionary<String, String> dict = new Hashtable<>();
        dict.put("credentials", "[172.21.182.143,mib2dev/if-mib]");
        tsdrSnmpConfigObj.updated(dict);
    }

    @Test
    public void testsetPollingInterval() throws Exception {
        SetPollingIntervalInputBuilder objSetPollingIntervalInputBuilder= new SetPollingIntervalInputBuilder();
        objSetPollingIntervalInputBuilder.setInterval(15000l);
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
