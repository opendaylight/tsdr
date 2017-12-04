/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.datacollection;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.tsdr.osc.TSDROpenflowCollector;
import org.opendaylight.tsdr.osc.handlers.FlowCapableNodeConnectorQueueStatisticsDataHandler;
import org.opendaylight.tsdr.osc.handlers.FlowStatisticsDataHandler;
import org.opendaylight.tsdr.osc.handlers.NodeGroupStatisticsChangeHandler;
import org.opendaylight.tsdr.osc.handlers.NodeMeterStatisticsChangeHandler;
import org.opendaylight.tsdr.osc.handlers.NodeTableStatisticsChangeHandler;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Counter32;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Counter64;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.Meter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.MeterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.MeterKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowStatisticsDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.statistics.FlowStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.statistics.FlowStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.FlowTableStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.FlowTableStatisticsDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.flow.table.statistics.FlowTableStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.flow.table.statistics.FlowTableStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.Queue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.QueueBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.QueueKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.queue.rev130925.QueueId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.group.statistics.GroupStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.group.statistics.GroupStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.nodes.node.meter.MeterStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.nodes.node.meter.MeterStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.types.rev130918.MeterId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.node.connector.statistics.BytesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.node.connector.statistics.PacketsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.FlowCapableNodeConnectorStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.FlowCapableNodeConnectorStatisticsDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.flow.capable.node.connector.statistics.FlowCapableNodeConnectorStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.flow.capable.node.connector.statistics.FlowCapableNodeConnectorStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.FlowCapableNodeConnectorQueueStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.FlowCapableNodeConnectorQueueStatisticsDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.flow.capable.node.connector.queue.statistics.FlowCapableNodeConnectorQueueStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.flow.capable.node.connector.queue.statistics.FlowCapableNodeConnectorQueueStatisticsBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class DataCollectionTest {
    private DataBroker dataBroker;
    private final TsdrCollectorSpiService collectorSPIService = mock(TsdrCollectorSpiService.class);
    private ReadOnlyTransaction readTransaction;
    private Nodes nodes;
    private final CheckedFuture<Optional<Nodes>, ReadFailedException> checkedFuture = mock(CheckedFuture.class);
    private final Optional<Nodes> optional = mock(Optional.class);
    private TSDROpenflowCollector collector;

    @Before
    public void before() throws InterruptedException, ExecutionException {
        dataBroker = mock(DataBroker.class);
        readTransaction = mock(ReadOnlyTransaction.class);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readTransaction);
        InstanceIdentifier<Nodes> id = InstanceIdentifier.create(Nodes.class);
        nodes = buildNodes();
        when(readTransaction.read(LogicalDatastoreType.OPERATIONAL, id)).thenReturn(checkedFuture);
        when(checkedFuture.get()).thenReturn(optional);
        when(optional.get()).thenReturn(nodes);
        collector = new TSDROpenflowCollector(this.dataBroker, collectorSPIService);
    }

    @After
    public void after() {
        for (Node node : nodes.getNode()) {
            InstanceIdentifier<Node> nodeID = InstanceIdentifier.create(Nodes.class).child(Node.class, node.getKey());
            collector.removeAllNodeBuilders(nodeID);
        }
        collector.close();
    }

    private Nodes buildNodes() {
        NodesBuilder nb = new NodesBuilder();
        List<Node> nodeList = new ArrayList<>(2);
        nodeList.add(buildNode("openflow:1"));
        nodeList.add(buildNode("openflow:2"));
        nb.setNode(nodeList);
        return nb.build();
    }

    private Node buildNode(String id) {
        NodeBuilder nb = new NodeBuilder();
        nb.setId(new NodeId(id));
        nb.setKey(new NodeKey(nb.getId()));
        List<NodeConnector> list = new ArrayList<>();
        list.add(buildNodeConnector(id + ":1"));
        list.add(buildNodeConnector(id + ":2"));
        nb.setNodeConnector(list);
        nb.addAugmentation(FlowCapableNode.class, buildFlowCapableNode());
        return nb.build();
    }

    private NodeConnector buildNodeConnector(String id) {
        NodeConnectorBuilder ncb = new NodeConnectorBuilder();
        ncb.setId(new NodeConnectorId(id));
        ncb.setKey(new NodeConnectorKey(ncb.getId()));
        ncb.addAugmentation(FlowCapableNodeConnectorStatisticsData.class,
                buildFlowCapableNodeConnectorStatisticsData());
        ncb.addAugmentation(FlowCapableNodeConnector.class, buildFlowCapableNodeConnector());
        return ncb.build();
    }

    private FlowCapableNode buildFlowCapableNode() {
        FlowCapableNodeBuilder builder = new FlowCapableNodeBuilder();
        List<Meter> meters = new ArrayList<>();
        meters.add(buildMeter(1));
        meters.add(buildMeter(2));
        builder.setMeter(meters);
        List<Table> tables = new ArrayList<>();
        tables.add(buildTable((short) 11));
        tables.add(buildTable((short) 22));
        builder.setTable(tables);
        List<Group> groups = new ArrayList<>();
        groups.add(buildGroup(1));
        groups.add(buildGroup(2));
        builder.setGroup(groups);
        return builder.build();
    }

    private Meter buildMeter(long id) {
        MeterBuilder builder = new MeterBuilder();
        builder.setMeterId(new MeterId(id));
        builder.setMeterName("Meter " + id);
        builder.addAugmentation(NodeMeterStatistics.class, buildNodeMeterStatistics());
        return builder.build();
    }

    private NodeMeterStatistics buildNodeMeterStatistics() {
        NodeMeterStatisticsBuilder builder = new NodeMeterStatisticsBuilder();
        builder.setMeterStatistics(buildMeterStatistics());
        return builder.build();
    }

    private MeterStatistics buildMeterStatistics() {
        MeterStatisticsBuilder builder = new MeterStatisticsBuilder();
        builder.setByteInCount(new Counter64(new BigInteger("1")));
        builder.setFlowCount(new Counter32(1L));
        builder.setPacketInCount(new Counter64(new BigInteger("2")));
        builder.setMeterId(new MeterId(1L));
        return builder.build();
    }

    private Table buildTable(short id) {
        TableBuilder builder = new TableBuilder();
        builder.setId(id);
        builder.setKey(new TableKey(builder.getId()));
        builder.addAugmentation(FlowTableStatisticsData.class, buildFlowTableStatisticsData());
        List<Flow> flows = new ArrayList<>();
        flows.add(buildFlow("1"));
        flows.add(buildFlow("2"));
        builder.setFlow(flows);
        return builder.build();
    }

    private FlowTableStatisticsData buildFlowTableStatisticsData() {
        FlowTableStatisticsDataBuilder builder = new FlowTableStatisticsDataBuilder();
        builder.setFlowTableStatistics(buildFlowTableStatistics());
        return builder.build();
    }

    private FlowTableStatistics buildFlowTableStatistics() {
        FlowTableStatisticsBuilder builder = new FlowTableStatisticsBuilder();
        builder.setActiveFlows(new Counter32(1L));
        builder.setPacketsLookedUp(new Counter64(new BigInteger("1")));
        builder.setPacketsMatched(new Counter64(new BigInteger("2")));
        return builder.build();
    }

    private Flow buildFlow(String id) {
        FlowBuilder builder = new FlowBuilder();
        builder.setId(new FlowId(id));
        builder.setKey(new FlowKey(builder.getId()));
        builder.addAugmentation(FlowStatisticsData.class, buildFlowStatisticsData());
        return builder.build();
    }

    private FlowStatisticsData buildFlowStatisticsData() {
        FlowStatisticsDataBuilder builder = new FlowStatisticsDataBuilder();
        builder.setFlowStatistics(buildFlowStatistics());
        return builder.build();
    }

    private FlowStatistics buildFlowStatistics() {
        FlowStatisticsBuilder builder = new FlowStatisticsBuilder();
        builder.setByteCount(new Counter64(new BigInteger("1")));
        builder.setPacketCount(new Counter64(new BigInteger("2")));
        return builder.build();
    }

    private Group buildGroup(long id) {
        GroupBuilder builder = new GroupBuilder();
        builder.setGroupId(new GroupId(id));
        builder.addAugmentation(NodeGroupStatistics.class, buildNodeGroupStatistics());
        return builder.build();
    }

    private NodeGroupStatistics buildNodeGroupStatistics() {
        NodeGroupStatisticsBuilder builder = new NodeGroupStatisticsBuilder();
        builder.setGroupStatistics(buildGroupStatistics());
        return builder.build();
    }

    private GroupStatistics buildGroupStatistics() {
        GroupStatisticsBuilder builder = new GroupStatisticsBuilder();
        builder.setByteCount(new Counter64(new BigInteger("1")));
        builder.setPacketCount(new Counter64(new BigInteger("2")));
        builder.setRefCount(new Counter32(1L));
        return builder.build();
    }

    private FlowCapableNodeConnectorStatisticsData buildFlowCapableNodeConnectorStatisticsData() {
        FlowCapableNodeConnectorStatisticsDataBuilder builder = new FlowCapableNodeConnectorStatisticsDataBuilder();
        builder.setFlowCapableNodeConnectorStatistics(buildFlowCapableNodeConnectorStatistics());
        return builder.build();
    }

    private FlowCapableNodeConnectorStatistics buildFlowCapableNodeConnectorStatistics() {
        FlowCapableNodeConnectorStatisticsBuilder builder = new FlowCapableNodeConnectorStatisticsBuilder();
        BytesBuilder bb = new BytesBuilder();
        bb.setReceived(new BigInteger("100"));
        bb.setTransmitted(new BigInteger("100"));
        builder.setBytes(bb.build());
        builder.setCollisionCount(new BigInteger("100"));
        PacketsBuilder pb = new PacketsBuilder();
        pb.setReceived(new BigInteger("100"));
        pb.setTransmitted(new BigInteger("100"));
        builder.setPackets(pb.build());
        builder.setReceiveCrcError(new BigInteger("100"));
        builder.setReceiveDrops(new BigInteger("100"));
        builder.setReceiveErrors(new BigInteger("100"));
        builder.setReceiveFrameError(new BigInteger("100"));
        builder.setReceiveOverRunError(new BigInteger("100"));
        builder.setTransmitDrops(new BigInteger("100"));
        builder.setTransmitErrors(new BigInteger("100"));
        return builder.build();
    }

    private FlowCapableNodeConnector buildFlowCapableNodeConnector() {
        FlowCapableNodeConnectorBuilder builder = new FlowCapableNodeConnectorBuilder();
        List<Queue> queues = new ArrayList<>();
        queues.add(buildQueue());
        builder.setQueue(queues);
        return builder.build();
    }

    private Queue buildQueue() {
        QueueBuilder builder = new QueueBuilder();
        builder.setQueueId(new QueueId(1L));
        builder.setKey(new QueueKey(builder.getQueueId()));
        builder.addAugmentation(FlowCapableNodeConnectorQueueStatisticsData.class,
                buildFlowCapableNodeConnectorQueueStatisticsData());
        return builder.build();
    }

    private FlowCapableNodeConnectorQueueStatisticsData buildFlowCapableNodeConnectorQueueStatisticsData() {
        FlowCapableNodeConnectorQueueStatisticsDataBuilder builder =
                new FlowCapableNodeConnectorQueueStatisticsDataBuilder();
        builder.setFlowCapableNodeConnectorQueueStatistics(buildFlowCapableNodeConnectorQueueStatistics());
        return builder.build();
    }

    private FlowCapableNodeConnectorQueueStatistics buildFlowCapableNodeConnectorQueueStatistics() {
        FlowCapableNodeConnectorQueueStatisticsBuilder builder = new FlowCapableNodeConnectorQueueStatisticsBuilder();
        builder.setTransmissionErrors(new Counter64(new BigInteger("1")));
        builder.setTransmittedBytes(new Counter64(new BigInteger("1")));
        builder.setTransmittedPackets(new Counter64(new BigInteger("1")));
        return builder.build();
    }

    @Test
    public void testCollectStatistics() {
        for (Node node : nodes.getNode()) {
            collector.collectStatistics(node);
        }
    }

    @Test
    public void testNodeMeterStatisticsChangeHandler() {
        NodeMeterStatisticsChangeHandler handler = new NodeMeterStatisticsChangeHandler(collector);
        Node node = nodes.getNode().get(0);
        InstanceIdentifier<Node> nodeID = InstanceIdentifier.create(Nodes.class).child(Node.class, node.getKey());
        InstanceIdentifier<NodeMeterStatistics> id = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, node.getKey()).augmentation(FlowCapableNode.class)
                .child(Meter.class, new MeterKey(new MeterId(1L))).augmentation(NodeMeterStatistics.class);
        handler.handleData(nodeID, id, buildNodeMeterStatistics());
    }

    @Test
    public void testNodeTableStatisticsChangeHandler() {
        NodeTableStatisticsChangeHandler handler = new NodeTableStatisticsChangeHandler(collector);
        Node node = nodes.getNode().get(0);
        InstanceIdentifier<Node> nodeID = InstanceIdentifier.create(Nodes.class).child(Node.class, node.getKey());
        InstanceIdentifier<FlowTableStatisticsData> id = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, node.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, buildTable((short) 11).getKey()).augmentation(FlowTableStatisticsData.class);
        handler.handleData(nodeID, id, buildFlowTableStatisticsData());
    }

    @Test
    public void testFlowStatisticsDataHandler() {
        FlowStatisticsDataHandler handler = new FlowStatisticsDataHandler(collector);
        Node node = nodes.getNode().get(0);
        InstanceIdentifier<Node> nodeID = InstanceIdentifier.create(Nodes.class).child(Node.class, node.getKey());
        InstanceIdentifier<FlowStatisticsData> id = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, node.getKey()).augmentation(FlowCapableNode.class)
                .child(Table.class, buildTable((short) 11).getKey()).child(Flow.class, buildFlow("1").getKey())
                .augmentation(FlowStatisticsData.class);
        handler.handleData(nodeID, id, buildFlowStatisticsData());
    }

    @Test
    public void testNodeGroupStatisticsChangeHandler() {
        NodeGroupStatisticsChangeHandler handler = new NodeGroupStatisticsChangeHandler(collector);
        Node node = nodes.getNode().get(0);
        InstanceIdentifier<Node> nodeID = InstanceIdentifier.create(Nodes.class).child(Node.class, node.getKey());
        InstanceIdentifier<NodeGroupStatistics> id = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, node.getKey()).augmentation(FlowCapableNode.class)
                .child(Group.class, buildGroup(1).getKey()).augmentation(NodeGroupStatistics.class);
        handler.handleData(nodeID, id, buildNodeGroupStatistics());
    }

    /*
     * @Test public void testNodeConnectorStatisticsChangeHandler(){
     * NodeConnectorStatisticsChangeHandler handler = new
     * NodeConnectorStatisticsChangeHandler(collector); Node node =
     * nodes.getNode().get(0); InstanceIdentifier<Node> nodeID =
     * InstanceIdentifier.create( Nodes.class).child(Node.class, node.getKey());
     * InstanceIdentifier<FlowCapableNodeConnectorStatisticsData> id =
     * InstanceIdentifier .create(Nodes.class) .child(Node.class, node.getKey())
     * .child(NodeConnector.class,
     * buildNodeConnector(node.getId().getValue()+":1").getKey()) .augmentation(
     * FlowCapableNodeConnectorStatisticsData.class); handler.handleData(nodeID,
     * id, buildFlowCapableNodeConnectorStatisticsData()); }
     */
    @Test
    public void testFlowCapableNodeConnectorQueueStatisticsDataHandler() {
        FlowCapableNodeConnectorQueueStatisticsDataHandler handler =
                new FlowCapableNodeConnectorQueueStatisticsDataHandler(collector);
        Node node = nodes.getNode().get(0);
        InstanceIdentifier<Node> nodeID = InstanceIdentifier.create(Nodes.class).child(Node.class, node.getKey());
        InstanceIdentifier<FlowCapableNodeConnectorQueueStatisticsData> id = InstanceIdentifier.create(Nodes.class)
                .child(Node.class, node.getKey())
                .child(NodeConnector.class, buildNodeConnector(node.getId().getValue() + ":1").getKey())
                .augmentation(FlowCapableNodeConnector.class).child(Queue.class, buildQueue().getKey())
                .augmentation(FlowCapableNodeConnectorQueueStatisticsData.class);
        handler.handleData(nodeID, id, buildFlowCapableNodeConnectorQueueStatisticsData());
    }
}
