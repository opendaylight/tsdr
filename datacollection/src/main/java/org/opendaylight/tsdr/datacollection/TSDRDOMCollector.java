/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.datacollection;

import java.io.File;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.tsdr.datacollection.listeners.FlowCapableNodeConnectorQueueStatisticsDataListener;
import org.opendaylight.tsdr.datacollection.listeners.FlowStatisticsDataListener;
import org.opendaylight.tsdr.datacollection.listeners.NodeConnectorStatisticsChangeListener;
import org.opendaylight.tsdr.datacollection.listeners.NodeGroupStatisticsChangeListener;
import org.opendaylight.tsdr.datacollection.listeners.NodeTableStatisticsChangeListener;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.StoreTSDRMetricRecordInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.StoreTSDRMetricRecordInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Counter64;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.FlowStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.FlowTableStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.Queue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.FlowCapableNodeConnectorStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.FlowCapableNodeConnectorQueueStatisticsData;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
/*
 * The TSDRDOMCollector is the place to collect metric data that exist in the
 * Inventory model and its augmentations It registers on specific locations in
 * the data broker and every 30 seconds persists the data to the TSDR data
 * storage
 */
public class TSDRDOMCollector {
    // A reference to the data broker
    private DataBroker dataBroker = null;
    // A map representing the instance identifier of the metric collection to
    // the place in the cached builder collection array
    private Map<InstanceIdentifier<?>, ContainerIndex> id2Index = new ConcurrentHashMap<InstanceIdentifier<?>, ContainerIndex>();
    // An array of BuilderContainer, a builder container is a collection of
    // metric builders that serves as a cache
    // so we won't need to instantiate and set all the static meta data of the
    // metric when ever we want to store
    // It is an array to avoid iteration problems and synchronization issues
    // when working with List/Set
    // As we don't really care about synchronization when reading the array, it
    // will be much faster than using
    // some object that we need to synchronize.
    private TSDRMetricRecordBuilderContainer[] containers = new TSDRMetricRecordBuilderContainer[0];
    // Is the collector running, an indication to stop the thresds if it is
    // closed
    private boolean running = true;
    // The reference to the the RPC registry to store the data
    private RpcProviderRegistry rpcRegistry = null;
    // Logger reference
    private static final Logger logger = LoggerFactory
            .getLogger(TSDRDOMCollector.class);
    // for debugging, specify if the logs should go to external file or the
    // karaf log
    private static boolean logToExternalFile = false;

    // The registered listeners so we could remove and unregister them on demand
    private Map<InstanceIdentifier<?>, Map<InstanceIdentifier<?>, Map<String, TSDRBaseDataChangeListener>>> listeners = new ConcurrentHashMap<>();

    public TSDRDOMCollector(DataBroker _dataBroker,
            RpcProviderRegistry _rpcRegistry) {
        log("TSDR DOM Collector Started", INFO);
        this.dataBroker = _dataBroker;
        this.rpcRegistry = _rpcRegistry;
        /*
         * try { //Register of the main inventory node (nodes) to receive
         * notification when //a node is being added InstanceIdentifier<Nodes>
         * id = InstanceIdentifier.builder(Nodes.class).build();
         * this.dataBroker.registerDataChangeListener(
         * LogicalDatastoreType.OPERATIONAL, id, new
         * TSDRDOMCollector.MyStatisticsChangeListener(), DataChangeScope.ONE);
         * new StoringThread(); } catch (Exception err) {
         * TSDRDOMCollector.log(err); }
         */

        new TSDRInventoryNodesPoller(this);
        new StoringThread();
    }

