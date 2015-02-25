/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.datastorage.test;

import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.tsdr.datastorage.TSDRStorageServiceImpl;
import org.opendaylight.tsdr.model.TSDRConstants;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.StoreOFStatsInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.openflowstats.ObjectKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.openflowstats.ObjectKeysBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storeofstats.input.TSDROFStats;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storeofstats.input.TSDROFStatsBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Counter32;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Counter64;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.statistics.FlowStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.statistics.rev130819.flow.statistics.FlowStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.flow.table.statistics.FlowTableStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.table.statistics.rev131215.flow.table.statistics.FlowTableStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.group.statistics.GroupStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.statistics.rev131111.group.statistics.GroupStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.BucketId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.Bucket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.BucketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.BucketKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.statistics.BucketsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.statistics.buckets.BucketCounter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.statistics.buckets.BucketCounterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.statistics.buckets.BucketCounterKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.duration.DurationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.statistics.types.rev130925.node.connector.statistics.PacketsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.flow.capable.node.connector.statistics.FlowCapableNodeConnectorStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.port.statistics.rev131214.flow.capable.node.connector.statistics.FlowCapableNodeConnectorStatisticsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.flow.capable.node.connector.queue.statistics.FlowCapableNodeConnectorQueueStatistics;
import org.opendaylight.yang.gen.v1.urn.opendaylight.queue.statistics.rev131216.flow.capable.node.connector.queue.statistics.FlowCapableNodeConnectorQueueStatisticsBuilder;
/**
 * Unit Test for TSDR Data Storage Service.
 * * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 *
 * Created: Feb 24, 2015
 */
import org.opendaylight.yangtools.yang.common.RpcResult;

public class TSDRStorageImplTest {

    public TSDRStorageServiceImpl storageService;
    @Before
    public void setup() {
        storageService = new TSDRStorageServiceImpl();
    }

