package org.opendaylight.tsdr.datacollection;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.tsdr.datacollection.handlers.FlowCapableNodeConnectorQueueStatisticsDataHandler;
import org.opendaylight.tsdr.datacollection.handlers.FlowStatisticsDataHandler;
import org.opendaylight.tsdr.datacollection.handlers.NodeConnectorStatisticsChangeHandler;
import org.opendaylight.tsdr.datacollection.handlers.NodeGroupStatisticsChangeHandler;
import org.opendaylight.tsdr.datacollection.handlers.NodeMeterStatisticsChangeHandler;
import org.opendaylight.tsdr.datacollection.handlers.NodeTableStatisticsChangeHandler;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Counter32;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Counter64;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.FlowCapableNodeConnectorStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.FlowCapableNodeConnectorStatisticsDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.flow.capable.node.connector.statistics.FlowCapableNodeConnectorStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.flow.capable.node.connector.statistics.FlowCapableNodeConnectorStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.FlowCapableNodeConnectorQueueStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.FlowCapableNodeConnectorQueueStatisticsDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.flow.capable.node.connector.queue.statistics.FlowCapableNodeConnectorQueueStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.flow.capable.node.connector.queue.statistics.FlowCapableNodeConnectorQueueStatisticsBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

public class DataCollectionTest {
    private DataBroker dataBroker = null;
    private RpcProviderRegistry rpcRegistry = null;
    private ReadOnlyTransaction readTransaction = null;
    private Nodes nodes = null;
    private CheckedFuture<Optional<Nodes>, ReadFailedException> checkedFuture = mock(CheckedFuture.class);
    private Optional<Nodes> optional = mock(Optional.class);
    private TSDRDOMCollector collector = null;
    @Before
    public void before(){
        dataBroker = mock(DataBroker.class);
        rpcRegistry = mock(RpcProviderRegistry.class);
        readTransaction = mock(ReadOnlyTransaction.class);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readTransaction);
        InstanceIdentifier<Nodes> id = InstanceIdentifier.create(Nodes.class);
        nodes = buildNodes();
        when(readTransaction.read(LogicalDatastoreType.OPERATIONAL, id)).thenReturn(checkedFuture);
        try {
            when(checkedFuture.get()).thenReturn(optional);
        } catch (InterruptedException | ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        when(optional.get()).thenReturn(nodes);
        collector = new TSDRDOMCollector(this.dataBroker, rpcRegistry);
    }
    @After
    public void after(){
        for(Node node:nodes.getNode()){
            InstanceIdentifier<Node> nodeID = InstanceIdentifier.create(
                    Nodes.class).child(Node.class, node.getKey());
            collector.removeAllNodeBuilders(nodeID);
        }
        collector.shutdown();
    }
    private Nodes buildNodes(){
        NodesBuilder nb = new NodesBuilder();
        List<Node> nodeList = new ArrayList<Node>(2);
        nodeList.add(buildNode("openflow:1"));
        nodeList.add(buildNode("openflow:2"));
        nb.setNode(nodeList);
        return nb.build();
    }
    private Node buildNode(String id){
        NodeBuilder nb = new NodeBuilder();
        nb.setId(new NodeId(id));
        nb.setKey(new NodeKey(nb.getId()));
        List<NodeConnector> cList = new ArrayList<NodeConnector>();
        cList.add(buildNodeConnector(id+":1"));
        cList.add(buildNodeConnector(id+":2"));
        nb.setNodeConnector(cList);
        nb.addAugmentation(FlowCapableNode.class, buildFlowCapableNode());
        return nb.build();
    }
    private NodeConnector buildNodeConnector(String id){
        NodeConnectorBuilder ncb = new NodeConnectorBuilder();
        ncb.setId(new NodeConnectorId(id));
        ncb.setKey(new NodeConnectorKey(ncb.getId()));
        ncb.addAugmentation(FlowCapableNodeConnectorStatisticsData.class, buildFlowCapableNodeConnectorStatisticsData());
        ncb.addAugmentation(FlowCapableNodeConnector.class, buildFlowCapableNodeConnector());
        return ncb.build();
    }
    private FlowCapableNode buildFlowCapableNode(){
        FlowCapableNodeBuilder b = new FlowCapableNodeBuilder();
        List<Meter> m = new ArrayList<>();
        m.add(buildMeter(1));
        m.add(buildMeter(2));
        b.setMeter(m);
        List<Table> t = new ArrayList<>();
        t.add(buildTable((short)11));
        t.add(buildTable((short)22));
        b.setTable(t);
        List<Group> g = new ArrayList<>();
        g.add(buildGroup(1));
        g.add(buildGroup(2));
        b.setGroup(g);
        return b.build();
    }
    private Meter buildMeter(long id){
       MeterBuilder b = new MeterBuilder();
       b.setMeterId(new MeterId(id));
       b.setMeterName("Meter "+id);
       b.addAugmentation(NodeMeterStatistics.class, buildNodeMeterStatistics());
       return b.build();
    }
    private NodeMeterStatistics buildNodeMeterStatistics(){
        NodeMeterStatisticsBuilder b = new NodeMeterStatisticsBuilder();
        b.setMeterStatistics(buildMeterStatistics());
        return b.build();
    }
    private MeterStatistics buildMeterStatistics(){
        MeterStatisticsBuilder b = new MeterStatisticsBuilder();
        b.setByteInCount(new Counter64(new BigInteger("1")));
        b.setFlowCount(new Counter32(1l));
        b.setPacketInCount(new Counter64(new BigInteger("2")));
        b.setMeterId(new MeterId(1l));
        return b.build();
    }
    private Table buildTable(short s){
        TableBuilder b = new TableBuilder();
        b.setId(s);
        b.setKey(new TableKey(b.getId()));
        b.addAugmentation(FlowTableStatisticsData.class, buildFlowTableStatisticsData());
        List<Flow> f = new ArrayList<>();
        f.add(buildFlow("1"));
        f.add(buildFlow("2"));
        b.setFlow(f);
        return b.build();
    }
    private FlowTableStatisticsData buildFlowTableStatisticsData(){
        FlowTableStatisticsDataBuilder b = new FlowTableStatisticsDataBuilder();
        b.setFlowTableStatistics(buildFlowTableStatistics());
        return b.build();
    }
    private FlowTableStatistics buildFlowTableStatistics(){
        FlowTableStatisticsBuilder b = new FlowTableStatisticsBuilder();
        b.setActiveFlows(new Counter32(1l));
        b.setPacketsLookedUp(new Counter64(new BigInteger("1")));
        b.setPacketsMatched(new Counter64(new BigInteger("2")));
        return b.build();
    }
    private Flow buildFlow(String id){
        FlowBuilder b = new FlowBuilder();
        b.setId(new FlowId(id));
        b.setKey(new FlowKey(b.getId()));
        b.addAugmentation(FlowStatisticsData.class, buildFlowStatisticsData());
        return b.build();
    }
    private FlowStatisticsData buildFlowStatisticsData(){
        FlowStatisticsDataBuilder b = new FlowStatisticsDataBuilder();
        b.setFlowStatistics(buildFlowStatistics());
        return b.build();
    }
    private FlowStatistics buildFlowStatistics(){
        FlowStatisticsBuilder b = new FlowStatisticsBuilder();
        b.setByteCount(new Counter64(new BigInteger("1")));
        b.setPacketCount(new Counter64(new BigInteger("2")));
        return b.build();
    }
    private Group buildGroup(long id){
        GroupBuilder b = new GroupBuilder();
        b.setGroupId(new GroupId(id));
        b.addAugmentation(NodeGroupStatistics.class, buildNodeGroupStatistics());
        return b.build();
    }
    private NodeGroupStatistics buildNodeGroupStatistics(){
        NodeGroupStatisticsBuilder b = new NodeGroupStatisticsBuilder();
        b.setGroupStatistics(buildGroupStatistics());
        return b.build();
    }
    private GroupStatistics buildGroupStatistics(){
        GroupStatisticsBuilder b = new GroupStatisticsBuilder();
        b.setByteCount(new Counter64(new BigInteger("1")));
        b.setPacketCount(new Counter64(new BigInteger("2")));
        b.setRefCount(new Counter32(1l));
        return b.build();
    }
    private FlowCapableNodeConnectorStatisticsData buildFlowCapableNodeConnectorStatisticsData(){
        FlowCapableNodeConnectorStatisticsDataBuilder b = new FlowCapableNodeConnectorStatisticsDataBuilder();
        b.setFlowCapableNodeConnectorStatistics(buildFlowCapableNodeConnectorStatistics());
        return b.build();
    }
    private FlowCapableNodeConnectorStatistics buildFlowCapableNodeConnectorStatistics(){
        FlowCapableNodeConnectorStatisticsBuilder b = new FlowCapableNodeConnectorStatisticsBuilder();
        BytesBuilder bb = new BytesBuilder();
        bb.setReceived(new BigInteger("100"));
        bb.setTransmitted(new BigInteger("100"));
        b.setBytes(bb.build());
        b.setCollisionCount(new BigInteger("100"));
        PacketsBuilder pb = new PacketsBuilder();
        pb.setReceived(new BigInteger("100"));
        pb.setTransmitted(new BigInteger("100"));
        b.setPackets(pb.build());
        b.setReceiveCrcError(new BigInteger("100"));
        b.setReceiveDrops(new BigInteger("100"));
        b.setReceiveErrors(new BigInteger("100"));
        b.setReceiveFrameError(new BigInteger("100"));
        b.setReceiveOverRunError(new BigInteger("100"));
        b.setTransmitDrops(new BigInteger("100"));
        b.setTransmitErrors(new BigInteger("100"));
        return b.build();
    }
    private FlowCapableNodeConnector buildFlowCapableNodeConnector(){
        FlowCapableNodeConnectorBuilder b = new FlowCapableNodeConnectorBuilder();
        List<Queue> q = new ArrayList<>();
        q.add(buildQueue());
        b.setQueue(q);
        return b.build();
    }
    private Queue buildQueue(){
        QueueBuilder b = new QueueBuilder();
        b.setQueueId(new QueueId(1l));
        b.setKey(new QueueKey(b.getQueueId()));
        b.addAugmentation(FlowCapableNodeConnectorQueueStatisticsData.class, buildFlowCapableNodeConnectorQueueStatisticsData());
        return b.build();
    }
    private FlowCapableNodeConnectorQueueStatisticsData buildFlowCapableNodeConnectorQueueStatisticsData(){
        FlowCapableNodeConnectorQueueStatisticsDataBuilder b = new FlowCapableNodeConnectorQueueStatisticsDataBuilder();
        b.setFlowCapableNodeConnectorQueueStatistics(buildFlowCapableNodeConnectorQueueStatistics());
        return b.build();
    }
    private FlowCapableNodeConnectorQueueStatistics buildFlowCapableNodeConnectorQueueStatistics(){
        FlowCapableNodeConnectorQueueStatisticsBuilder b = new FlowCapableNodeConnectorQueueStatisticsBuilder();
        b.setTransmissionErrors(new Counter64(new BigInteger("1")));
        b.setTransmittedBytes(new Counter64(new BigInteger("1")));
        b.setTransmittedPackets(new Counter64(new BigInteger("1")));
        return b.build();
    }
    @Test
    public void testCollectStatistics(){
        for(Node node:nodes.getNode()){
            collector.collectStatistics(node);
        }
    }
    @Test
    public void testNodeMeterStatisticsChangeHandler(){
        NodeMeterStatisticsChangeHandler handler = new NodeMeterStatisticsChangeHandler(collector);
        Node node = nodes.getNode().get(0);
        InstanceIdentifier<Node> nodeID = InstanceIdentifier.create(
                Nodes.class).child(Node.class, node.getKey());
        InstanceIdentifier<NodeMeterStatistics> id = InstanceIdentifier
                .create(Nodes.class)
                .child(Node.class, node.getKey())
                .augmentation(FlowCapableNode.class)
                .child(Meter.class, new MeterKey(new MeterId(1l)))
                .augmentation(NodeMeterStatistics.class);
        handler.handleData(nodeID, id, buildNodeMeterStatistics());
    }
    @Test
    public void testNodeTableStatisticsChangeHandler(){
        NodeTableStatisticsChangeHandler handler = new NodeTableStatisticsChangeHandler(collector);
        Node node = nodes.getNode().get(0);
        InstanceIdentifier<Node> nodeID = InstanceIdentifier.create(
                Nodes.class).child(Node.class, node.getKey());
        InstanceIdentifier<FlowTableStatisticsData> id = InstanceIdentifier
                .create(Nodes.class)
                .child(Node.class, node.getKey())
                .augmentation(FlowCapableNode.class)
                .child(Table.class, buildTable((short)11).getKey())
                .augmentation(
                        FlowTableStatisticsData.class);
        handler.handleData(nodeID, id,buildFlowTableStatisticsData());
    }
    @Test
    public void testFlowStatisticsDataHandler(){
        FlowStatisticsDataHandler handler = new FlowStatisticsDataHandler(collector);
        Node node = nodes.getNode().get(0);
        InstanceIdentifier<Node> nodeID = InstanceIdentifier.create(
                Nodes.class).child(Node.class, node.getKey());
        InstanceIdentifier<FlowStatisticsData> id = InstanceIdentifier
                .create(Nodes.class)
                .child(Node.class,
                        node.getKey())
                .augmentation(
                        FlowCapableNode.class)
                .child(Table.class, buildTable((short)11).getKey())
                .child(Flow.class,
                        buildFlow("1").getKey())
                .augmentation(
                        FlowStatisticsData.class);
        handler.handleData(nodeID, id, buildFlowStatisticsData());
    }
    @Test
    public void testNodeGroupStatisticsChangeHandler(){
        NodeGroupStatisticsChangeHandler handler = new NodeGroupStatisticsChangeHandler(collector);
        Node node = nodes.getNode().get(0);
        InstanceIdentifier<Node> nodeID = InstanceIdentifier.create(
                Nodes.class).child(Node.class, node.getKey());
        InstanceIdentifier<NodeGroupStatistics> id = InstanceIdentifier
                .create(Nodes.class)
                .child(Node.class, node.getKey())
                .augmentation(FlowCapableNode.class)
                .child(Group.class, buildGroup(1).getKey())
                .augmentation(NodeGroupStatistics.class);
        handler.handleData(nodeID, id, buildNodeGroupStatistics());
    }
    @Test
    public void testNodeConnectorStatisticsChangeHandler(){
        NodeConnectorStatisticsChangeHandler handler = new NodeConnectorStatisticsChangeHandler(collector);
        Node node = nodes.getNode().get(0);
        InstanceIdentifier<Node> nodeID = InstanceIdentifier.create(
                Nodes.class).child(Node.class, node.getKey());
        InstanceIdentifier<FlowCapableNodeConnectorStatisticsData> id = InstanceIdentifier
                .create(Nodes.class)
                .child(Node.class, node.getKey())
                .child(NodeConnector.class, buildNodeConnector(node.getId().getValue()+":1").getKey())
                .augmentation(
                        FlowCapableNodeConnectorStatisticsData.class);
        handler.handleData(nodeID, id, buildFlowCapableNodeConnectorStatisticsData());
    }
    @Test
    public void testFlowCapableNodeConnectorQueueStatisticsDataHandler(){
        FlowCapableNodeConnectorQueueStatisticsDataHandler handler = new FlowCapableNodeConnectorQueueStatisticsDataHandler(collector);
        Node node = nodes.getNode().get(0);
        InstanceIdentifier<Node> nodeID = InstanceIdentifier.create(
                Nodes.class).child(Node.class, node.getKey());
        InstanceIdentifier<FlowCapableNodeConnectorQueueStatisticsData> id = InstanceIdentifier
                .create(Nodes.class)
                .child(Node.class, node.getKey())
                .child(NodeConnector.class, buildNodeConnector(node.getId().getValue()+":1").getKey())
                .augmentation(
                        FlowCapableNodeConnector.class)
                .child(Queue.class, buildQueue().getKey())
                .augmentation(
                        FlowCapableNodeConnectorQueueStatisticsData.class);
        handler.handleData(nodeID, id, buildFlowCapableNodeConnectorQueueStatisticsData());
    }
}
