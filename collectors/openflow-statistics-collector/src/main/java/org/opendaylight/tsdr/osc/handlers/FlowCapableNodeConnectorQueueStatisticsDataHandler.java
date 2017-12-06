/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.osc.handlers;

import java.util.List;
import org.opendaylight.tsdr.osc.TSDRBaseDataHandler;
import org.opendaylight.tsdr.osc.TSDRMetricRecordBuilderContainer;
import org.opendaylight.tsdr.osc.TSDROpenflowCollector;
import org.opendaylight.tsdr.spi.util.FormatUtil;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.FlowCapableNodeConnectorQueueStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.flow.capable.node.connector.queue.statistics.FlowCapableNodeConnectorQueueStatistics;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * A handler for FlowCapableNodeConnectorQueueStatisticsData metric.
 *
 * @author Sharon Aicler(saichler@gmail.com)
 */
public class FlowCapableNodeConnectorQueueStatisticsDataHandler
        extends TSDRBaseDataHandler<FlowCapableNodeConnectorQueueStatisticsData> {
    public FlowCapableNodeConnectorQueueStatisticsDataHandler(TSDROpenflowCollector collector) {
        super(collector);
    }

    @Override
    public void handleData(InstanceIdentifier<Node> nodeID,InstanceIdentifier<?> id,
            FlowCapableNodeConnectorQueueStatisticsData stData) {
        FlowCapableNodeConnectorQueueStatistics gs = stData.getFlowCapableNodeConnectorQueueStatistics();
        if (gs == null) {
            // no data yet, ignore
            return;
        }

        TSDRMetricRecordBuilderContainer bc = getCollector().getTSDRMetricRecordBuilderContainer(id);
        if (bc != null) {
            TSDRMetricRecordBuilder[] builder = bc.getBuilders();
            long timeStamp = getTimeStamp();
            builder[0].setMetricValue(FormatUtil.toMetricValue(gs.getTransmissionErrors()));
            builder[0].setTimeStamp(timeStamp);
            builder[1].setMetricValue(FormatUtil.toMetricValue(gs.getTransmittedBytes()));
            builder[1].setTimeStamp(timeStamp);
            builder[2].setMetricValue(FormatUtil.toMetricValue(gs.getTransmittedPackets()));
            builder[2].setTimeStamp(timeStamp);
        } else {
            List<RecordKeys> recKeys = createRecordKeys(id);
            getCollector().createTSDRMetricRecordBuilder(nodeID,id, recKeys,
                    "TransmissionErrors",
                    FormatUtil.toMetricValue(gs.getTransmissionErrors()),
                    DataCategory.QUEUESTATS);
            getCollector().createTSDRMetricRecordBuilder(nodeID,id, recKeys,
                    "TransmittedBytes",
                    FormatUtil.toMetricValue(gs.getTransmittedBytes()),
                    DataCategory.QUEUESTATS);
            getCollector().createTSDRMetricRecordBuilder(nodeID,id, recKeys,
                    "TransmittedPackets",
                    FormatUtil.toMetricValue(gs.getTransmittedPackets()),
                    DataCategory.QUEUESTATS);
        }
    }
}
