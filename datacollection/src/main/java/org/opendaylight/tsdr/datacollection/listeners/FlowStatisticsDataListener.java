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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.statistics.FlowStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
// Flow Statistics Listener
public class FlowStatisticsDataListener extends TSDRBaseDataChangeListener {
    public FlowStatisticsDataListener(InstanceIdentifier<Node> nodeID,
            InstanceIdentifier<? extends DataObject> IID,
            TSDRDOMCollector _collector) {
        super(nodeID, IID, "FlowStatisticsDataListener", _collector);
        if (getBaseLine() == null)
            return;
        FlowStatisticsData flowStatisticsData = (FlowStatisticsData) getBaseLine();
        FlowStatistics flowStatistics = flowStatisticsData.getFlowStatistics();
        TSDRMetricRecordBuilderContainer bc = getCollector()
                .getTSDRMetricRecordBuilderContainer(IID);
        if (bc == null) {
            List<RecordKeys> recKeys = createRecordKeys(IID);
            getCollector().createTSDRMetricRecordBuilder(IID, recKeys,
                    "ByteCount", "" + flowStatistics.getByteCount().getValue(),
                    DataCategory.FLOWSTATS);
            getCollector().createTSDRMetricRecordBuilder(IID, recKeys,
                    "PacketCount",
                    "" + flowStatistics.getPacketCount().getValue(),
                    DataCategory.FLOWSTATS);
        }
    }

    @Override
    public void onDataChanged(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> data) {
        Map<InstanceIdentifier<?>, DataObject> updates = data.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : updates
                .entrySet()) {
            if (entry.getValue() instanceof FlowStatisticsData) {
                FlowStatisticsData stData = (FlowStatisticsData) entry
                        .getValue();
                FlowStatistics gs = stData.getFlowStatistics();
                InstanceIdentifier<?> id = entry.getKey();
                TSDRMetricRecordBuilderContainer bc = getCollector()
                        .getTSDRMetricRecordBuilderContainer(id);
                if (bc != null) {
                    TSDRMetricRecordBuilder builder[] = bc.getBuilders();
                    long timeStamp = getTimeStamp();
                    builder[0].setMetricValue(new Counter64(new BigInteger(""
                            + gs.getByteCount().getValue())));
                    builder[0].setTimeStamp(timeStamp);
                    builder[1].setMetricValue(new Counter64(new BigInteger(""
                            + gs.getPacketCount().getValue())));
                    builder[1].setTimeStamp(timeStamp);
                } else {
                    List<RecordKeys> recKeys = createRecordKeys(id);
                    getCollector().createTSDRMetricRecordBuilder(id, recKeys,
                            "ByteCount", "" + gs.getByteCount().getValue(),
                            DataCategory.FLOWSTATS);
                    getCollector().createTSDRMetricRecordBuilder(id, recKeys,
                            "PacketCount", "" + gs.getPacketCount().getValue(),
                            DataCategory.FLOWSTATS);
                }
            }
        }
    }
}