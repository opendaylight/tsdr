/*
 * Copyright (c) 2015 Tata Consultancy Services, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.sdc;

/**
 * @author Prajaya Talwar(prajaya.talwar@tcs.com)
 **/
// The inventory nodes poller is polling the inventory every 15 seconds and
// determinate if there are nodes added/removed
public class TSDRSNMPInterfacePoller extends Thread {
    // The collector
    private SNMPDataCollector collector = null;

    public TSDRSNMPInterfacePoller(SNMPDataCollector _collector) {
        super("TSDR SNMP Interface Poller");
        this.collector = _collector;
        _collector.loadConfigData();
        this.start();
    }

    public void run() {
        while (collector.isRunning()) {
            //Call handle method of TSDR SNMP Collector
            collector.loadGetInterfacesData();
            collector.insertInterfacesEnteries();
            synchronized(this.collector){
                this.collector.notifyAll();
            }
            //This object is only for the time when we shutdown so we want to break the waiting time
            synchronized(this.collector.pollerSyncObject){
                try {
                    this.collector.pollerSyncObject.wait(this.collector.getConfigData().getPollingInterval());
                } catch (InterruptedException err) {
                    SNMPDataCollector.log(
                            "Unknown error when sleeping in TSDR poller",
                            SNMPDataCollector.ERROR);
                }
            }
            this.collector.loadConfigData();
        }
    }
}
