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
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.StoreTSDRMetricRecordInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.StoreTSDRMetricRecordInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Counter64;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.FlowTableStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.flow.table.statistics.FlowTableStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.Queue;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.port.rev130925.queues.QueueKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.NodeGroupStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.group.statistics.GroupStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.FlowCapableNodeConnectorStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.flow.capable.node.connector.statistics.FlowCapableNodeConnectorStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.FlowCapableNodeConnectorQueueStatisticsData;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.flow.capable.node.connector.queue.statistics.FlowCapableNodeConnectorQueueStatistics;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Identifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.IdentifiableItem;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.Item;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
/*
 * The TSDRDOMCollector is the place to collect metric data that exist in the Inventory model and its augmentations
 * It registers on specific locations in the data broker and every 30 seconds persists the data to the TSDR data storage
 */
public class TSDRDOMCollector {
    //A reference to the data broker
    private DataBroker dataBroker = null;
    //A map representing the instance identifier of the metric collection to the place in the cached builder collection array
    private Map<InstanceIdentifier<?>, Integer> id2Index = new ConcurrentHashMap<InstanceIdentifier<?>, Integer>();
    //An array of BuilderContainer, a builder container is a collection of metric builders that serves as a cache
    //so we won't need to instantiate and set all the static meta data of the metric when ever we want to store
    //It is an array to avoid iteration problems and synchronization issues when working with List/Set
    //As we don't really care about synchronization when reading the array, it will be much faster than using
    //some object that we need to synchronize.
    private BuilderContainer[] containers = new BuilderContainer[0];
    //Is the collector running, an indication to stop the thresds if it is closed
    private boolean running = true;
    //The reference to the the RPC registry to store the data
    private RpcProviderRegistry rpcRegistry = null;
    //Logger reference
    private static final Logger logger = LoggerFactory
            .getLogger(TSDRDOMCollector.class);
    //for debugging, specify if the logs should go to external file or the karaf log
    private static boolean logToExternalFile = false;

    public TSDRDOMCollector(DataBroker _dataBroker,
            RpcProviderRegistry _rpcRegistry) {
        log("TSDR DOM Collector Started", INFO);
        this.dataBroker = _dataBroker;
        this.rpcRegistry = _rpcRegistry;
        try {
            //Register of the main inventory node (nodes) to receive notification when
            //a node is being added
            InstanceIdentifier<Nodes> id = InstanceIdentifier.builder(Nodes.class).build();
            this.dataBroker.registerDataChangeListener(
                    LogicalDatastoreType.OPERATIONAL, id,
                    new TSDRDOMCollector.MyStatisticsChangeListener(),
                    DataChangeScope.ONE);
            new StoringThread();
        } catch (Exception err) {
            TSDRDOMCollector.log(err);
        }
    }

    //This container aggregate several metrics  builders that correlate to the same InstanceIdentifier.
    //It has an array of builders that their metric value is update by notification
    //the class assums that the order of initially adding the different metrics to the container
    //is also kept during update so it the metrics were added as "A,B,C" to the container
    //the place in the array will be 0,1,2 so when updating metric A you need to update builder[0] & so on...
    private class BuilderContainer {
        private InstanceIdentifier<?> id = null;
        private TSDRMetricRecordBuilder[] builders = new TSDRMetricRecordBuilder[0];
        //A set to make sure the same metric is not been added twice.
        private Set<String> metricNames = new HashSet<String>();

        public BuilderContainer(InstanceIdentifier<?> _id) {
            this.id = _id;
        }

        //happens only once per metric path+type, a new builder is added to be the cahce for this metric
        //type
        public void addBuilder(TSDRMetricRecordBuilder builder) {
            synchronized (this) {
                if (!metricNames.contains(builder.getMetricName())) {
                    metricNames.add(builder.getMetricName());
                    TSDRMetricRecordBuilder temp[] = new TSDRMetricRecordBuilder[builders.length + 1];
                    System.arraycopy(builders, 0, temp, 0, builders.length);
                    temp[builders.length] = builder;
                    builders = temp;
                }
            }
        }
        //return the array of buuilder for the update operations following notifications
        public TSDRMetricRecordBuilder[] getBuilders() {
            return this.builders;
        }
    }

