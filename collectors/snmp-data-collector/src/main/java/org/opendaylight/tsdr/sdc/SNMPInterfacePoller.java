/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2015 Tata Consultancy Services, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.sdc;

import java.util.Dictionary;
import org.opendaylight.tsdr.spi.util.DataEncrypter;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.GetInterfacesOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The SNMP interface poller is polling the SNMP.
 *
 * @author Sharon Aicler(saichler@gmail.com)
 * @author Prajaya Talwar(prajaya.talwar@tcs.com)
 * @author Trapti Khandelwal(trapti.khandelwal@tcs.com)
 * @author Razi Ahmed(ahmed.razi@tcs.com)
 */
public class SNMPInterfacePoller extends Thread implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(SNMPInterfacePoller.class);

    // The collector
    private final SNMPDataCollector collector;
    private int nodeConfigDetails = 0;
    private String community;
    private RpcResult<GetInterfacesOutput> result;
    private final Object shutdownSync = new Object();

    public SNMPInterfacePoller(SNMPDataCollector collector) {
        super("TSDR SNMP Interface Poller");
        this.collector = collector;
        this.setDaemon(true);
    }

    @Override
    public void run() {
        while (collector.isRunning()) {
            this.collector.loadConfigData();
            Dictionary<String, String[]> configuration = collector.getSnmpConfig().getConfiguration();

            final String[] credentials = configuration.get(SNMPConfig.P_CREDENTIALS);
            for (nodeConfigDetails = 0; nodeConfigDetails < credentials.length; nodeConfigDetails += 2) {
                Ipv4Address ip = new Ipv4Address(credentials[nodeConfigDetails]);
                community = DataEncrypter.decrypt(credentials[nodeConfigDetails + 1]);
                result = collector.loadGetInterfacesData(ip, community);
                collector.insertInterfacesEntries(ip, result);
            }

            synchronized (this.collector) {
                this.collector.notifyAll();
            }

            //This object is only for the time when we shutdown so we want to break the waiting time
            synchronized (shutdownSync) {
                try {
                    if (collector.isRunning()) {
                        shutdownSync.wait(this.collector.getConfigData().getPollingInterval());
                    }
                } catch (InterruptedException e) {
                    LOG.debug("Interrupted when sleeping in SNMP poller", e);
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @Override
    public void close() {
        synchronized (shutdownSync) {
            shutdownSync.notifyAll();
        }
    }
}
