/*
 * Copyright (c) 2015 Tata Consultancy Services, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.sdc;

import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.GetInterfacesOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;
import java.util.Dictionary;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
/**
 * @author Prajaya Talwar(prajaya.talwar@tcs.com)
 **/
// The SNMP interface poller is polling the SNMP
public class TSDRSNMPInterfacePoller extends Thread {
    // The collector
    private SNMPDataCollector collector = null;

    public TSDRSNMPInterfacePoller(SNMPDataCollector _collector) {
        super("TSDR SNMP Interface Poller");
        this.collector = _collector;
        this.setDaemon(true);
        _collector.loadConfigData();
        this.start();
    }

    public void run() {
        while (collector.isRunning()) {
            //Call handle method of TSDR SNMP Collector
             Dictionary<String, Object> configuration = TSDRSNMPConfig.getInstance().getConfiguration();
             Ipv4Address ip = new Ipv4Address(configuration.get(TSDRSNMPConfig.P_HOST).toString());
             String community = configuration.get(TSDRSNMPConfig.P_COMMUNITY).toString();
             RpcResult<GetInterfacesOutput> result = null;
             result = collector.loadGetInterfacesData(ip,community);
             collector.insertInterfacesEntries(ip,result);

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
                }
            }
            this.collector.loadConfigData();
        }
    }
}