    //Adds a new builder to the builder container, the first metric for the InstanceIdenfier will create
    //the builder container.
    public void addBuilderToContainer(InstanceIdentifier<?> id,TSDRMetricRecordBuilder builder) {
        BuilderContainer container = null;
        //We want to synchronize here because when adding a new builder we want to make sure there
        //is only one builder container per metric path as we might get on the same InstanceIdentifier two notification in a very short time
        //and we don't want to instantiate two containers for the same metric path.
        synchronized (id2Index) {
            Integer index = id2Index.get(id);
            if (index != null) {
                container = containers[index];
            } else {
                container = new BuilderContainer(id);
                BuilderContainer temp[] = new BuilderContainer[containers.length + 1];
                System.arraycopy(containers, 0, temp, 0, containers.length);
                id2Index.put(id, containers.length);
                temp[containers.length] = container;
                containers = temp;
            }
        }
        //once we have the container, synchronization of the builders array inside the container
        //is under the container responsibility
        container.addBuilder(builder);
    }

    //Retrieve a BuilderContainer according to the InstanceIdentifier
    public BuilderContainer getBuilderContainer(InstanceIdentifier<?> id) {
        Integer index = this.id2Index.get(id);
        if (index != null) {
            return containers[index];
        }
        return null;
    }

    //Create a RecordsKeys "shrink" instance path from an InstanceIdentifier
    public static RecordKeys getIdentifiableItemID(IdentifiableItem<?, Identifier<?>> ia) {
        RecordKeysBuilder rec = new RecordKeysBuilder();
        rec.setKeyName(ia.getType().getSimpleName());
        if (ia.getKey() instanceof QueueKey) {
            QueueKey qk = (QueueKey) ia.getKey();
            rec.setKeyValue("" + qk.getQueueId().getValue());
        } else if (ia.getKey() instanceof GroupKey) {
            GroupKey gk = (GroupKey) ia.getKey();
            rec.setKeyValue("" + gk.getGroupId().getValue());
        } else if (ia.getKey() instanceof NodeConnectorKey) {
            NodeConnectorKey nck = (NodeConnectorKey) ia.getKey();
            rec.setKeyValue(nck.getId().getValue());
        } else if (ia.getKey() instanceof NodeKey) {
            NodeKey nk = (NodeKey) ia.getKey();
            rec.setKeyValue(nk.getId().getValue());
        } else if (ia.getKey() instanceof TableKey) {
            TableKey tk = (TableKey) ia.getKey();
            rec.setKeyValue("" + tk.getId());
        } else {
            log("Error! - Missed Key Of type " + ia.getType().getName(), ERROR);
        }
        return rec.build();
    }

    //Create a new TSDRMetricRecordBuilder and adds it to its builder container
    //according to the instanceIdentifier
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