    // Adds a new builder to the builder container, the first metric for the
    // InstanceIdenfier will create
    // the builder container.
    public void addBuilderToContainer(InstanceIdentifier<?> id,
            TSDRMetricRecordBuilder builder) {
        TSDRMetricRecordBuilderContainer container = null;
        // We want to synchronize here because when adding a new builder we want
        // to make sure there
        // is only one builder container per metric path as we might get on the
        // same InstanceIdentifier two notification in a very short time
        // and we don't want to instantiate two containers for the same metric
        // path.
        synchronized (id2Index) {
            ContainerIndex index = id2Index.get(id);
            if (index != null) {
                container = containers[index.index];
            } else {
                container = new TSDRMetricRecordBuilderContainer();
                TSDRMetricRecordBuilderContainer temp[] = new TSDRMetricRecordBuilderContainer[containers.length + 1];
                System.arraycopy(containers, 0, temp, 0, containers.length);
                id2Index.put(id, new ContainerIndex(containers.length));
                temp[containers.length] = container;
                containers = temp;
            }
        }
        // once we have the container, synchronization of the builders array
        // inside the container
        // is under the container responsibility
        container.addBuilder(builder);
    }

    public void removeBuilderContailer(InstanceIdentifier<?> id) {
        synchronized (id2Index) {
            ContainerIndex index = id2Index.remove(id);
            if (index != null) {
                TSDRMetricRecordBuilderContainer temp[] = new TSDRMetricRecordBuilderContainer[containers.length - 1];
                if (index.index == 0) {
                    System.arraycopy(containers, 1, temp, 0, temp.length);
                } else if (index.index == containers.length - 1) {
                    System.arraycopy(containers, 0, temp, 0, temp.length);
                } else {
                    System.arraycopy(containers, 0, temp, 0, index.index);
                    System.arraycopy(containers, index.index + 1, temp,
                            index.index, containers.length - (index.index + 1));
                }
                for (ContainerIndex ndx : id2Index.values()) {
                    if (ndx.index > index.index) {
                        ndx.index--;
                    }
                }
                containers = temp;
            }
        }
    }

    // Retrieve a BuilderContainer according to the InstanceIdentifier
    public TSDRMetricRecordBuilderContainer getTSDRMetricRecordBuilderContainer(
            InstanceIdentifier<?> id) {
        ContainerIndex index = this.id2Index.get(id);
        if (index != null) {
            return containers[index.index];
        }
        return null;
    }

    // Create a new TSDRMetricRecordBuilder and adds it to its builder container
    // according to the instanceIdentifier
    public void createTSDRMetricRecordBuilder(InstanceIdentifier<?> id,
            List<RecordKeys> recKeys, String metricName, String value,
            DataCategory category) {
        TSDRMetricRecordBuilder builder = new TSDRMetricRecordBuilder();
        builder.setRecordKeys(recKeys);
        builder.setNodeID(getNodeIDFrom(recKeys));
        builder.setMetricName(metricName);
        builder.setTSDRDataCategory(category);
        Counter64 _value = null;
        try {
            _value = new Counter64(new BigInteger(value));
        } catch (Exception err) {
            log("Failed to set the counter value metric=" + metricName + " - "
                    + value, ERROR);
        }
        builder.setMetricValue(_value);
        builder.setTimeStamp(new BigInteger("" + System.currentTimeMillis()));
        addBuilderToContainer(id, builder);
    }

