/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.datacollection;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
// The inventory nodes poller is polling the inventory every 15 seconds and
// determinate if there are nodes added/removed
public class TSDRInventoryNodesPoller extends Thread {
    // List of nodes already registered on
    private Set<InstanceIdentifier<Node>> knownNodes = new HashSet<>();
    // The collector
    private TSDRDOMCollector collector = null;

    public TSDRInventoryNodesPoller(TSDRDOMCollector _collector) {
        super("TSDR Inventory Nodes Poller");
        this.collector = _collector;
        this.start();
    }

    public void run() {
        while (collector.isRunning()) {
            InstanceIdentifier<Nodes> id = InstanceIdentifier.builder(
                    Nodes.class).build();
            ReadOnlyTransaction read = collector.getDataBroker()
                    .newReadOnlyTransaction();
            try {
                Nodes nodes = read.read(LogicalDatastoreType.OPERATIONAL, id)
                        .get().get();
                Set<InstanceIdentifier<Node>> nodeSet = new HashSet<>();
                for (Node n : nodes.getNode()) {
                    InstanceIdentifier<Node> nodeID = id.child(Node.class,
                            n.getKey());
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
                for (Iterator<InstanceIdentifier<Node>> iter = knownNodes
                        .iterator(); iter.hasNext();) {
                    InstanceIdentifier<Node> nodeID = iter.next();
                    if (!nodeSet.contains(nodeID)) {
                        iter.remove();
                        collector.removeAllNodeBuilders(nodeID);
                    }
                }
            } catch (Exception err) {
                TSDRDOMCollector.log("No Nodes are available",
                        TSDRDOMCollector.INFO);
            } finally {
                read.close();
            }
            try {
                Thread.sleep(15000);
            } catch (InterruptedException err) {
                TSDRDOMCollector.log(
                        "Unknown error when sleeping in TSDR poller",
                        TSDRDOMCollector.ERROR);
            }
            ;
        }
    }
}
