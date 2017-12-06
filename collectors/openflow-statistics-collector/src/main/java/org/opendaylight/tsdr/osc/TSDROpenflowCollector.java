/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.osc;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.concurrent.GuardedBy;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.tsdr.collector.spi.RPCFutures;
import org.opendaylight.tsdr.osc.handlers.FlowCapableNodeConnectorQueueStatisticsDataHandler;
import org.opendaylight.tsdr.osc.handlers.FlowStatisticsDataHandler;
import org.opendaylight.tsdr.osc.handlers.NodeConnectorStatisticsChangeHandler;
import org.opendaylight.tsdr.osc.handlers.NodeGroupStatisticsChangeHandler;
import org.opendaylight.tsdr.osc.handlers.NodeMeterStatisticsChangeHandler;
import org.opendaylight.tsdr.osc.handlers.NodeTableStatisticsChangeHandler;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.meters.Meter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.FlowTableStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.Queue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.meter.statistics.rev131111.NodeMeterStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRMetricRecordInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRMetricRecordInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.openflow.statistics.collector.rev150820.SetPollingIntervalInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.openflow.statistics.collector.rev150820.TSDROSCConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.openflow.statistics.collector.rev150820.TSDROSCConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.openflow.statistics.collector.rev150820.TsdrOpenflowStatisticsCollectorService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.FlowCapableNodeConnectorStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.FlowCapableNodeConnectorQueueStatisticsData;
import org.opendaylight.yangtools.util.concurrent.SpecialExecutors;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The TSDRDOMCollector is the place to collect metric data that exist in the
 * Inventory model and its augmentations. It registers on specific locations in
 * the data broker and every 30 seconds persists the data to the TSDR data
 * storage.
 *
 * @author Sharon Aicler(saichler@gmail.com)
 * @author Thomas Pantelis
 */
