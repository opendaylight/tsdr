/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.osc;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.MeterKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.QueueKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
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
 * This class is an abstract handler where all handlers should inherit as it has
 * a reference to the main collector and provide some common methods to use by
 * the handlers.
 */
public abstract class TSDRBaseDataHandler {
    private TSDRDOMCollector collector = null;

    public TSDRBaseDataHandler(TSDRDOMCollector _collector) {
        this.collector = _collector;
    }

    /*
     * An abstract method that each handler should implement accordign to the
     * type of data it is handling.
     */
    public abstract void handleData(InstanceIdentifier<Node> nodeID,
            InstanceIdentifier<?> id, DataObject dataObject);

    /*
     * Returns a reference to the main collector
     */
    public TSDRDOMCollector getCollector() {
        return this.collector;
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
                /* Bug 3465 - metric detail missleading issue
                RecordKeysBuilder rec = new RecordKeysBuilder();
                rec.setKeyName(pa.getType().getSimpleName());
                rec.setKeyValue(pa.getType().getSimpleName());
                recKeys.add(rec.build());
                */
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

        if (ia.getKey() instanceof MeterKey){
            MeterKey mk = (MeterKey)ia.getKey();
            rec.setKeyValue(""+mk.getMeterId().getValue());
        } else
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
            throw new IllegalArgumentException("Unknown DataObject Key of type "+ia.getType().getName());
        }
        return rec.build();
    }

    public static long getTimeStamp() {
        return System.currentTimeMillis();
    }
}
