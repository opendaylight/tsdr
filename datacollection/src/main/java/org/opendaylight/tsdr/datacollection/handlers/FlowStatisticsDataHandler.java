/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.datacollection.handlers;

import java.math.BigInteger;
import java.util.List;

import org.opendaylight.tsdr.datacollection.TSDRBaseDataHandler;
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
/*
 * Handle data of type FlowStatisticsData
 */
public class FlowStatisticsDataHandler extends TSDRBaseDataHandler {
    public FlowStatisticsDataHandler(TSDRDOMCollector _collector) {
        super(_collector);
    }

    @Override
    public void handleData(InstanceIdentifier<Node> nodeID, InstanceIdentifier<?> id, DataObject dataObject) {
        FlowStatisticsData stData = (FlowStatisticsData) dataObject;
        FlowStatistics gs = stData.getFlowStatistics();
        if(gs==null){
            //no data yet, ignore
            return;
        }
        TSDRMetricRecordBuilderContainer bc = getCollector().getTSDRMetricRecordBuilderContainer(id);
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
            getCollector().createTSDRMetricRecordBuilder(nodeID,id, recKeys,
                    "ByteCount", "" + gs.getByteCount().getValue(),
                    DataCategory.FLOWSTATS);
            getCollector().createTSDRMetricRecordBuilder(nodeID,id, recKeys,
                    "PacketCount", "" + gs.getPacketCount().getValue(),
                    DataCategory.FLOWSTATS);
        }
    }
}
