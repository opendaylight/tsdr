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
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.FlowCapableNodeConnectorQueueStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.flow.capable.node.connector.queue.statistics.FlowCapableNodeConnectorQueueStatistics;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
// The registered lister for
// FlowCapableNodeConnectorQueueStatisticsDataListener, create and update the
// FlowCapableNodeConnectorQueueStatisticsDataListener notifications
// to the TSDRMetricRecordBuilder.
public class FlowCapableNodeConnectorQueueStatisticsDataListener extends
        TSDRBaseDataChangeListener {

    public FlowCapableNodeConnectorQueueStatisticsDataListener(
            InstanceIdentifier<Node> nodeID,
            InstanceIdentifier<? extends DataObject> IID,
            TSDRDOMCollector _collector) {
        super(nodeID, IID,
                "FlowCapableNodeConnectorQueueStatisticsDataListener",
                _collector);
        if (getBaseLine() == null)
            return;
        FlowCapableNodeConnectorQueueStatisticsData stData = (FlowCapableNodeConnectorQueueStatisticsData) getBaseLine();
        FlowCapableNodeConnectorQueueStatistics gs = stData
                .getFlowCapableNodeConnectorQueueStatistics();
        TSDRMetricRecordBuilderContainer bc = getCollector()
                .getTSDRMetricRecordBuilderContainer(IID);
        if (bc == null) {
            List<RecordKeys> recKeys = createRecordKeys(IID);
            getCollector().createTSDRMetricRecordBuilder(IID, recKeys,
                    "TransmissionErrors", "" + gs.getTransmissionErrors().getValue(),
                    DataCategory.QUEUESTATS);
            getCollector().createTSDRMetricRecordBuilder(IID, recKeys,
                    "TransmittedBytes", "" + gs.getTransmittedBytes().getValue(),
                    DataCategory.QUEUESTATS);
            getCollector().createTSDRMetricRecordBuilder(IID, recKeys,
                    "TransmittedPackets", "" + gs.getTransmittedPackets().getValue(),
                    DataCategory.QUEUESTATS);
        }
    }

    public void onDataChanged(
            AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> data) {
        Map<InstanceIdentifier<?>, DataObject> updates = data.getUpdatedData();
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : updates
                .entrySet()) {
            if (entry.getValue() instanceof FlowCapableNodeConnectorQueueStatisticsData) {
                FlowCapableNodeConnectorQueueStatisticsData stData = (FlowCapableNodeConnectorQueueStatisticsData) entry
                        .getValue();
                FlowCapableNodeConnectorQueueStatistics gs = stData
                        .getFlowCapableNodeConnectorQueueStatistics();
                InstanceIdentifier<?> id = entry.getKey();
                TSDRMetricRecordBuilderContainer bc = getCollector()
                        .getTSDRMetricRecordBuilderContainer(id);
                if (bc != null) {
                    TSDRMetricRecordBuilder builder[] = bc.getBuilders();
                    long timeStamp = getTimeStamp();
                    builder[0].setMetricValue(new Counter64(new BigInteger(""
                            + gs.getTransmissionErrors().getValue())));
                    builder[0].setTimeStamp(timeStamp);
                    builder[1].setMetricValue(new Counter64(new BigInteger(""
                            + gs.getTransmittedBytes().getValue())));
                    builder[1].setTimeStamp(timeStamp);
                    builder[2].setMetricValue(new Counter64(new BigInteger(""
                            + gs.getTransmittedPackets().getValue())));
                    builder[2].setTimeStamp(timeStamp);
                } else {
                    List<RecordKeys> recKeys = createRecordKeys(id);
                    getCollector().createTSDRMetricRecordBuilder(id, recKeys,
                            "TransmissionErrors",
                            "" + gs.getTransmissionErrors().getValue(),
                            DataCategory.QUEUESTATS);
                    getCollector().createTSDRMetricRecordBuilder(id, recKeys,
                            "TransmittedBytes", "" + gs.getTransmittedBytes().getValue(),
                            DataCategory.QUEUESTATS);
                    getCollector().createTSDRMetricRecordBuilder(id, recKeys,
                            "TransmittedPackets",
                            "" + gs.getTransmittedPackets().getValue(),
                            DataCategory.QUEUESTATS);
                }
            }
        }
    }
}