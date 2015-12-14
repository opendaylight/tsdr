package org.opendaylight.tsdr.datacollection;

import com.google.common.util.concurrent.SettableFuture;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.OngoingStubbing;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Counter32;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.smiv2._if.mib.rev000614.InterfaceIndex;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.smiv2._if.mib.rev000614.interfaces.group.IfEntry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.smiv2._if.mib.rev000614.interfaces.group.IfEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.GetInterfacesInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.GetInterfacesOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.SnmpGetInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.SnmpGetOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.SnmpGetType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.SnmpService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.SnmpSetInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.snmp.get.output.Results;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.snmp.get.output.ResultsBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.smiv2.snmpv2.tc.rev990401.DisplayString;
import org.opendaylight.snmp.plugin.internal.SNMPImpl;

public class SnmpCollectorTest {

    private static final String GET_IP_ADDRESS = "127.0.0.1";
    private static final Integer snmpListenPort = 161;
    private static final String COMMUNITY = "mib2dev/if-mib";
    private static Snmp mockSnmp = null;
    private static RpcProviderRegistry mockRpcReg = null;
    private static SNMPImpl snmpImpl = null;
    //private static AsyncGetHandler getHandler = null;
    private static Future<RpcResult<SnmpGetOutput>> futureSnmpGetOutput = null;

    @Before
    public void setUp() throws IOException {
        mockRpcReg = mock(RpcProviderRegistry.class);
        when(mockRpcReg.addRpcImplementation(eq(SnmpService.class), any(SnmpService.class))).thenReturn(null);

        // GET response
        mockSnmp = mock(Snmp.class);
    }

    @Test
    public void testGetInterfacesDataCollection() throws Exception {
        final String baseIFOIB = "1.3.6.1.2.1.2.2.1.";
        final OID ifIndexOID = new OID(baseIFOIB + "1");
        final OID ifAdminStatusOID = new OID(baseIFOIB + "7");
        final OID ifDescrOID = new OID(baseIFOIB + "2");
        final OID ifInErrorsOID = new OID(baseIFOIB + "14");

        // Generate the test list of interfaces
        final List<IfEntry> testInterfaceEntries = new ArrayList<>();

        for (int i=0; i<10; i++) {
            IfEntryBuilder ifEntryBuilder = new IfEntryBuilder()
            .setIfIndex(new InterfaceIndex(i + 1))
            .setIfAdminStatus(IfEntry.IfAdminStatus.forValue(i % 3))
            .setIfDescr(new DisplayString(String.format("Interface %s", i)))
            .setIfInErrors(new Counter32(99l-i));
            testInterfaceEntries.add(ifEntryBuilder.build());
        }

        // Set up the response for the mock snmp4j
        // This is responsible for calling the onResponse() callback for SNMP messages
        doAnswer(new Answer<Object>() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PDU requestPDU = (PDU) invocation.getArguments()[0];
                ResponseListener callback = (ResponseListener) invocation.getArguments()[3];

                // Create the response PDU based on the request PDU
                PDU responsePDU = new PDU();
                responsePDU.setType(PDU.GET);

                // Get the OID of the request
                assertEquals("Checking size of PDU response variable bindings", 1, requestPDU.getVariableBindings().size());
                OID requestOID = requestPDU.getVariableBindings().get(0).getOid();

                for (int i=0; i<testInterfaceEntries.size(); i++) {
                    IfEntry testIfEntry = testInterfaceEntries.get(i);
                    int[] prefix = requestOID.getValue();
                    Variable val = null;

                    if (requestOID.equals(ifIndexOID)) {
                        // Add all of the ifIndexes to the response
                        val = new Integer32(i + 1);

                    } else if (requestOID.equals(ifAdminStatusOID)) {
                        val = new Integer32(i % 3);

                    } else if (requestOID.equals(ifDescrOID)) {
                        val = new OctetString(testIfEntry.getIfDescr().toString());

                    } else if (requestOID.equals(ifInErrorsOID)) {
                        val = new org.snmp4j.smi.Counter32(testIfEntry.getIfInErrors().getValue());

                    } else {
                        // Don't add any variable bindings to the response
                        break;
                    }

                    if (val != null) {
                        OID objOID = new OID(prefix, i);
                        responsePDU.add(new VariableBinding(objOID, val));
                    }
                }

                ResponseEvent responseEvent = new ResponseEvent(mockSnmp,
                        new UdpAddress(Inet4Address.getByName(GET_IP_ADDRESS), snmpListenPort),
                        requestPDU,
                        responsePDU,
                        null,
                        null);

                callback.onResponse(responseEvent);
                return null;
            }
        }).when(mockSnmp).send(any(PDU.class), any(Target.class), any(), (ResponseListener) any());

        try (SNMPImpl snmpImpl = new SNMPImpl(mockRpcReg)) {
            Ipv4Address ip = new Ipv4Address(GET_IP_ADDRESS);
            GetInterfacesInputBuilder input = new GetInterfacesInputBuilder();
            input.setCommunity(COMMUNITY);
            input.setIpAddress(ip);

            RpcResult<GetInterfacesOutput> result = null;
            Future<RpcResult<GetInterfacesOutput>> resultFuture = snmpImpl.getInterfaces(input.build());
            result = resultFuture.get();
            assertTrue(result.isSuccessful());
        }
    }
}