 /*   @Test
    public void testFlowStatistics() {
        FlowStatistics flowStats = new FlowStatisticsBuilder()
         .setByteCount(new Counter64(new BigInteger("10000000")))
         .setPacketCount(new Counter64(new BigInteger("10000000")))
         .build();

        List<TSDROFStats> tsdrstats = new ArrayList<TSDROFStats>();
        List<ObjectKeys> objectKeys = new ArrayList<ObjectKeys>();
        objectKeys.add((new ObjectKeysBuilder())
            .setKeyName("NodeID").setKeyValue("node1").build());
        objectKeys.add((new ObjectKeysBuilder())
            .setKeyName(TSDRConstants.FLOW_KEY_NAME)
            .setKeyValue("flow1").build());
        objectKeys.add((new ObjectKeysBuilder())
            .setKeyName(TSDRConstants.FLOW_TABLE_KEY_NAME)
            .setKeyValue("table1").build());
        tsdrstats.add((new TSDROFStatsBuilder())
            .setObjectKeys(objectKeys)
                .setFlowStatistics(flowStats)
                .setStatsType(DataCategory.FLOWSTATS).build());
        Future<RpcResult<java.lang.Void>> result = storageService
            .storeOFStats(new StoreOFStatsInputBuilder().setTSDROFStats(tsdrstats).build());

        try{
            assertTrue(result.get().isSuccessful());
        }catch(ExecutionException ee){
            ee.printStackTrace();
        }catch(InterruptedException ie){
            ie.printStackTrace();
        }
    }

    @Test
    public void testFlowTableStatistics() {
        FlowTableStatistics flowTableStats = new FlowTableStatisticsBuilder()
             .setActiveFlows(new Counter32(new Long("20000000")))
             .setPacketsLookedUp(new Counter64(new BigInteger("20000000")))
             .setPacketsMatched(new Counter64(new BigInteger("20000000")))
             .build();

        List<TSDROFStats> tsdrstats = new ArrayList<TSDROFStats>();
        List<ObjectKeys> objectKeys = new ArrayList<ObjectKeys>();
        objectKeys.add((new ObjectKeysBuilder())
            .setKeyName("NodeID").setKeyValue("node1").build());
        objectKeys.add((new ObjectKeysBuilder())
            .setKeyName(TSDRConstants.FLOW_TABLE_KEY_NAME)
            .setKeyValue("table1").build());
        tsdrstats.add((new TSDROFStatsBuilder())
            .setObjectKeys(objectKeys)
            .setFlowTableStatistics(flowTableStats)
            .setStatsType(DataCategory.FLOWTABLESTATS).build());
        Future<RpcResult<java.lang.Void>> result = storageService
            .storeOFStats(new StoreOFStatsInputBuilder().setTSDROFStats(tsdrstats).build());

        try{
        	assertTrue(result.get().isSuccessful());
        }catch(ExecutionException ee){
            ee.printStackTrace();
        }catch(InterruptedException ie){
            ie.printStackTrace();
        }
    }

    @Test
    public void testPortStatistics() {
        FlowCapableNodeConnectorStatistics portStats = new FlowCapableNodeConnectorStatisticsBuilder()
            .setPackets(new PacketsBuilder().setReceived(new BigInteger("2000"))
            .setTransmitted(new BigInteger("2000")).build())
            .setReceiveCrcError(new BigInteger("2000"))
            .setReceiveDrops(new BigInteger("2000"))
            .setReceiveErrors(new BigInteger("2000"))
            .setReceiveFrameError(new BigInteger("2000"))
            .setReceiveOverRunError(new BigInteger("2000"))
            .setTransmitDrops(new BigInteger("2000"))
            .setTransmitErrors(new BigInteger("2000"))
            .setCollisionCount(new BigInteger("2000"))
            .setDuration(new DurationBuilder().setSecond(new Counter32(new Long("20"))).build())
            .build();
        List<TSDROFStats> tsdrstats = new ArrayList<TSDROFStats>();
        List<ObjectKeys> objectKeys = new ArrayList<ObjectKeys>();
        objectKeys.add((new ObjectKeysBuilder())
            .setKeyName("NodeID").setKeyValue("node1").build());
        objectKeys.add((new ObjectKeysBuilder())
            .setKeyName(TSDRConstants.INTERNFACE_KEY_NAME)
            .setKeyValue("port1").build());
        tsdrstats.add((new TSDROFStatsBuilder())
            .setObjectKeys(objectKeys)
            .setFlowCapableNodeConnectorStatistics(portStats)
            .setStatsType(DataCategory.PORTSTATS).build());
        Future<RpcResult<java.lang.Void>> result = storageService
            .storeOFStats(new StoreOFStatsInputBuilder()
            .setTSDROFStats(tsdrstats).build());

        try{
            assertTrue(result.get().isSuccessful());
        }catch(ExecutionException ee){
            ee.printStackTrace();
        }catch(InterruptedException ie){
            ie.printStackTrace();
        }
    }

    @Test
    public void testQueueStatistics() {
        FlowCapableNodeConnectorQueueStatistics queueStats =
            new FlowCapableNodeConnectorQueueStatisticsBuilder()
                .setDuration(new DurationBuilder().setSecond
                    (new Counter32(new Long("30"))).build())
                .setTransmissionErrors(new Counter64(new BigInteger("3000")))
                .setTransmittedBytes(new Counter64(new BigInteger("3000")))
                .setTransmittedPackets(new Counter64(new BigInteger("3000")))
                    .build();

        List<TSDROFStats> tsdrstats = new ArrayList<TSDROFStats>();
        List<ObjectKeys> objectKeys = new ArrayList<ObjectKeys>();
        objectKeys.add((new ObjectKeysBuilder())
                .setKeyName("NodeID").setKeyValue("node1").build());
        objectKeys.add((new ObjectKeysBuilder())
                .setKeyName(TSDRConstants.INTERNFACE_KEY_NAME)
                .setKeyValue("port1").build());
        tsdrstats.add((new TSDROFStatsBuilder())
                .setObjectKeys(objectKeys)
                .setFlowCapableNodeConnectorQueueStatistics(queueStats)
                .setStatsType(DataCategory.QUEUESTATS).build());
        Future<RpcResult<java.lang.Void>> result = storageService.storeOFStats
            (new StoreOFStatsInputBuilder().setTSDROFStats(tsdrstats).build());


        try{
            assertTrue(result.get().isSuccessful());
        }catch(ExecutionException ee){
            ee.printStackTrace();
        }catch(InterruptedException ie){
            ie.printStackTrace();
        }
    }


    @Test
    public void testGroupStatistics() {
        List<Bucket> bucketList = new ArrayList<Bucket>();
        Bucket bucket = new BucketBuilder()
            .setBucketId(new BucketId(new Long("4000")))
            .setKey(new BucketKey(new BucketId(new Long("4000"))))
            .build();
        bucketList.add(bucket);
        List<BucketCounter> bucketCounter = new ArrayList<BucketCounter>();
        bucketCounter.add(new BucketCounterBuilder().setBucketId(new BucketId(new Long("4000")))
                .setByteCount(new Counter64(new BigInteger(new Long(40).toString())))
                .setKey(new BucketCounterKey(new BucketId(new Long("4000"))))
                .setPacketCount(new Counter64(new BigInteger(new Long(40).toString())))
                .build());
        GroupStatistics groupStats =
            new GroupStatisticsBuilder()
                .setBuckets(new BucketsBuilder()
                .setBucketCounter(bucketCounter)
                    .build())
                .setByteCount(new Counter64(new BigInteger(new Long("4000").toString())))
                .setPacketCount(new Counter64(new BigInteger(new Long("4000").toString())))
                .setRefCount(new Counter32(new Long("4000")))
                .build();
        List<TSDROFStats> tsdrstats = new ArrayList<TSDROFStats>();
        List<ObjectKeys> objectKeys = new ArrayList<ObjectKeys>();
        objectKeys.add((new ObjectKeysBuilder())
            .setKeyName("NodeID").setKeyValue("node1").build());
        objectKeys.add((new ObjectKeysBuilder())
            .setKeyName(TSDRConstants.GROUP_KEY_NAME)
            .setKeyValue("Group1").build());
        objectKeys.add((new ObjectKeysBuilder())
            .setKeyName(TSDRConstants.BUCKET_KEY_NAME)
            .setKeyValue("Bucket1").build());
        tsdrstats.add((new TSDROFStatsBuilder())
            .setObjectKeys(objectKeys)
            .setGroupStatistics(groupStats)
            .setStatsType(DataCategory.FLOWGROUPSTATS).build());
        Future<RpcResult<java.lang.Void>> result = storageService.storeOFStats
            (new StoreOFStatsInputBuilder().setTSDROFStats(tsdrstats).build());

        try{
            assertTrue(result.get().isSuccessful());
        }catch(ExecutionException ee){
            ee.printStackTrace();
        }catch(InterruptedException ie){
            ie.printStackTrace();
        }
    }*/

    @After
    public void teardown() {
         ;
    }
}
