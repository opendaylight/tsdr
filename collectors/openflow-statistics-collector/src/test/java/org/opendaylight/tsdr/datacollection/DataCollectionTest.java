/*
 * Copyright (c) 2015, 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.datacollection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Uninterruptibles;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.test.ConstantSchemaAbstractDataBrokerTest;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.tsdr.osc.TSDROpenflowCollector;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Counter32;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Counter64;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.Meter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.MeterBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRMetricRecordInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.openflow.statistics.collector.rev150820.SetPollingIntervalInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.openflow.statistics.collector.rev150820.SetPollingIntervalOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.openflow.statistics.collector.rev150820.TSDROSCConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.openflow.statistics.collector.rev150820.TSDROSCConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.FlowCapableNodeConnectorStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.FlowCapableNodeConnectorStatisticsDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.flow.capable.node.connector.statistics.FlowCapableNodeConnectorStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.flow.capable.node.connector.statistics.FlowCapableNodeConnectorStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.FlowCapableNodeConnectorQueueStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.FlowCapableNodeConnectorQueueStatisticsDataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.flow.capable.node.connector.queue.statistics.FlowCapableNodeConnectorQueueStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.flow.capable.node.connector.queue.statistics.FlowCapableNodeConnectorQueueStatisticsBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

public class DataCollectionTest extends ConstantSchemaAbstractDataBrokerTest {
    private final TsdrCollectorSpiService collectorSPIService = mock(TsdrCollectorSpiService.class);
    private TSDROpenflowCollector collector;

    @Before
    public void before() throws InterruptedException, ExecutionException {
        doReturn(RpcResultBuilder.<Void>success().buildFuture()).when(collectorSPIService)
                .insertTSDRMetricRecord(any());

        collector = new TSDROpenflowCollector(getDataBroker(), collectorSPIService,
                new TSDROSCConfigBuilder().setPollingInterval(500L).setRecordStoreBatchSize(100L).build());
    }

    @Test
    public void testPolling() throws InterruptedException, ExecutionException, TimeoutException {
        WriteTransaction writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.put(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(Nodes.class), buildNodes());
        writeTx.submit().get(5, TimeUnit.SECONDS);

        Collection<TSDRMetricRecord> metricRecords = Collections.synchronizedList(new ArrayList<>());
        AtomicReference<CountDownLatch> storeMetricsLatchRef = new AtomicReference<>(new CountDownLatch(2));
        AtomicBoolean storeMetricsContinue = new AtomicBoolean();
        doAnswer(invocation -> {
            metricRecords.addAll(invocation.getArgumentAt(0, InsertTSDRMetricRecordInput.class).getTSDRMetricRecord());
            CountDownLatch storeMetricsLatch = storeMetricsLatchRef.get();
            storeMetricsLatch.countDown();
            if (storeMetricsLatch.getCount() == 0) {
                synchronized (storeMetricsContinue) {
                    while (!storeMetricsContinue.get()) {
                        storeMetricsContinue.wait();
                    }

                    storeMetricsContinue.set(false);
                }
            }
            return RpcResultBuilder.<Void>success().buildFuture();
        }).when(collectorSPIService).insertTSDRMetricRecord(any());

        collector.init();

        // Wait for first poll

        assertTrue("Timed out waiting for metrics to be stored",
                Uninterruptibles.awaitUninterruptibly(storeMetricsLatchRef.get(), 5, TimeUnit.SECONDS));
        storeMetricsLatchRef.set(new CountDownLatch(2));

        verify(metricRecords, "openflow:1", "openflow:2");
        metricRecords.clear();

        synchronized (storeMetricsContinue) {
            storeMetricsContinue.set(true);
            storeMetricsContinue.notifyAll();
        }

        // Wait for second poll

        assertTrue("Timed out waiting for metrics to be stored",
                Uninterruptibles.awaitUninterruptibly(storeMetricsLatchRef.get(), 5, TimeUnit.SECONDS));
        storeMetricsLatchRef.set(new CountDownLatch(1));

        verify(metricRecords, "openflow:1", "openflow:2");
        metricRecords.clear();

        // Delete the openflow:1 node

        writeTx = getDataBroker().newWriteOnlyTransaction();
        writeTx.delete(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.create(Nodes.class)
                .child(Node.class, new NodeKey(new NodeId("openflow:1"))));
        writeTx.submit().get(5, TimeUnit.SECONDS);

        synchronized (storeMetricsContinue) {
            storeMetricsContinue.set(true);
            storeMetricsContinue.notifyAll();
        }

        // Wait for third  poll (sans openflow:1)

        assertTrue("Timed out waiting for metrics to be stored",
                Uninterruptibles.awaitUninterruptibly(storeMetricsLatchRef.get(), 5, TimeUnit.SECONDS));
        storeMetricsLatchRef.set(new CountDownLatch(1));

        verify(metricRecords, "openflow:2");

        reset(collectorSPIService);

        collector.close();

        synchronized (storeMetricsContinue) {
            storeMetricsContinue.set(true);
            storeMetricsContinue.notifyAll();
        }

        Mockito.verify(collectorSPIService, after(1000).never()).insertTSDRMetricRecord(any());
    }

    @Test
    public void testSetPollingInterval() throws InterruptedException, ExecutionException, TimeoutException {
        RpcResult<SetPollingIntervalOutput> rpcResult = collector.setPollingInterval(
                new SetPollingIntervalInputBuilder().setInterval(1000L).build()).get();
        assertTrue("setPollingInterval failed: " + rpcResult, rpcResult.isSuccessful());

        TSDROSCConfig config = getDataBroker().newReadOnlyTransaction().read(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifier.create(TSDROSCConfig.class)).get(5, TimeUnit.SECONDS).get();
        assertEquals("Polling interval", Long.valueOf(1000L), config.getPollingInterval());
    }

    private void verify(Collection<TSDRMetricRecord> metricRecords, String... nodeIds) {
        Set<String> nodeIdSet = ImmutableSet.copyOf(Arrays.asList(nodeIds));
        Map<String, TSDRMetricRecord> map = new HashMap<>();
        for (TSDRMetricRecord rec: metricRecords) {
            assertTrue("Unexpected node Id " + rec.getNodeID(), nodeIdSet.contains(rec.getNodeID()));
            StringBuilder builder = new StringBuilder(rec.getMetricName());
            for (RecordKeys key: rec.getRecordKeys()) {
                builder.append('.').append(key.getKeyName()).append(':').append(key.getKeyValue());
            }

            map.put(builder.toString(), rec);
        }

        for (String nodeId: nodeIds) {
            verifyNodeQueueStats(map, nodeId);
            verifyNodeFlowStats(map, nodeId);
            verifyNodeConnectorStats(map, nodeId);
            verifyNodeGroupStats(map, nodeId);
            verifyNodeMeterStats(map, nodeId);
            verifyNodeTableStats(map, nodeId);
        }
    }

    private void verifyNodeTableStats(Map<String, TSDRMetricRecord> map, String nodeId) {
        verifyTableStats(map, "Node:" + nodeId + ".Table:11");
        verifyTableStats(map, "Node:" + nodeId + ".Table:22");
    }

    private void verifyTableStats(Map<String, TSDRMetricRecord> map, String tableId) {
        verifyStat(map, "ActiveFlows." + tableId, DataCategory.FLOWTABLESTATS, BigDecimal.valueOf(10));
        verifyStat(map, "PacketLookup." + tableId, DataCategory.FLOWTABLESTATS, BigDecimal.valueOf(1));
        verifyStat(map, "PacketMatch." + tableId, DataCategory.FLOWTABLESTATS, BigDecimal.valueOf(2));
    }

    private void verifyNodeMeterStats(Map<String, TSDRMetricRecord> map, String nodeId) {
        verifyMeterStats(map, "Node:" + nodeId + ".Meter:1");
        verifyMeterStats(map, "Node:" + nodeId + ".Meter:2");
    }

    private void verifyMeterStats(Map<String, TSDRMetricRecord> map, String meterId) {
        verifyStat(map, "ByteInCount." + meterId, DataCategory.FLOWMETERSTATS, BigDecimal.valueOf(1));
        verifyStat(map, "FlowCount." + meterId, DataCategory.FLOWMETERSTATS, BigDecimal.valueOf(10));
        verifyStat(map, "PacketInCount." + meterId, DataCategory.FLOWMETERSTATS, BigDecimal.valueOf(2));
    }

    private void verifyNodeGroupStats(Map<String, TSDRMetricRecord> map, String nodeId) {
        verifyGroupStats(map, "Node:" + nodeId + ".Group:1");
        verifyGroupStats(map, "Node:" + nodeId + ".Group:2");
    }

    private void verifyGroupStats(Map<String, TSDRMetricRecord> map, String groupKey) {
        verifyStat(map, "RefCount." + groupKey, DataCategory.FLOWGROUPSTATS, BigDecimal.valueOf(10));
        verifyStat(map, "ByteCount." + groupKey, DataCategory.FLOWGROUPSTATS, BigDecimal.valueOf(1));
        verifyStat(map, "PacketCount." + groupKey, DataCategory.FLOWGROUPSTATS, BigDecimal.valueOf(2));
    }

    private static void verifyNodeConnectorStats(Map<String, TSDRMetricRecord> map, String nodeId) {
        verifyConnectorStats(map, nodeConnectorKey(nodeId, "1"));
        verifyConnectorStats(map, nodeConnectorKey(nodeId, "2"));
    }

    private static void verifyConnectorStats(Map<String, TSDRMetricRecord> map, String connectorId) {
        verifyStat(map, "TransmitDrops." + connectorId, DataCategory.PORTSTATS, BigDecimal.valueOf(110));
        verifyStat(map, "ReceiveDrops." + connectorId, DataCategory.PORTSTATS, BigDecimal.valueOf(106));
        verifyStat(map, "ReceiveCrcError." + connectorId, DataCategory.PORTSTATS, BigDecimal.valueOf(105));
        verifyStat(map, "ReceiveFrameError." + connectorId, DataCategory.PORTSTATS, BigDecimal.valueOf(108));
        verifyStat(map, "ReceiveOverRunError." + connectorId, DataCategory.PORTSTATS, BigDecimal.valueOf(109));
        verifyStat(map, "TransmitErrors." + connectorId, DataCategory.PORTSTATS, BigDecimal.valueOf(111));
        verifyStat(map, "CollisionCount." + connectorId, DataCategory.PORTSTATS, BigDecimal.valueOf(102));
        verifyStat(map, "ReceiveErrors." + connectorId, DataCategory.PORTSTATS, BigDecimal.valueOf(107));
        verifyStat(map, "TransmittedBytes." + connectorId, DataCategory.PORTSTATS, BigDecimal.valueOf(101));
        verifyStat(map, "ReceivedBytes." + connectorId, DataCategory.PORTSTATS, BigDecimal.valueOf(100));
        verifyStat(map, "TransmittedPackets." + connectorId, DataCategory.PORTSTATS, BigDecimal.valueOf(104));
        verifyStat(map, "ReceivedPackets." + connectorId, DataCategory.PORTSTATS, BigDecimal.valueOf(103));
    }

    private static void verifyNodeFlowStats(Map<String, TSDRMetricRecord> map, String nodeId) {
        verifyFlowStats(map, flowKey(nodeId, "11", "1"));
        verifyFlowStats(map, flowKey(nodeId, "11", "2"));
        verifyFlowStats(map, flowKey(nodeId, "22", "1"));
        verifyFlowStats(map, flowKey(nodeId, "22", "2"));
    }

    private static void verifyFlowStats(Map<String, TSDRMetricRecord> map, String flowId) {
        verifyStat(map, "ByteCount." + flowId, DataCategory.FLOWSTATS, BigDecimal.valueOf(1));
        verifyStat(map, "PacketCount." + flowId, DataCategory.FLOWSTATS, BigDecimal.valueOf(2));
    }

    private static void verifyNodeQueueStats(Map<String, TSDRMetricRecord> map, String nodeId) {
        verifyQueueStats(map, nodeConnectorKey(nodeId, "1") + ".Queue:1");
        verifyQueueStats(map, nodeConnectorKey(nodeId, "2") + ".Queue:1");
    }

    private static void verifyQueueStats(Map<String, TSDRMetricRecord> map, String queueId) {
        verifyStat(map, "TransmissionErrors." + queueId, DataCategory.QUEUESTATS, BigDecimal.valueOf(1));
        verifyStat(map, "TransmittedBytes." + queueId, DataCategory.QUEUESTATS, BigDecimal.valueOf(2));
        verifyStat(map, "TransmittedPackets." + queueId, DataCategory.QUEUESTATS, BigDecimal.valueOf(3));
    }

    private static void verifyStat(Map<String, TSDRMetricRecord> map, String key, DataCategory category,
            BigDecimal value) {
        TSDRMetricRecord record = map.get(key);
        assertNotNull("TSDRMetricRecord not found for " + key, record);
        assertEquals("Metric DataCategory for " + key, category, record.getTSDRDataCategory());
        assertEquals("Metric value for " + key, value, record.getMetricValue());
    }

    private static String nodeConnectorKey(String nodeId, String id) {
        return "Node:" + nodeId + ".NodeConnector:" + nodeId + ":" + id;
    }

    private static String flowKey(String nodeId, String tableId, String flowId) {
        return "Node:" + nodeId + ".Table:" + tableId + ".Flow:" + flowId;
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
        builder.setFlowCount(new Counter32(10L));
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
        builder.setActiveFlows(new Counter32(10L));
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
        builder.setRefCount(new Counter32(10L));
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
        bb.setTransmitted(new BigInteger("101"));
        builder.setBytes(bb.build());
        builder.setCollisionCount(new BigInteger("102"));

        PacketsBuilder pb = new PacketsBuilder();
        pb.setReceived(new BigInteger("103"));
        pb.setTransmitted(new BigInteger("104"));
        builder.setPackets(pb.build());
        builder.setReceiveCrcError(new BigInteger("105"));
        builder.setReceiveDrops(new BigInteger("106"));
        builder.setReceiveErrors(new BigInteger("107"));
        builder.setReceiveFrameError(new BigInteger("108"));
        builder.setReceiveOverRunError(new BigInteger("109"));
        builder.setTransmitDrops(new BigInteger("110"));
        builder.setTransmitErrors(new BigInteger("111"));
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
        builder.setTransmittedBytes(new Counter64(new BigInteger("2")));
        builder.setTransmittedPackets(new Counter64(new BigInteger("3")));
        return builder.build();
    }
}