public class TSDROpenflowCollector implements TsdrOpenflowStatisticsCollectorService, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(TSDROpenflowCollector.class);

    // A reference to the data broker
    private final DataBroker dataBroker;

    // A map representing the instance identifier of the metric collection to the TSDRMetricRecordBuilderContainers,
    // which is a collection of metric builders that serves as a cache so we won't need to instantiate and set all the
    // static meta data of the metric when ever we want to store.
    @GuardedBy("builderContainers")
    private final Map<InstanceIdentifier<?>, TSDRMetricRecordBuilderContainer> builderContainers = new HashMap<>();

    @GuardedBy("builderContainers")
    private final Map<InstanceIdentifier<Node>, Set<InstanceIdentifier<?>>> nodeID2SubIDs = new HashMap<>();

    // collectors
    private final Map<Class<? extends DataObject>, TSDRBaseDataHandler<?>> handlers = new ConcurrentHashMap<>();
    private final TsdrCollectorSpiService collectorSPIService;

    private final ExecutorService storeMetricsExecutor = SpecialExecutors.newBoundedSingleThreadExecutor(
            1000, "TSDR Openflow Storing Thread");

    private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(
            1, new ThreadFactoryBuilder().setNameFormat("TSDR Openflow Poller-%d").build());

    // Set of nodes already registered on
    private final Set<InstanceIdentifier<Node>> knownNodes = ConcurrentHashMap.newKeySet();

    private final TSDROSCConfig config;

    public TSDROpenflowCollector(DataBroker dataBroker, TsdrCollectorSpiService collectorSPIService,
            TSDROSCConfig config) {
        this.dataBroker = dataBroker;
        this.collectorSPIService = collectorSPIService;
        this.config = config;
    }

    public void init() {
        // initialize handlers
        handlers.put(FlowCapableNodeConnectorQueueStatisticsData.class,
                new FlowCapableNodeConnectorQueueStatisticsDataHandler(this));
        handlers.put(FlowStatisticsData.class, new FlowStatisticsDataHandler(this));
        handlers.put(FlowCapableNodeConnectorStatisticsData.class,
                new NodeConnectorStatisticsChangeHandler(this));
        handlers.put(NodeGroupStatistics.class,
                new NodeGroupStatisticsChangeHandler(this));
        handlers.put(FlowTableStatisticsData.class,
                new NodeTableStatisticsChangeHandler(this));
        handlers.put(NodeMeterStatistics.class,
                new NodeMeterStatisticsChangeHandler(this));

        long pollingInterval = config.getPollingInterval();
        scheduledExecutor.scheduleWithFixedDelay(this::poll, pollingInterval, pollingInterval, TimeUnit.MILLISECONDS);

        LOG.info("TSDR Openflow Collector initialized - polling interval: {} ms", config.getPollingInterval());
    }

    @Override
    public void close() {
        storeMetricsExecutor.shutdown();
        scheduledExecutor.shutdownNow();

        LOG.info("TSDR Openflow Collector closed");
    }

    // Adds a new builder to the builder container, the first metric for the InstanceIdenfier will create
    // the builder container.
    public void addBuilderToContainer(InstanceIdentifier<Node> nodeID,
            InstanceIdentifier<?> id, TSDRMetricRecordBuilder builder) {
        // We want to synchronize here because when adding a new builder we want to make sure there is only one builder
        // container per metric path as we might get on the same InstanceIdentifier two notification in a very short
        // time and we don't want to instantiate two containers for the same metric path.
        TSDRMetricRecordBuilderContainer container;
        synchronized (builderContainers) {
            container = builderContainers.computeIfAbsent(id, key -> {
                nodeID2SubIDs.computeIfAbsent(nodeID, k -> new HashSet<>()).add(id);
                return new TSDRMetricRecordBuilderContainer();
            });
        }

        // once we have the container, synchronization of the builders array inside the container
        // is under the container responsibility.
        container.addBuilder(builder);
    }

    // Retrieve a BuilderContainer according to the InstanceIdentifier
    public TSDRMetricRecordBuilderContainer getTSDRMetricRecordBuilderContainer(InstanceIdentifier<?> id) {
        synchronized (builderContainers) {
            return builderContainers.get(id);
        }
    }

    // Create a new TSDRMetricRecordBuilder and adds it to its builder container
    // according to the instanceIdentifier
    public void createTSDRMetricRecordBuilder(InstanceIdentifier<Node> nodeID,
            InstanceIdentifier<?> id, List<RecordKeys> recKeys,
            String metricName, BigDecimal value, DataCategory category) {
        TSDRMetricRecordBuilder builder = new TSDRMetricRecordBuilder();
        builder.setRecordKeys(recKeys);
        builder.setNodeID(getNodeIDFrom(recKeys));
        builder.setMetricName(metricName);
        builder.setTSDRDataCategory(category);
        builder.setMetricValue(value);
        builder.setTimeStamp(System.currentTimeMillis());
        addBuilderToContainer(nodeID, id, builder);
    }

    private void poll() {
        InstanceIdentifier<Nodes> id = InstanceIdentifier.builder(Nodes.class).build();
        ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
        try {
            Optional<Nodes> optional = readTx.read(LogicalDatastoreType.OPERATIONAL, id).get();

            Set<InstanceIdentifier<Node>> nodeSet = new HashSet<>();
            if (optional.isPresent()) {
                Nodes nodes = optional.get();
                for (Node node : nodes.getNode()) {
                    InstanceIdentifier<Node> nodeID = id.child(Node.class, node.getKey());
                    nodeSet.add(nodeID);
                    knownNodes.add(nodeID);
                    collectStatistics(node);
                }
            }

            // unregister on removed nodes
            for (Iterator<InstanceIdentifier<Node>> iter = knownNodes.iterator(); iter.hasNext();) {
                InstanceIdentifier<Node> nodeID = iter.next();
                if (!nodeSet.contains(nodeID)) {
                    iter.remove();
                    removeAllNodeBuilders(nodeID);
                }
            }

            storeCollectedMetrics();
        } catch (ExecutionException e) {
            LOG.error("Error reading inventory", e);
        } catch (InterruptedException e) {
            LOG.debug("Inventory read interrupted");
        } finally {
            readTx.close();
        }
    }

    // Finds the handler for this statistics and apply it
    @SuppressWarnings("unchecked")
    private <T extends DataObject> void handle(InstanceIdentifier<Node> nodeID,
            InstanceIdentifier<?> id, T dataObject, Class<T> cls) {
        if (dataObject == null) {
            return;
        }
        TSDRBaseDataHandler<T> handler = (TSDRBaseDataHandler<T>) handlers.get(cls);
        if (handler == null) {
            LOG.error("Can't find collector for {}", cls.getSimpleName());
            return;
        }
        handler.handleData(nodeID, id, dataObject);
    }

    // Extract the statistics from a node and updates the builders with the updated data.
    private void collectStatistics(Node node) {
        if (node == null) {
            return;
        }

        InstanceIdentifier<Node> nodeID = InstanceIdentifier.create(
                Nodes.class).child(Node.class, node.getKey());
        FlowCapableNode fcnode = node.getAugmentation(FlowCapableNode.class);
        if (fcnode != null) {
            processMeters(node, nodeID, fcnode);

            processTables(node, nodeID, fcnode);

            processGroups(node, nodeID, fcnode);
        }

        processNodeConnectors(node, nodeID);
    }

    private void processNodeConnectors(Node node, InstanceIdentifier<Node> nodeID) {
        List<NodeConnector> ports = node.getNodeConnector();
        if (ports != null) {
            for (NodeConnector nc : ports) {
                FlowCapableNodeConnectorStatisticsData fnc =
                        nc.getAugmentation(FlowCapableNodeConnectorStatisticsData.class);
                InstanceIdentifier<FlowCapableNodeConnectorStatisticsData> statID = InstanceIdentifier
                        .create(Nodes.class)
                        .child(Node.class, node.getKey())
                        .child(NodeConnector.class, nc.getKey())
                        .augmentation(FlowCapableNodeConnectorStatisticsData.class);

                handle(nodeID, statID, fnc, FlowCapableNodeConnectorStatisticsData.class);

                FlowCapableNodeConnector fcnc = nc.getAugmentation(FlowCapableNodeConnector.class);
                if (fcnc != null) {
                    List<Queue> queues = fcnc.getQueue();
                    if (queues != null) {
                        for (Queue q : queues) {
                            InstanceIdentifier<FlowCapableNodeConnectorQueueStatisticsData> queueStatID =
                                    InstanceIdentifier.create(Nodes.class)
                                    .child(Node.class, node.getKey())
                                    .child(NodeConnector.class, nc.getKey())
                                    .augmentation(FlowCapableNodeConnector.class)
                                    .child(Queue.class, q.getKey())
                                    .augmentation(FlowCapableNodeConnectorQueueStatisticsData.class);
                            handle(nodeID, queueStatID,
                                    q.getAugmentation(FlowCapableNodeConnectorQueueStatisticsData.class),
                                    FlowCapableNodeConnectorQueueStatisticsData.class);
                        }
                    }
                }
            }
        }
    }

    private void processGroups(Node node, InstanceIdentifier<Node> nodeID, FlowCapableNode fcnode) {
        List<Group> groups = fcnode.getGroup();
        if (groups != null) {
            for (Group g : groups) {
                NodeGroupStatistics ngs = g.getAugmentation(NodeGroupStatistics.class);
                InstanceIdentifier<NodeGroupStatistics> statID = InstanceIdentifier
                        .create(Nodes.class)
                        .child(Node.class, node.getKey())
                        .augmentation(FlowCapableNode.class)
                        .child(Group.class, g.getKey())
                        .augmentation(NodeGroupStatistics.class);
                handle(nodeID, statID, ngs, NodeGroupStatistics.class);
            }
        }
    }

    private void processTables(Node node, InstanceIdentifier<Node> nodeID, FlowCapableNode fcnode) {
        List<Table> tables = fcnode.getTable();
        if (tables != null) {
            for (Table t : tables) {
                FlowTableStatisticsData data = t.getAugmentation(FlowTableStatisticsData.class);
                if (data != null) {
                    InstanceIdentifier<FlowTableStatisticsData> statID = InstanceIdentifier
                            .create(Nodes.class)
                            .child(Node.class, node.getKey())
                            .augmentation(FlowCapableNode.class)
                            .child(Table.class, t.getKey())
                            .augmentation(FlowTableStatisticsData.class);
                    handle(nodeID, statID, data, FlowTableStatisticsData.class);
                }
                // Flow Statistics
                if (t.getFlow() != null) {
                    for (Flow flow : t.getFlow()) {
                        FlowStatisticsData flowStatisticsData = flow.getAugmentation(FlowStatisticsData.class);
                        if (flowStatisticsData != null) {
                            InstanceIdentifier<FlowStatisticsData> statID = InstanceIdentifier
                                    .create(Nodes.class)
                                    .child(Node.class, node.getKey())
                                    .augmentation(FlowCapableNode.class)
                                    .child(Table.class, t.getKey())
                                    .child(Flow.class,flow.getKey())
                                    .augmentation(FlowStatisticsData.class);
                            handle(nodeID, statID, flowStatisticsData, FlowStatisticsData.class);
                        }
                    }
                }
            }
        }
    }

    private void processMeters(Node node, InstanceIdentifier<Node> nodeID, FlowCapableNode fcnode) {
        List<Meter> meters = fcnode.getMeter();
        if (meters != null) {
            for (Meter meter : meters) {
                NodeMeterStatistics nodeMeterStatistics = meter.getAugmentation(NodeMeterStatistics.class);
                if (nodeMeterStatistics != null) {
                    InstanceIdentifier<NodeMeterStatistics> statID = InstanceIdentifier
                            .create(Nodes.class)
                            .child(Node.class, node.getKey())
                            .augmentation(FlowCapableNode.class)
                            .child(Meter.class, meter.getKey())
                            .augmentation(NodeMeterStatistics.class);
                    handle(nodeID, statID, nodeMeterStatistics, NodeMeterStatistics.class);
                }
            }
        }
    }

    private String getNodeIDFrom(List<RecordKeys> recordKeys) {
        String result = null;
        for (RecordKeys key : recordKeys) {
            if (key.getKeyName().equalsIgnoreCase("Node")) {
                if (key.getKeyValue() != null) {
                    return key.getKeyValue();
                }
            }
        }
        return result;
    }

    private void storeCollectedMetrics() {
        try {
            storeMetricsExecutor.execute(this::doStoreCollectedMetrics);
        } catch (RejectedExecutionException e) {
            if (!storeMetricsExecutor.isShutdown()) {
                LOG.error("Could not enqueue task to store metrics", e);
            }
        }
    }

    private void doStoreCollectedMetrics() {
        TSDRMetricRecordBuilderContainer[] containers;
        synchronized (builderContainers) {
            containers = builderContainers.values().toArray(
                    new TSDRMetricRecordBuilderContainer[builderContainers.size()]);
        }

        if (containers.length == 0) {
            return;
        }

        LOG.debug("doStoreCollectedMetrics: {} containers", containers.length);

        // Iterate over the builder containers. create a metric data list and wrap it up as input for
        // the RPC and invoke the storage RPC method.
        List<TSDRMetricRecord> recordList = new ArrayList<>();
        for (TSDRMetricRecordBuilderContainer bc : containers) {
            for (TSDRMetricRecordBuilder builder : bc.getBuilders()) {
                recordList.add(builder.build());
            }

            if (recordList.size() >= config.getRecordStoreBatchSize()) {
                store(new InsertTSDRMetricRecordInputBuilder().setTSDRMetricRecord(recordList)
                        .setCollectorCodeName("OpenFlowStatistics").build());
                recordList = new ArrayList<>();
            }
        }

        if (!recordList.isEmpty()) {
            store(new InsertTSDRMetricRecordInputBuilder().setTSDRMetricRecord(recordList)
                    .setCollectorCodeName("OpenFlowStatistics").build());
        }
    }

    // Invoke the storage rpc method
    private void store(InsertTSDRMetricRecordInput input) {
        LOG.debug("Storing {} records", input.getTSDRMetricRecord().size());
        RPCFutures.logResult(collectorSPIService.insertTSDRMetricRecord(input), "insertTSDRMetricRecord", LOG);
    }

    private void removeAllNodeBuilders(InstanceIdentifier<Node> nodeID) {
        synchronized (builderContainers) {
            Set<InstanceIdentifier<?>> subIDs = nodeID2SubIDs.remove(nodeID);
            if (subIDs == null) {
                return;
            }

            for (InstanceIdentifier<?> subID : subIDs) {
                builderContainers.remove(subID);
            }
        }

        LOG.info("Removed all data for node {}", nodeID);
    }

    @Override
    public Future<RpcResult<Void>> setPollingInterval(SetPollingIntervalInput input) {
        TSDROSCConfigBuilder builder = new TSDROSCConfigBuilder();
        builder.setPollingInterval(input.getInterval());

        SettableFuture<RpcResult<Void>> resultFuture = SettableFuture.create();
        WriteTransaction wrt = dataBroker.newWriteOnlyTransaction();
        wrt.put(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.create(TSDROSCConfig.class), builder.build());

        ListenableFuture<Void> future = wrt.submit();
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                LOG.debug("Save config succeeded");
                resultFuture.set(RpcResultBuilder.<Void>success().build());
            }

            @Override
            public void onFailure(Throwable ex) {
                if (ex instanceof TransactionCommitFailedException) {
                    resultFuture.set(RpcResultBuilder.<Void>failed()
                            .withRpcErrors(((TransactionCommitFailedException)ex).getErrorList()).build());
                } else {
                    resultFuture.set(RpcResultBuilder.<Void>failed().withError(ErrorType.APPLICATION,
                            "Unexpected error saving config", ex).build());
                }
            }
        }, MoreExecutors.directExecutor());

        return resultFuture;
    }
}