    // this method receives a Node InstanceIdentifier, retrieve all its info
    // from the dataBroker
    // and traverse it to find and register on the different statistics
    public void registerOnStatistics(InstanceIdentifier<Node> nodeID) {
        ReadOnlyTransaction rot = dataBroker.newReadOnlyTransaction();
        CheckedFuture<Optional<Node>, ReadFailedException> cf = rot.read(
                LogicalDatastoreType.OPERATIONAL, nodeID);

        try {
            Node node = cf.get().get();
            if (node != null) {
                FlowCapableNode fcnode = node
                        .getAugmentation(FlowCapableNode.class);
                if (fcnode != null) {
                    // Node Flow Statistics
                    if (!listeners.containsKey(nodeID)) {
                        log(node.getId().toString(), INFO);
                    }
                    List<Table> tables = fcnode.getTable();
                    if (tables != null) {
                        for (Table t : tables) {
                            FlowTableStatisticsData data = t
                                    .getAugmentation(FlowTableStatisticsData.class);
                            if (data != null) {
                                InstanceIdentifier<FlowTableStatisticsData> tIID = InstanceIdentifier
                                        .create(Nodes.class)
                                        .child(Node.class, node.getKey())
                                        .augmentation(FlowCapableNode.class)
                                        .child(Table.class, t.getKey())
                                        .augmentation(
                                                FlowTableStatisticsData.class);
                                if (!checkIfListenerExist(nodeID, tIID,
                                        "NodeTableStatisticsChangeListener")) {
                                    new NodeTableStatisticsChangeListener(
                                            nodeID, tIID, this);
                                }
                            }
                            // Flow Statistics
                            if (t.getFlow() != null) {
                                for (Flow flow : t.getFlow()) {
                                    FlowStatisticsData flowStatisticsData = flow
                                            .getAugmentation(FlowStatisticsData.class);
                                    if (flowStatisticsData != null) {
                                        InstanceIdentifier<FlowStatisticsData> tIID = InstanceIdentifier
                                                .create(Nodes.class)
                                                .child(Node.class,
                                                        node.getKey())
                                                .augmentation(
                                                        FlowCapableNode.class)
                                                .child(Table.class, t.getKey())
                                                .child(Flow.class,
                                                        flow.getKey())
                                                .augmentation(
                                                        FlowStatisticsData.class);
                                        if (!checkIfListenerExist(nodeID, tIID,
                                                "FlowStatisticsDataListener")) {
                                            new FlowStatisticsDataListener(
                                                    nodeID, tIID, this);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Node Group Statistics
                    List<Group> groups = fcnode.getGroup();
                    if (groups != null) {
                        for (Group g : groups) {
                            NodeGroupStatistics ngs = g
                                    .getAugmentation(NodeGroupStatistics.class);
                            InstanceIdentifier<NodeGroupStatistics> tIID = InstanceIdentifier
                                    .create(Nodes.class)
                                    .child(Node.class, node.getKey())
                                    .augmentation(FlowCapableNode.class)
                                    .child(Group.class, g.getKey())
                                    .augmentation(NodeGroupStatistics.class);
                            if (!checkIfListenerExist(nodeID, tIID,
                                    "NodeGroupStatisticsChangeListener")) {
                                new NodeGroupStatisticsChangeListener(nodeID,
                                        tIID, this);
                            }
                        }
                    }
                }

                // Node Connector Statistics
                List<NodeConnector> ports = node.getNodeConnector();
                if (ports != null) {
                    for (NodeConnector nc : ports) {
                        FlowCapableNodeConnectorStatisticsData fnc = nc
                                .getAugmentation(FlowCapableNodeConnectorStatisticsData.class);
                        InstanceIdentifier<FlowCapableNodeConnectorStatisticsData> tIID = InstanceIdentifier
                                .create(Nodes.class)
                                .child(Node.class, node.getKey())
                                .child(NodeConnector.class, nc.getKey())
                                .augmentation(
                                        FlowCapableNodeConnectorStatisticsData.class);
                        if (!checkIfListenerExist(nodeID, tIID,
                                "NodeConnectorStatisticsChangeListener")) {
                            new NodeConnectorStatisticsChangeListener(nodeID,
                                    tIID, this);
                        }

                        FlowCapableNodeConnector fcnc = nc
                                .getAugmentation(FlowCapableNodeConnector.class);
                        List<Queue> queues = fcnc.getQueue();
                        if (queues != null) {
                            for (Queue q : queues) {
                                InstanceIdentifier<FlowCapableNodeConnectorQueueStatisticsData> tIID2 = InstanceIdentifier
                                        .create(Nodes.class)
                                        .child(Node.class, node.getKey())
                                        .child(NodeConnector.class, nc.getKey())
                                        .augmentation(
                                                FlowCapableNodeConnector.class)
                                        .child(Queue.class, q.getKey())
                                        .augmentation(
                                                FlowCapableNodeConnectorQueueStatisticsData.class);
                                if (!checkIfListenerExist(nodeID, tIID2,
                                        "FlowCapableNodeConnectorQueueStatisticsDataListener")) {
                                    new FlowCapableNodeConnectorQueueStatisticsDataListener(
                                            nodeID, tIID2, this);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception err) {
            log("Failed to register on metric data due to the following exception:",
                    ERROR);
            log(err);
        }
    }

    // This is the main listener, it sole purpose is to get notified on a new
    // node and then invoke a thread
    // that waits for 5 seconds (just in case the transactions on the node are
    // not finished yet) and
    // then invoke the register method to register on the nodes different
    // statistics.
    private class MyStatisticsChangeListener implements DataChangeListener {
        private Set<NodeId> availableNodes = new HashSet<NodeId>();

        public void process(Map<InstanceIdentifier<?>, DataObject> map) {
            try {
                for (DataObject dobj : map.values()) {
                    Nodes nodes = (Nodes) dobj;
                    for (Node node : nodes.getNode()) {
                        if (!availableNodes.contains(node.getId())) {
                            final InstanceIdentifier<Node> nodeIID = InstanceIdentifier
                                    .create(Nodes.class)
                                    .child(Node.class,
                                            new NodeKey(node.getId()))
                                    .builder().build();
                            Runnable runthis = new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Thread.sleep(5000);
                                    } catch (InterruptedException err) {
                                        log("Interrupted while sleeping before processing",
                                                ERROR);
                                    }
                                    registerOnStatistics(nodeIID);
                                }
                            };
                            new Thread(runthis).start();
                        }
                    }
                }
            } catch (Exception err) {
                log("Unknown Error has occured as follows:", ERROR);
                log(err);
            }
        }

        @Override
        public void onDataChanged(
                AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> arg0) {
            if (arg0.getCreatedData() != null) {
                process(arg0.getCreatedData());
            }
            if (arg0.getUpdatedData() != null) {
                process(arg0.getUpdatedData());
            }
            if (arg0.getRemovedPaths() != null) {
                for (InstanceIdentifier<?> id : arg0.getRemovedPaths()) {
                    try {
                        removeAllNodeListeners((InstanceIdentifier<Node>) id);
                    } catch (Exception err) {
                        log("Failed to remove node listeners due to the following exception:",
                                ERROR);
                        log(err);
                    }
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

    // This class is the storing thread, every 30 seconds it will wake up and
    // iterate over the builder container array and create
    // metric data list out of the container builders, wrap it up as input for
    // the RPC and invoke the storage RPC method.

    private class StoringThread extends Thread {
        public StoringThread() {
            this.setName("TSDR Storing Thread");
            this.start();
            log("Storing Thread Started", INFO);
        }

        public void run() {
            while (running) {
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException err) {
                    log("Storing Thread Interrupted.", ERROR);
                }
                try {
                    for (int i = 0; i < containers.length; i++) {
                        try {
                            TSDRMetricRecordBuilderContainer bc = containers[i];
                            StoreTSDRMetricRecordInputBuilder input = new StoreTSDRMetricRecordInputBuilder();
                            List<TSDRMetricRecord> list = new LinkedList<>();
                            for (TSDRMetricRecordBuilder builder : bc
                                    .getBuilders()) {
                                list.add(builder.build());
                            }
                            input.setTSDRMetricRecord(list);
                            store(input.build());
                            // store.storeTSDRMetricRecord(input.build());
                        } catch (Exception err) {
                            log("Fail to store data due to the following exception:",
                                    ERROR);
                            log(err);
                        }
                    }
                } catch (Exception err) {
                    log("Fail to iterate over builder containers due to the following error:",
                            ERROR);
                    log(err);
                }
            }
        }
    }

    // Invoke the storage rpc method
    private void store(StoreTSDRMetricRecordInput input) {
        TSDRService tsdrService = this.rpcRegistry
                .getRpcService(TSDRService.class);
        tsdrService.storeTSDRMetricRecord(input);
        log("Data Storage called", DEBUG);
    }

    // For debugging, enable the ability to output to a different file to avoid
    // looking for TSDR logs in the main log.
    public static PrintStream out = null;
    public static final int INFO = 1;
    public static final int DEBUG = 2;
    public static final int ERROR = 3;
    public static final int WARNING = 4;

    public static synchronized void log(Exception e) {
        if (logToExternalFile) {
            try {
                if (out == null) {
                    File f = new File("/tmp/tsdr.log");
                    out = new PrintStream(f);
                }
                e.printStackTrace(out);
                out.flush();
            } catch (Exception err) {
                err.printStackTrace();
            }
        } else {
            logger.error(e.getMessage(), e);
        }
    }

    public static synchronized void log(String str, int type) {
        if (logToExternalFile) {
            try {
                if (out == null) {
                    File f = new File("/tmp/tsdr.log");
                    out = new PrintStream(f);
                }
                out.println(str);
                out.flush();
            } catch (Exception err) {
                err.printStackTrace();
            }
        } else {
            switch (type) {
            case INFO:
                logger.info(str);
                break;
            case DEBUG:
                logger.debug(str);
                break;
            case ERROR:
                logger.error(str);
                break;
            case WARNING:
                logger.warn(str);
                break;
            default:
                logger.debug(str);
            }
        }
    }

    private boolean checkIfListenerExist(InstanceIdentifier<Node> nodeID,
            InstanceIdentifier<?> statID, String listenerName) {
        Map<InstanceIdentifier<?>, Map<String, TSDRBaseDataChangeListener>> nodeListeners = listeners
                .get(nodeID);
        if (nodeListeners == null)
            return false;
        Map<String, TSDRBaseDataChangeListener> statsListeners = nodeListeners
                .get(statID);
        if (statsListeners == null)
            return false;
        return statsListeners.containsKey(listenerName);
    }

    public void addListener(InstanceIdentifier<Node> nodeID,
            InstanceIdentifier<?> statID, String listenerName,
            TSDRBaseDataChangeListener listener) {
        Map<InstanceIdentifier<?>, Map<String, TSDRBaseDataChangeListener>> nodeListeners = listeners
                .get(nodeID);
        if (nodeListeners == null) {
            nodeListeners = new ConcurrentHashMap<>();
            listeners.put(nodeID, nodeListeners);
        }
        Map<String, TSDRBaseDataChangeListener> statsListeners = nodeListeners
                .get(statID);
        if (statsListeners == null) {
            statsListeners = new ConcurrentHashMap<>();
            nodeListeners.put(statID, statsListeners);
        }
        statsListeners.put(listenerName, listener);
    }

    public void removeAllNodeListeners(InstanceIdentifier<Node> nodeID) {
        Map<InstanceIdentifier<?>, Map<String, TSDRBaseDataChangeListener>> nodeListeners = listeners
                .get(nodeID);
        if (nodeListeners == null)
            return;
        for (Map<String, TSDRBaseDataChangeListener> l : nodeListeners.values()) {
            for (TSDRBaseDataChangeListener listener : l.values()) {
                listener.closeRegistrations();
                removeBuilderContailer(listener.getIID());
            }
        }
        listeners.remove(nodeID);
    }

    public DataBroker getDataBroker() {
        return this.dataBroker;
    }

    public boolean isRunning() {
        return this.running;
    }

    private class ContainerIndex {
        public ContainerIndex(Integer _index) {
            this.index = _index;
        }

        private Integer index = -1;
    }
}
