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
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.group.statistics.GroupStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * Handles NodeGroupStatistics data type.
 *
 * @author Sharon Aicler(saichler@gmail.com)
 */
public class NodeGroupStatisticsChangeHandler extends TSDRBaseDataHandler<NodeGroupStatistics> {
    public NodeGroupStatisticsChangeHandler(TSDROpenflowCollector collector) {
        super(collector);
    }

    @Override
    public void handleData(InstanceIdentifier<Node> nodeID, InstanceIdentifier<?> id, NodeGroupStatistics stData) {
        GroupStatistics gs = stData.getGroupStatistics();
        if (gs == null) {
            //no data yet, ignore
            return;
        }
        TSDRMetricRecordBuilderContainer bc = getCollector()
                .getTSDRMetricRecordBuilderContainer(id);
        if (bc != null) {
            TSDRMetricRecordBuilder[] builder = bc.getBuilders();
            long timeStamp = getTimeStamp();
            builder[0].setMetricValue(FormatUtil.toMetricValue(gs.getRefCount()));
            builder[0].setTimeStamp(timeStamp);
            builder[1].setMetricValue(FormatUtil.toMetricValue(gs.getPacketCount().getValue()));
            builder[1].setTimeStamp(timeStamp);
            builder[2].setMetricValue(FormatUtil.toMetricValue(gs.getByteCount().getValue()));
            builder[2].setTimeStamp(timeStamp);
        } else {
            List<RecordKeys> recKeys = createRecordKeys(id);
            getCollector().createTSDRMetricRecordBuilder(nodeID, id, recKeys,
                    "RefCount", FormatUtil.toMetricValue(gs.getRefCount()),
                    DataCategory.FLOWGROUPSTATS);
            getCollector().createTSDRMetricRecordBuilder(nodeID, id, recKeys,
                    "PacketCount", FormatUtil.toMetricValue(gs.getPacketCount()),
                    DataCategory.FLOWGROUPSTATS);
            getCollector().createTSDRMetricRecordBuilder(nodeID, id, recKeys,
                    "ByteCount", FormatUtil.toMetricValue(gs.getByteCount()),
                    DataCategory.FLOWGROUPSTATS);
        }
    }
}
