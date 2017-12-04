/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.osc;

import com.google.common.base.Optional;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Polls the inventory every 15 seconds and determines if there are nodes added/removed.
 *
 * @author Sharon Aicler(saichler@gmail.com)
 */
public class TSDRInventoryNodesPoller extends Thread implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(TSDRInventoryNodesPoller.class);

    // The collector
    private final TSDROpenflowCollector collector;

    private final Object shutdownSync = new Object();

    public TSDRInventoryNodesPoller(TSDROpenflowCollector collector) {
        super("TSDR Inventory Nodes Poller");
        this.collector = collector;
        this.setDaemon(true);
        collector.loadConfigData();
    }

    @Override
    public void run() {
        // List of nodes already registered on
        Set<InstanceIdentifier<Node>> knownNodes = new HashSet<>();

        while (collector.isRunning()) {
            InstanceIdentifier<Nodes> id = InstanceIdentifier.builder(Nodes.class).build();
            ReadOnlyTransaction readTx = collector.getDataBroker().newReadOnlyTransaction();
            try {
                Optional<Nodes> optional = readTx.read(LogicalDatastoreType.OPERATIONAL, id).get();
                if (optional.isPresent()) {
                    Nodes nodes = optional.get();
                    Set<InstanceIdentifier<Node>> nodeSet = new HashSet<>();
                    for (Node n : nodes.getNode()) {
                        InstanceIdentifier<Node> nodeID = id.child(Node.class, n.getKey());
                        nodeSet.add(nodeID);
                        collector.collectStatistics(n);
                    }
                    // Register on added nodes
                    for (InstanceIdentifier<Node> nodeID : nodeSet) {
                        knownNodes.add(nodeID);
                        // The registration won't register on those nodes that
                        // already have a registration in place
                        // collector.registerOnStatistics(nodeID);
                    }
                    // unregister on removed nodes
                    for (Iterator<InstanceIdentifier<Node>> iter = knownNodes.iterator(); iter.hasNext();) {
                        InstanceIdentifier<Node> nodeID = iter.next();
                        if (!nodeSet.contains(nodeID)) {
                            iter.remove();
                            collector.removeAllNodeBuilders(nodeID);
                        }
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Error reading inventory", e);
            } finally {
                readTx.close();
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
                    LOG.debug("Interrupted when sleeping in TSDR poller", e);
                    Thread.currentThread().interrupt();
                }
            }

            this.collector.loadConfigData();
        }
    }

    @Override
    public void close() {
        synchronized (shutdownSync) {
            shutdownSync.notifyAll();
        }
    }
}