    //The registered lister for NodeTableStatistics, create and update the NodeTableStatistics notifications
    //to the TSDRMetricRecordBuilder.
    private class NodeTableStatisticsChangeListener implements DataChangeListener {
        public void onDataChanged(
                AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> data) {
            Map<InstanceIdentifier<?>, DataObject> updates = data
                    .getUpdatedData();
            for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : updates
                    .entrySet()) {
                if (entry.getValue() instanceof FlowTableStatisticsData) {
                    FlowTableStatisticsData table = (FlowTableStatisticsData) entry
                            .getValue();
                    FlowTableStatistics fs = table.getFlowTableStatistics();
                    InstanceIdentifier<?> id = entry.getKey();
                    BuilderContainer bc = getBuilderContainer(id);
                    if (bc != null) {
                        TSDRMetricRecordBuilder builder[] = bc.getBuilders();
                        BigInteger timeStamp = new BigInteger(""
                                + System.currentTimeMillis());
                        builder[0].setMetricValue(new Counter64(new BigInteger(
                                "" + fs.getActiveFlows())));
                        builder[0].setTimeStamp(timeStamp);
                        builder[1].setMetricValue(new Counter64(new BigInteger(
                                "" + fs.getPacketsMatched())));
                        builder[1].setTimeStamp(timeStamp);
                        builder[2].setMetricValue(new Counter64(new BigInteger(
                                "" + fs.getPacketsLookedUp())));
                        builder[2].setTimeStamp(timeStamp);
                    } else {
                        List<RecordKeys> recKeys = getRecordKeys(entry.getKey());
                        createTSDRMetricRecordBuilder(id, recKeys, "ActiveFlows", ""
                                + fs.getActiveFlows(),
                                DataCategory.FLOWTABLESTATS);
                        createTSDRMetricRecordBuilder(id, recKeys, "PacketMatch", ""
                                + fs.getPacketsMatched(),
                                DataCategory.FLOWTABLESTATS);
                        createTSDRMetricRecordBuilder(id, recKeys, "PacketLookup", ""
                                + fs.getPacketsLookedUp(),
                                DataCategory.FLOWTABLESTATS);
                    }
                }
            }
        }
    }

    //The registered lister for NodeConnectorStatistics, create and update the NodeConnectorStatistics notifications
    //to the TSDRMetricRecordBuilder.
    private class NodeConnectorStatisticsChangeListener implements
            DataChangeListener {
        public void onDataChanged(
                AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> data) {
            Map<InstanceIdentifier<?>, DataObject> updates = data
                    .getUpdatedData();
            for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : updates
                    .entrySet()) {
                if (entry.getValue() instanceof FlowCapableNodeConnectorStatisticsData) {
                    FlowCapableNodeConnectorStatisticsData stData = (FlowCapableNodeConnectorStatisticsData) entry
                            .getValue();
                    FlowCapableNodeConnectorStatistics fs = stData
                            .getFlowCapableNodeConnectorStatistics();
                    InstanceIdentifier<?> id = entry.getKey();
                    BuilderContainer bc = getBuilderContainer(id);
                    if (bc != null) {
                        TSDRMetricRecordBuilder builder[] = bc.getBuilders();
                        BigInteger timeStamp = new BigInteger(""
                                + System.currentTimeMillis());
                        builder[0].setMetricValue(new Counter64(new BigInteger(
                                "" + fs.getTransmitDrops())));
                        builder[0].setTimeStamp(timeStamp);
                        builder[1].setMetricValue(new Counter64(new BigInteger(
                                "" + fs.getReceiveDrops())));
                        builder[1].setTimeStamp(timeStamp);
                        builder[2].setMetricValue(new Counter64(new BigInteger(
                                "" + fs.getReceiveCrcError())));
                        builder[2].setTimeStamp(timeStamp);
                        builder[3].setMetricValue(new Counter64(new BigInteger(
                                "" + fs.getReceiveFrameError())));
                        builder[3].setTimeStamp(timeStamp);
                        builder[4].setMetricValue(new Counter64(new BigInteger(
                                "" + fs.getReceiveOverRunError())));
                        builder[4].setTimeStamp(timeStamp);
                        builder[5].setMetricValue(new Counter64(new BigInteger(
                                "" + fs.getTransmitErrors())));
                        builder[5].setTimeStamp(timeStamp);
                        builder[6].setMetricValue(new Counter64(new BigInteger(
                                "" + fs.getCollisionCount())));
                        builder[6].setTimeStamp(timeStamp);
                        builder[7].setMetricValue(new Counter64(new BigInteger(
                                "" + fs.getReceiveErrors())));
                        builder[7].setTimeStamp(timeStamp);
                        builder[8].setTimeStamp(timeStamp);
                        builder[9].setTimeStamp(timeStamp);
                        builder[10].setTimeStamp(timeStamp);
                        builder[11].setTimeStamp(timeStamp);
                        if(fs.getBytes()!=null){
                            builder[8].setMetricValue(new Counter64(fs.getBytes().getTransmitted()));
                            builder[9].setMetricValue(new Counter64(fs.getBytes().getReceived()));
                        }
                        if(fs.getPackets()!=null){
                            builder[10].setMetricValue(new Counter64(fs.getPackets().getTransmitted()));
                            builder[11].setMetricValue(new Counter64(fs.getPackets().getReceived()));
                        }
                    } else {
                        List<RecordKeys> recKeys = getRecordKeys(entry.getKey());
                        createTSDRMetricRecordBuilder(id, recKeys, "TransmitDrops", ""
                                + fs.getTransmitDrops(), DataCategory.PORTSTATS);
                        createTSDRMetricRecordBuilder(id, recKeys, "ReceiveDrops", ""
                                + fs.getReceiveDrops(), DataCategory.PORTSTATS);
                        createTSDRMetricRecordBuilder(id, recKeys, "ReceiveCrcError",
                                "" + fs.getReceiveCrcError(),
                                DataCategory.PORTSTATS);
                        createTSDRMetricRecordBuilder(id, recKeys,
                                "ReceiveFrameError",
                                "" + fs.getReceiveFrameError(),
                                DataCategory.PORTSTATS);
                        createTSDRMetricRecordBuilder(id, recKeys,
                                "ReceiveOverRunError",
                                "" + fs.getReceiveOverRunError(),
                                DataCategory.PORTSTATS);
                        createTSDRMetricRecordBuilder(id, recKeys, "TransmitErrors",
                                "" + fs.getTransmitErrors(),
                                DataCategory.PORTSTATS);
                        createTSDRMetricRecordBuilder(id, recKeys, "CollisionCount",
                                "" + fs.getCollisionCount(),
                                DataCategory.PORTSTATS);
                        createTSDRMetricRecordBuilder(id, recKeys, "ReceiveErrors", ""
                                + fs.getReceiveErrors(), DataCategory.PORTSTATS);
                        createTSDRMetricRecordBuilder(id, recKeys, "TransmittedBytes", "0", DataCategory.PORTSTATS);
                        createTSDRMetricRecordBuilder(id, recKeys, "ReceivedBytes", "0", DataCategory.PORTSTATS);
                        createTSDRMetricRecordBuilder(id, recKeys, "TransmittedPackets", "0", DataCategory.PORTSTATS);
                        createTSDRMetricRecordBuilder(id, recKeys, "ReceivedPackets", "0", DataCategory.PORTSTATS);
                    }
                }
            }
        }
    }

    //The registered lister for NodeGroupStatistics, create and update the NodeGroupStatistics notifications
    //to the TSDRMetricRecordBuilder.
    private class NodeGroupStatisticsChangeListener implements
            DataChangeListener {
        public void onDataChanged(
                AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> data) {
            Map<InstanceIdentifier<?>, DataObject> updates = data
                    .getUpdatedData();
            for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : updates
                    .entrySet()) {
                if (entry.getValue() instanceof NodeGroupStatistics) {
                    NodeGroupStatistics stData = (NodeGroupStatistics) entry
                            .getValue();
                    GroupStatistics gs = stData.getGroupStatistics();
                    InstanceIdentifier<?> id = entry.getKey();
                    BuilderContainer bc = getBuilderContainer(id);
                    if (bc != null) {
                        TSDRMetricRecordBuilder builder[] = bc.getBuilders();
                        BigInteger timeStamp = new BigInteger(""
                                + System.currentTimeMillis());
                        builder[0].setMetricValue(new Counter64(new BigInteger(
                                "" + gs.getRefCount())));
                        builder[0].setTimeStamp(timeStamp);
                        builder[1].setMetricValue(new Counter64(new BigInteger(
                                "" + gs.getPacketCount())));
                        builder[1].setTimeStamp(timeStamp);
                        builder[2].setMetricValue(new Counter64(new BigInteger(
                                "" + gs.getByteCount())));
                        builder[2].setTimeStamp(timeStamp);
                    } else {
                        List<RecordKeys> recKeys = getRecordKeys(entry.getKey());
                        createTSDRMetricRecordBuilder(id, recKeys, "RefCount",
                                "" + gs.getRefCount(),
                                DataCategory.FLOWGROUPSTATS);
                        createTSDRMetricRecordBuilder(id, recKeys, "PacketCount", ""
                                + gs.getPacketCount(),
                                DataCategory.FLOWGROUPSTATS);
                        createTSDRMetricRecordBuilder(id, recKeys, "ByteCount", ""
                                + gs.getByteCount(),
                                DataCategory.FLOWGROUPSTATS);
                    }
                }
            }
        }
    }

    //Create a list of RecordKeys representing the metric path from the instanceIdentifier
    private List<RecordKeys> getRecordKeys(InstanceIdentifier<?> instanceID) {
        List<RecordKeys> recKeys = new ArrayList<RecordKeys>(5);
        for (PathArgument pa : instanceID.getPathArguments()) {
            if (pa instanceof Item) {
                RecordKeysBuilder rec = new RecordKeysBuilder();
                rec.setKeyName(pa.getType().getSimpleName());
                rec.setKeyValue("");
                recKeys.add(rec.build());
            } else if (pa instanceof IdentifiableItem) {
                recKeys.add(getIdentifiableItemID((IdentifiableItem) pa));
            } else {
                log("Missed class:" + pa.getClass().getName(), ERROR);
            }
        }
        return recKeys;
    }

    //The registered lister for FlowCapableNodeConnectorQueueStatisticsDataListener, create and update the FlowCapableNodeConnectorQueueStatisticsDataListener notifications
    //to the TSDRMetricRecordBuilder.
    private class FlowCapableNodeConnectorQueueStatisticsDataListener implements DataChangeListener {
        public void onDataChanged(
                AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> data) {
            Map<InstanceIdentifier<?>, DataObject> updates = data
                    .getUpdatedData();
            for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : updates
                    .entrySet()) {
                if (entry.getValue() instanceof FlowCapableNodeConnectorQueueStatisticsData) {
                    FlowCapableNodeConnectorQueueStatisticsData stData = (FlowCapableNodeConnectorQueueStatisticsData) entry
                            .getValue();
                    FlowCapableNodeConnectorQueueStatistics gs = stData
                            .getFlowCapableNodeConnectorQueueStatistics();
                    InstanceIdentifier<?> id = entry.getKey();
                    BuilderContainer bc = getBuilderContainer(id);
                    if (bc != null) {
                        TSDRMetricRecordBuilder builder[] = bc.getBuilders();
                        BigInteger timeStamp = new BigInteger(""
                                + System.currentTimeMillis());
                        builder[0].setMetricValue(new Counter64(new BigInteger(
                                "" + gs.getTransmissionErrors())));
                        builder[0].setTimeStamp(timeStamp);
                        builder[1].setMetricValue(new Counter64(new BigInteger(
                                "" + gs.getTransmittedBytes())));
                        builder[1].setTimeStamp(timeStamp);
                        builder[2].setMetricValue(new Counter64(new BigInteger(
                                "" + gs.getTransmittedPackets())));
                        builder[2].setTimeStamp(timeStamp);
                    } else {
                        List<RecordKeys> recKeys = getRecordKeys(id);
                        createTSDRMetricRecordBuilder(id, recKeys,
                                "TransmissionErrors",
                                "" + gs.getTransmissionErrors(),
                                DataCategory.QUEUESTATS);
                        createTSDRMetricRecordBuilder(id, recKeys, "TransmittedBytes",
                                "" + gs.getTransmittedBytes(),
                                DataCategory.QUEUESTATS);
                        createTSDRMetricRecordBuilder(id, recKeys,
                                "TransmittedPackets",
                                "" + gs.getTransmittedPackets(),
                                DataCategory.QUEUESTATS);
                    }
                }
            }
        }
    }

    //this method receives a Node InstanceIdentifier, retrieve all its info from the dataBroker
    //and traverse it to find and register on the different statistics
    private void registerOnStatistics(InstanceIdentifier<Node> nodeID) {
        ReadOnlyTransaction rot = dataBroker.newReadOnlyTransaction();
        CheckedFuture<Optional<Node>, ReadFailedException> cf = rot.read(LogicalDatastoreType.OPERATIONAL, nodeID);

        try {
            Node node = cf.get().get();
            if (node != null) {
                FlowCapableNode fcnode = node
                        .getAugmentation(FlowCapableNode.class);
                if (fcnode != null) {
                    // Node Flow Statistics
                    log(node.getId().toString(), INFO);
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
                                dataBroker
                                        .registerDataChangeListener(
                                                LogicalDatastoreType.OPERATIONAL,
                                                tIID,
                                                new NodeTableStatisticsChangeListener(),
                                                DataChangeScope.SUBTREE);
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
                            dataBroker.registerDataChangeListener(
                                    LogicalDatastoreType.OPERATIONAL, tIID,
                                    new NodeGroupStatisticsChangeListener(),
                                    DataChangeScope.SUBTREE);
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
                        dataBroker.registerDataChangeListener(
                                LogicalDatastoreType.OPERATIONAL, tIID,
                                new NodeConnectorStatisticsChangeListener(),
                                DataChangeScope.SUBTREE);

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
                                dataBroker
                                        .registerDataChangeListener(
                                                LogicalDatastoreType.OPERATIONAL,
                                                tIID2,
                                                new FlowCapableNodeConnectorQueueStatisticsDataListener(),
                                                DataChangeScope.SUBTREE);
                            }
                        }
                    }
                }
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
    }

    //This is themain listener, it sole purpose is to get notified on a new node and then invoke a thread
    //that waits for 5 seconds (just in case the transactions on the node are not finished yet) and
    //then invoke the register method to register on the nodes different statistics.
    private class MyStatisticsChangeListener implements DataChangeListener {
        private Set<NodeId> availableNodes = new HashSet<NodeId>();

        public void process(Map<InstanceIdentifier<?>, DataObject> map) {
            for (DataObject dobj : map.values()) {
                Nodes nodes = (Nodes) dobj;
                for (Node node : nodes.getNode()) {
                    if (!availableNodes.contains(node.getId())) {
                        final InstanceIdentifier<Node> nodeIID = InstanceIdentifier
                                .create(Nodes.class)
                                .child(Node.class, new NodeKey(node.getId()))
                                .builder().build();
                        Runnable runthis = new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(5000);
                                } catch (Exception err) {
                                }
                                registerOnStatistics(nodeIID);
                            }
                        };
                        new Thread(runthis).start();
                    }
                }
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

    //This class is the storing thread, every 30 seconds it will wake up and iterate over the builder container array and create
    //metric data list out of the container builders, wrap it up as input for the RPC and invoke the storage RPC method.

    private class StoringThread extends Thread {
        public StoringThread() {
            this.setName("TSDR Storing Thread");
            this.start();
        }

        public void run() {
            while (running) {
                try {
                    Thread.sleep(30000);
                } catch (Exception err) {
                }
                for (int i = 0; i < containers.length; i++) {
                    try {
                        BuilderContainer bc = containers[i];
                        StoreTSDRMetricRecordInputBuilder input = new StoreTSDRMetricRecordInputBuilder();
                        List<TSDRMetricRecord> list = new LinkedList<>();
                        for (TSDRMetricRecordBuilder builder : bc.getBuilders()) {
                            list.add(builder.build());
                        }
                        input.setTSDRMetricRecord(list);
                        store(input.build());
                        // store.storeTSDRMetricRecord(input.build());
                    } catch (Exception err) {
                        log(err);
                    }
                }
            }
        }
    }

    //Invoke the storage rpc method
    private void store(StoreTSDRMetricRecordInput input) {
        TSDRService tsdrService = this.rpcRegistry
                .getRpcService(TSDRService.class);
        tsdrService.storeTSDRMetricRecord(input);
        log("Data Storage called", INFO);
    }

    //For debugging, enable the ability to output to a different file to avoid
    //looking for TSDR logs in the main log.
    private static PrintStream out = null;
    private static final int INFO = 1;
    private static final int DEBUG = 2;
    private static final int ERROR = 3;
    private static final int WARNING = 4;

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
}
