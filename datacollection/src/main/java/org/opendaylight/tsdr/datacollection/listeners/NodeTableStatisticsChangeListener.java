/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.datacollection.listeners;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.tsdr.datacollection.TSDRBaseDataChangeListener;
import org.opendaylight.tsdr.datacollection.TSDRDOMCollector;
import org.opendaylight.tsdr.datacollection.TSDRMetricRecordBuilderContainer;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Counter64;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.FlowTableStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.flow.table.statistics.FlowTableStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
// The registered lister for NodeTableStatistics, create and update the
// NodeTableStatistics notifications
// to the TSDRMetricRecordBuilder.
public class NodeTableStatisticsChangeListener extends
        TSDRBaseDataChangeListener {
    public NodeTableStatisticsChangeListener(InstanceIdentifier<Node> nodeID,
            InstanceIdentifier<? extends DataObject> IID,
            TSDRDOMCollector _collector) {
        super(nodeID, IID, "NodeTableStatisticsChangeListener", _collector);
        // There is no data in that node for this metric, skip it.
        if (getBaseLine() == null)
            return;
        // read initial data and use as a baseline
        FlowTableStatisticsData table = (FlowTableStatisticsData) getBaseLine();
        FlowTableStatistics fs = table.getFlowTableStatistics();
        TSDRMetricRecordBuilderContainer bc = getCollector()
                .getTSDRMetricRecordBuilderContainer(IID);
        if (bc == null) {
            List<RecordKeys> recKeys = createRecordKeys(IID);
            getCollector().createTSDRMetricRecordBuilder(IID, recKeys,
                    "ActiveFlows", "" + fs.getActiveFlows().getValue(),
                    DataCategory.FLOWTABLESTATS);
            getCollector().createTSDRMetricRecordBuilder(IID, recKeys,
                    "PacketMatch", "" + fs.getPacketsMatched().getValue(),
                    DataCategory.FLOWTABLESTATS);
            getCollector().createTSDRMetricRecordBuilder(IID, recKeys,
                    "PacketLookup", "" + fs.getPacketsLookedUp().getValue(),
                    DataCategory.FLOWTABLESTATS);
        }
    }

    public void onDataChanged(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> data) {
        Map<InstanceIdentifier<?>, DataObject> updates = data.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : updates
                .entrySet()) {
            if (entry.getValue() instanceof FlowTableStatisticsData) {
                FlowTableStatisticsData table = (FlowTableStatisticsData) entry
                        .getValue();
                FlowTableStatistics fs = table.getFlowTableStatistics();
                InstanceIdentifier<?> id = entry.getKey();
                TSDRMetricRecordBuilderContainer bc = getCollector()
                        .getTSDRMetricRecordBuilderContainer(id);
                if (bc != null) {
                    TSDRMetricRecordBuilder builder[] = bc.getBuilders();
                    BigInteger timeStamp = getTimeStamp();
                    builder[0].setMetricValue(new Counter64(new BigInteger(""
                            + fs.getActiveFlows().getValue())));
                    builder[0].setTimeStamp(timeStamp);
                    builder[1].setMetricValue(new Counter64(new BigInteger(""
                            + fs.getPacketsMatched().getValue())));
                    builder[1].setTimeStamp(timeStamp);
                    builder[2].setMetricValue(new Counter64(new BigInteger(""
                            + fs.getPacketsLookedUp().getValue())));
                    builder[2].setTimeStamp(timeStamp);
                } else {
                    List<RecordKeys> recKeys = createRecordKeys(id);
                    getCollector().createTSDRMetricRecordBuilder(id, recKeys,
                            "ActiveFlows", "" + fs.getActiveFlows().getValue(),
                            DataCategory.FLOWTABLESTATS);
                    getCollector().createTSDRMetricRecordBuilder(id, recKeys,
                            "PacketMatch",
                            "" + fs.getPacketsMatched().getValue(),
                            DataCategory.FLOWTABLESTATS);
                    getCollector().createTSDRMetricRecordBuilder(id, recKeys,
                            "PacketLookup",
                            "" + fs.getPacketsLookedUp().getValue(),
                            DataCategory.FLOWTABLESTATS);
                }
            }
        }
    }
}
