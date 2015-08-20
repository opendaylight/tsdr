/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.osc.handlers;

import java.math.BigInteger;
import java.util.List;

import org.opendaylight.tsdr.osc.TSDRBaseDataHandler;
import org.opendaylight.tsdr.osc.TSDRDOMCollector;
import org.opendaylight.tsdr.osc.TSDRMetricRecordBuilderContainer;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Counter64;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.FlowCapableNodeConnectorStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.flow.capable.node.connector.statistics.FlowCapableNodeConnectorStatistics;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
/*
 * A Handler for the NodeConnectorStatistics data
 */
public class NodeConnectorStatisticsChangeHandler extends TSDRBaseDataHandler {
    public NodeConnectorStatisticsChangeHandler(TSDRDOMCollector _collector) {
        super(_collector);
    }

    @Override
    public void handleData(InstanceIdentifier<Node> nodeID, InstanceIdentifier<?> id, DataObject dataObject) {
        FlowCapableNodeConnectorStatisticsData stData = (FlowCapableNodeConnectorStatisticsData) dataObject;
        FlowCapableNodeConnectorStatistics fs = stData.getFlowCapableNodeConnectorStatistics();
        if(fs==null){
            //no data yet, ignore
            return;
        }
        TSDRMetricRecordBuilderContainer bc = getCollector()
                .getTSDRMetricRecordBuilderContainer(id);
        if (bc != null) {
            TSDRMetricRecordBuilder builder[] = bc.getBuilders();
            long timeStamp = getTimeStamp();
            builder[0].setMetricValue(new Counter64(new BigInteger(""
                    + fs.getTransmitDrops())));
            builder[0].setTimeStamp(timeStamp);
            builder[1].setMetricValue(new Counter64(new BigInteger(""
                    + fs.getReceiveDrops())));
            builder[1].setTimeStamp(timeStamp);
            builder[2].setMetricValue(new Counter64(new BigInteger(""
                    + fs.getReceiveCrcError())));
            builder[2].setTimeStamp(timeStamp);
            builder[3].setMetricValue(new Counter64(new BigInteger(""
                    + fs.getReceiveFrameError())));
            builder[3].setTimeStamp(timeStamp);
            builder[4].setMetricValue(new Counter64(new BigInteger(""
                    + fs.getReceiveOverRunError())));
            builder[4].setTimeStamp(timeStamp);
            builder[5].setMetricValue(new Counter64(new BigInteger(""
                    + fs.getTransmitErrors())));
            builder[5].setTimeStamp(timeStamp);
            builder[6].setMetricValue(new Counter64(new BigInteger(""
                    + fs.getCollisionCount())));
            builder[6].setTimeStamp(timeStamp);
            builder[7].setMetricValue(new Counter64(new BigInteger(""
                    + fs.getReceiveErrors())));
            builder[7].setTimeStamp(timeStamp);
            builder[8].setTimeStamp(timeStamp);
            builder[9].setTimeStamp(timeStamp);
            builder[10].setTimeStamp(timeStamp);
            builder[11].setTimeStamp(timeStamp);
            if (fs.getBytes() != null) {
                builder[8].setMetricValue(new Counter64(fs.getBytes()
                        .getTransmitted()));
                builder[9].setMetricValue(new Counter64(fs.getBytes()
                        .getReceived()));
            }
            if (fs.getPackets() != null) {
                builder[10].setMetricValue(new Counter64(fs.getPackets()
                        .getTransmitted()));
                builder[11].setMetricValue(new Counter64(fs.getPackets()
                        .getReceived()));
            }
        } else {
            List<RecordKeys> recKeys = createRecordKeys(id);
            getCollector().createTSDRMetricRecordBuilder(nodeID,id, recKeys,
                    "TransmitDrops", "" + fs.getTransmitDrops(),
                    DataCategory.PORTSTATS);
            getCollector().createTSDRMetricRecordBuilder(nodeID,id, recKeys,
                    "ReceiveDrops", "" + fs.getReceiveDrops(),
                    DataCategory.PORTSTATS);
            getCollector().createTSDRMetricRecordBuilder(nodeID,id, recKeys,
                    "ReceiveCrcError", "" + fs.getReceiveCrcError(),
                    DataCategory.PORTSTATS);
            getCollector().createTSDRMetricRecordBuilder(nodeID,id, recKeys,
                    "ReceiveFrameError", "" + fs.getReceiveFrameError(),
                    DataCategory.PORTSTATS);
            getCollector().createTSDRMetricRecordBuilder(nodeID,id, recKeys,
                    "ReceiveOverRunError", "" + fs.getReceiveOverRunError(),
                    DataCategory.PORTSTATS);
            getCollector().createTSDRMetricRecordBuilder(nodeID,id, recKeys,
                    "TransmitErrors", "" + fs.getTransmitErrors(),
                    DataCategory.PORTSTATS);
            getCollector().createTSDRMetricRecordBuilder(nodeID,id, recKeys,
                    "CollisionCount", "" + fs.getCollisionCount(),
                    DataCategory.PORTSTATS);
            getCollector().createTSDRMetricRecordBuilder(nodeID,id, recKeys,
                    "ReceiveErrors", "" + fs.getReceiveErrors(),
                    DataCategory.PORTSTATS);
            getCollector().createTSDRMetricRecordBuilder(nodeID,id, recKeys,
                    "TransmittedBytes", "0", DataCategory.PORTSTATS);
            getCollector().createTSDRMetricRecordBuilder(nodeID,id, recKeys,
                    "ReceivedBytes", "0", DataCategory.PORTSTATS);
            getCollector().createTSDRMetricRecordBuilder(nodeID,id, recKeys,
                    "TransmittedPackets", "0", DataCategory.PORTSTATS);
            getCollector().createTSDRMetricRecordBuilder(nodeID,id, recKeys,
                    "ReceivedPackets", "0", DataCategory.PORTSTATS);
        }
    }
}