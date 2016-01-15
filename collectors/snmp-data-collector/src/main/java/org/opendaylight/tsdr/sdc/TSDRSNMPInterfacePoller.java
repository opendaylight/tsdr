/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2015 Tata Consultancy Services, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.sdc;

import org.opendaylight.tsdr.spi.util.DataEncrypter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.GetInterfacesOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;
import java.util.Dictionary;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
/**
 * @author Sharon Aicler(saichler@gmail.com)
 * @author Prajaya Talwar(prajaya.talwar@tcs.com)
 * @author Trapti Khandelwal(trapti.khandelwal@tcs.com)
 * @author Razi Ahmed(ahmed.razi@tcs.com)
 **/
// The SNMP interface poller is polling the SNMP
public class TSDRSNMPInterfacePoller extends Thread {
    // The collector
    private SNMPDataCollector collector = null;
    private Dictionary<String, String[]> configuration = null;
    private int nodeConfigDetails = 0;
    private Ipv4Address ip = null;
    private String community = null;
    private RpcResult<GetInterfacesOutput> result = null;

    public TSDRSNMPInterfacePoller(SNMPDataCollector _collector) {
        super("TSDR SNMP Interface Poller");
        this.collector = _collector;
        this.setDaemon(true);
        this.collector.loadConfigData();
        configuration = TSDRSNMPConfig.getInstance().getConfiguration();
        this.start();
    }

    public void run() {
        while (collector.isRunning()) {
            //Call handle method of TSDR SNMP Collector
            for(nodeConfigDetails = 0; nodeConfigDetails < configuration.get(TSDRSNMPConfig.P_CREDENTIALS).length; nodeConfigDetails += 2)
            {
                ip = new Ipv4Address(configuration.get(TSDRSNMPConfig.P_CREDENTIALS)[nodeConfigDetails].toString());
                community = DataEncrypter.decrypt(configuration.get(TSDRSNMPConfig.P_CREDENTIALS)[nodeConfigDetails+1].toString());
                result = collector.loadGetInterfacesData(ip,community);
                collector.insertInterfacesEntries(ip,result);
            }
            synchronized(this.collector){
                this.collector.notifyAll();
            }
            //This object is only for the time when we shutdown so we want to break the waiting time
            synchronized(this.collector.pollerSyncObject){
                try {
                    this.collector.pollerSyncObject.wait(this.collector.getConfigData().getPollingInterval());
                } catch (InterruptedException err) {
                    SNMPDataCollector.log(
                            "Unknown error when sleeping in TSDR SNMP poller",
                            SNMPDataCollector.ERROR);
                    Thread.currentThread().interrupt();
                }
            }
            this.collector.loadConfigData();
            configuration = TSDRSNMPConfig.getInstance().getConfiguration();
        }
    }
}
