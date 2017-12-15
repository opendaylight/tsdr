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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.FlowTableStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.flow.table.statistics.FlowTableStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Handles NodeTableStatistics data type.
 *
 * @author Sharon Aicler(saichler@gmail.com)
 */
public class NodeTableStatisticsChangeHandler extends TSDRBaseDataHandler<FlowTableStatisticsData> {
    public NodeTableStatisticsChangeHandler(TSDROpenflowCollector collector) {
        super(collector);
    }

    @Override
    public void handleData(InstanceIdentifier<Node> nodeID, InstanceIdentifier<?> id, FlowTableStatisticsData table) {
        FlowTableStatistics fs = table.getFlowTableStatistics();
        if (fs == null) {
            //no data yet, ignore
            return;
        }
        TSDRMetricRecordBuilderContainer bc = getCollector()
                .getTSDRMetricRecordBuilderContainer(id);
        if (bc != null) {
            TSDRMetricRecordBuilder[] builder = bc.getBuilders();
            long timeStamp = getTimeStamp();
            builder[0].setMetricValue(FormatUtil.toMetricValue(fs.getActiveFlows()));
            builder[0].setTimeStamp(timeStamp);
            builder[1].setMetricValue(FormatUtil.toMetricValue(fs.getPacketsMatched()));
            builder[1].setTimeStamp(timeStamp);
            builder[2].setMetricValue(FormatUtil.toMetricValue(fs.getPacketsLookedUp()));
            builder[2].setTimeStamp(timeStamp);
        } else {
            List<RecordKeys> recKeys = createRecordKeys(id);
            getCollector().createTSDRMetricRecordBuilder(nodeID, id, recKeys,
                    "ActiveFlows", FormatUtil.toMetricValue(fs.getActiveFlows()),
                    DataCategory.FLOWTABLESTATS);
            getCollector().createTSDRMetricRecordBuilder(nodeID, id, recKeys,
                    "PacketMatch", FormatUtil.toMetricValue(fs.getPacketsMatched()),
                    DataCategory.FLOWTABLESTATS);
            getCollector().createTSDRMetricRecordBuilder(nodeID, id, recKeys,
                    "PacketLookup", FormatUtil.toMetricValue(fs.getPacketsLookedUp()),
                    DataCategory.FLOWTABLESTATS);
        }
    }
}
