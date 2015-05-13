/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.datacollection;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.QueueKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.IdentifiableItem;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.Item;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;

/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
/*
 * This class is an abstract listener where all listeners should inherit as it
 * gives services of tracking the registration of the listener + retrieving the
 * initial data.
 */
public abstract class TSDRBaseDataChangeListener implements DataChangeListener {
    private InstanceIdentifier<? extends DataObject> IID = null;
    private ListenerRegistration<DataChangeListener> registration = null;
    private String keyName = null;
    private String key = null;
    private DataObject baseLine = null;
    private TSDRDOMCollector collector = null;

    public TSDRBaseDataChangeListener(InstanceIdentifier<Node> nodeID,
            InstanceIdentifier<? extends DataObject> _IID, String _keyName,
            TSDRDOMCollector _collector) {
        this.IID = _IID;
        this.keyName = _keyName;
        this.key = this.IID.toString() + this.keyName;
        this.collector = _collector;
        ReadOnlyTransaction rot = collector.getDataBroker()
                .newReadOnlyTransaction();
        try {
            this.baseLine = rot
                    .read(LogicalDatastoreType.OPERATIONAL, this.IID).get()
                    .get();
            this.registration = collector.getDataBroker()
                    .registerDataChangeListener(
                            LogicalDatastoreType.OPERATIONAL, this.IID, this,
                            DataChangeScope.SUBTREE);
            collector.addListener(nodeID, this.IID, this.keyName, this);
            TSDRDOMCollector.log("Added Listener for " + this.keyName,TSDRDOMCollector.DEBUG);
        } catch (Exception e) {
            // If the data does not exist in the node, we might hit this error a
            // lot.
            // Not logging this error as if the operation fails, the listener
            // will not be added
            // And we will wait for the next iteration to see if the data is
            // there.
        } finally {
            rot.close();
        }
    }

    public String getKey() {
        return key;
    }

    public void closeRegistrations() {
        this.registration.close();
    }

    public InstanceIdentifier<? extends DataObject> getIID() {
        return this.IID;
    }

    public TSDRDOMCollector getCollector() {
        return this.collector;
    }

    public DataObject getBaseLine() {
        return this.baseLine;
    }

    /*
     * Create a list of RecordKeys representing the metric path from the
     * InstanceIdentifier.
     */
    public static List<RecordKeys> createRecordKeys(
            InstanceIdentifier<?> instanceID) {
        List<RecordKeys> recKeys = new ArrayList<RecordKeys>(5);
        for (PathArgument pa : instanceID.getPathArguments()) {
            if (pa instanceof Item) {
                RecordKeysBuilder rec = new RecordKeysBuilder();
                rec.setKeyName(pa.getType().getSimpleName());
                rec.setKeyValue("");
                recKeys.add(rec.build());
            } else if (pa instanceof IdentifiableItem) {
                recKeys.add(getIdentifiableItemID((IdentifiableItem) pa));
            } else {
                TSDRDOMCollector.log("Missed class type:"
                        + pa.getClass().getName(), TSDRDOMCollector.ERROR);
            }
        }
        return recKeys;
    }

    // Create a RecordsKeys "shrink" instance path from an InstanceIdentifier
    public static RecordKeys getIdentifiableItemID(
            IdentifiableItem<?, Identifier<?>> ia) {
        RecordKeysBuilder rec = new RecordKeysBuilder();
        rec.setKeyName(ia.getType().getSimpleName());
        if (ia.getKey() instanceof FlowKey) {
            FlowKey flowKey = (FlowKey) ia.getKey();
            rec.setKeyValue("" + flowKey.getId().getValue());
        } else if (ia.getKey() instanceof QueueKey) {
            QueueKey qk = (QueueKey) ia.getKey();
            rec.setKeyValue("" + qk.getQueueId().getValue());
        } else if (ia.getKey() instanceof GroupKey) {
            GroupKey gk = (GroupKey) ia.getKey();
            rec.setKeyValue("" + gk.getGroupId().getValue());
        } else if (ia.getKey() instanceof NodeConnectorKey) {
            NodeConnectorKey nck = (NodeConnectorKey) ia.getKey();
            rec.setKeyValue(nck.getId().getValue());
        } else if (ia.getKey() instanceof NodeKey) {
            NodeKey nk = (NodeKey) ia.getKey();
            rec.setKeyValue(nk.getId().getValue());
        } else if (ia.getKey() instanceof TableKey) {
            TableKey tk = (TableKey) ia.getKey();
            rec.setKeyValue("" + tk.getId());
        } else {
            TSDRDOMCollector.log("Error! - Missed Key Of type "
                    + ia.getType().getName(), TSDRDOMCollector.ERROR);
        }
        return rec.build();
    }

    public static long getTimeStamp() {
        return System.currentTimeMillis();
    }
}