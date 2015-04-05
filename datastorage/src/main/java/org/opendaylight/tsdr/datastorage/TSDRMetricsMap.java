/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.datastorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
/**
 *  This class contains a hashmap that stores the metrics name and the method for
 *  obtaining the metrics from OpenFlow statistics data structures passed from
 *  Data Collection service.
 *  @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 *
 *    Created: March 15, 2015
 */
public class TSDRMetricsMap {
    public static HashMap<String, List<TSDRMetricsStruct>> tsdrMetricsMap
        = new HashMap<String, List<TSDRMetricsStruct>>();
    /*
     * Constants of the keys in the tsdrMetricsMap.
     */
    public static final String FlowMetrics = "FlowMetrics";
    public static final String FlowTableMetrics = "FlowTableMetrics";
    public static final String PortMetrics = "PortMetrics";
    public static final String GroupMetrics = "GroupMetrics";
    public static final String QueueMetrics = "QueueMetrics";

    /*
     * Constants of the metric names
     */
    //flow metrics
    public static final String PACKET_COUNT_FLOW = "PacketCount";
    public static final String BYTE_COUNT_FLOW = "ByteCount";
    //flow table metrics
    public static final String PACKETS_MATCHED_FLOWTABLE = "PacketsMatched";
    public static final String ACTIVE_FLOWS_FLOWTABLE = "AcrtiveFlows";
    public static final String PACKETS_LOOKED_UP_FLOWTABLE = "PacketsLookedUp";
    //port metrics
    public static final String COLLISION_COUNT_PORT = "CollisionCount";
    public static final String RECEIVE_CRC_ERROR_PORT = "ReceiveCRCError";
    public static final String RECEIVED_DROPS_PORT = "ReceivedDrops";
    public static final String RECEIVED_ERRORS_PORT = "ReceivedErrors";
    public static final String RECEIVE_FRAME_ERROR_PORT = "ReceiveFrameError";
    public static final String RECEIVE_OVERRUN_ERROR_PORT = "ReceiveOverRunError";
    public static final String TRANSMIT_DROPS_PORT = "TransmitDrops";
    public static final String TRANSMIT_ERRORS_PORT = "TransmitErrors";
    public static final String RECEIVED_PACKETS_PORT = "ReceivedPackets";
    public static final String TRANSMITTED_PACKETS_PORT = "TransmittedPackets";
    public static final String RECEIVED_BYTES_PORT = "ReceivedBytes";
    public static final String TRANSMITTED_BYTES_PORT = "TransmittedBytes";
    public static final String DURATION_IN_SECONDS_PORT = "DurationInSeconds";
    public static final String DURATION_IN_NANOSEC_PORT = "DurationInNanoSeconds";
    //group metrics
    public static final String PACKET_COUNT_GROUP = "PacketCount";
    public static final String BYTE_COUNT_GROUP = "ByteCount";
    public static final String REF_COUNT_GROUP = "RefCount";
    //queue metrics
    public static final String TRANSMISSION_ERRORS_QUEUE = "TransmissionErrors";
    public static final String TRANSMITTED_BYTES_QUEUE = "TransmittedBytes";
    public static final String TRANSMITTED_PACKETS_QUEUE = "TransmittedPackets";

    /*
     *  Constants of method names to obtain the metrics values from the corresponding classes
     *
     */
    //flow metrics method names
    public static final String GET_PACKET_COUNT_FLOW = "getPacketCount";
    public static final String GET_BYTE_COUNT_FLOW = "getByteCount";
    //flow table metrics method names
    public static final String GET_PACKETS_MATCHED_FLOWTABLE = "getPacketsMatched";
    public static final String GET_ACTIVE_FLOWS_FLOWTABLE = "getActiveFlows";
    public static final String GET_PACKETS_LOOKED_UP_FLOWTABLE = "getPacketsLookedUp";
    //port metrics method names
    public static final String GET_COLLISION_COUNT_PORT = "getCollisionCount";
    public static final String GET_RECEIVE_CRC_ERROR_PORT = "getReceiveCrcError";
    public static final String GET_RECEIVED_DROPS_PORT = "getReceiveDrops";
    public static final String GET_RECEIVED_ERRORS_PORT = "getReceiveErrors";
    public static final String GET_RECEIVE_FRAME_ERROR_PORT = "getReceiveFrameError";
    public static final String GET_RECEIVE_OVERRUN_ERROR_PORT = "getReceiveOverRunError";
    public static final String GET_TRANSMIT_DROPS_PORT = "getTransmitDrops";
    public static final String GET_TRANSMIT_ERRORS_PORT = "getTransmitErrors";
    public static final String GET_PACKETS_PORT = "getPackets";
    public static final String GET_TRANSMITTED_PORT = "getTransmitted";
    public static final String GET_RECEIVED_PORT = "getReceived";
    public static final String GET_BYTES_PORT = "getBytes";
    public static final String GET_DURATION_PORT = "getDuration";
    public static final String GET_DURATION_IN_SECONDS_PORT = "getSecond";
    public static final String GET_DURATION_IN_NANO_SEC_PORT = "getNanosecond";

    //group metrics method names
    public static final String GET_BYTE_COUNT_GROUP = "getByteCount";
    public static final String GET_PACKET_COUNT_GROUP = "getPacketCount";
    public static final String GET_REF_COUNT_GROUP = "getRefCount";
    //queue metrics method names
    public static final String GET_TRANSMISSION_ERRORS_QUEUE = "getTransmissionErrors";
    public static final String GET_TRANSMITTED_BYTES_QUEUE = "getTransmittedBytes";
    public static final String GET_TRANSMITTED_PACKETS_QUEUE = "getTransmittedPackets";


    /*
     * Initializes the tsdrMetricsMap for each category of tsdr metrics.
     */
    static {
     List<TSDRMetricsStruct> flowMetricsList = new ArrayList<TSDRMetricsStruct>();
     //FlowMetrics
     flowMetricsList.add(new TSDRMetricsStruct(PACKET_COUNT_FLOW, GET_PACKET_COUNT_FLOW));
     flowMetricsList.add(new TSDRMetricsStruct(BYTE_COUNT_FLOW, GET_BYTE_COUNT_FLOW));
     tsdrMetricsMap.put(FlowMetrics, flowMetricsList);
     //FlowTableMetrics
     List<TSDRMetricsStruct> flowTableMetricsList = new ArrayList<TSDRMetricsStruct>();
     flowTableMetricsList.add(new TSDRMetricsStruct(PACKETS_MATCHED_FLOWTABLE, GET_PACKETS_MATCHED_FLOWTABLE));
     flowTableMetricsList.add(new TSDRMetricsStruct(ACTIVE_FLOWS_FLOWTABLE, GET_ACTIVE_FLOWS_FLOWTABLE));
     flowTableMetricsList.add(new TSDRMetricsStruct(PACKETS_LOOKED_UP_FLOWTABLE, GET_PACKETS_LOOKED_UP_FLOWTABLE));
     tsdrMetricsMap.put(FlowTableMetrics, flowTableMetricsList);
     //PortMetrics
     List<TSDRMetricsStruct> portMetricsList = new ArrayList<TSDRMetricsStruct>();
     portMetricsList.add(new TSDRMetricsStruct(COLLISION_COUNT_PORT, GET_COLLISION_COUNT_PORT));
     portMetricsList.add(new TSDRMetricsStruct(RECEIVE_CRC_ERROR_PORT, GET_RECEIVE_CRC_ERROR_PORT));
     portMetricsList.add(new TSDRMetricsStruct(RECEIVED_DROPS_PORT, GET_RECEIVED_DROPS_PORT));
     portMetricsList.add(new TSDRMetricsStruct(RECEIVED_ERRORS_PORT, GET_RECEIVED_ERRORS_PORT));
     portMetricsList.add(new TSDRMetricsStruct(RECEIVE_FRAME_ERROR_PORT, GET_RECEIVE_FRAME_ERROR_PORT));
     portMetricsList.add(new TSDRMetricsStruct(RECEIVE_OVERRUN_ERROR_PORT, GET_RECEIVE_OVERRUN_ERROR_PORT));
     portMetricsList.add(new TSDRMetricsStruct(TRANSMIT_DROPS_PORT, GET_TRANSMIT_DROPS_PORT));
     portMetricsList.add(new TSDRMetricsStruct(TRANSMIT_ERRORS_PORT, GET_TRANSMIT_ERRORS_PORT));
     portMetricsList.add(new TSDRMetricsStruct(RECEIVED_PACKETS_PORT, GET_PACKETS_PORT, GET_RECEIVED_PORT));
     portMetricsList.add(new TSDRMetricsStruct(TRANSMITTED_PACKETS_PORT, GET_PACKETS_PORT, GET_TRANSMITTED_PORT));
     portMetricsList.add(new TSDRMetricsStruct(RECEIVED_BYTES_PORT, GET_BYTES_PORT, GET_RECEIVED_PORT));
     portMetricsList.add(new TSDRMetricsStruct(TRANSMITTED_BYTES_PORT, GET_BYTES_PORT, GET_TRANSMITTED_PORT));
     portMetricsList.add(new TSDRMetricsStruct(DURATION_IN_SECONDS_PORT, GET_DURATION_PORT, GET_DURATION_IN_SECONDS_PORT));
     portMetricsList.add(new TSDRMetricsStruct(DURATION_IN_NANOSEC_PORT, GET_DURATION_PORT, GET_DURATION_IN_NANO_SEC_PORT));

     tsdrMetricsMap.put(PortMetrics, portMetricsList);
     //GroupMetrics
     List<TSDRMetricsStruct> groupMetricsList = new ArrayList<TSDRMetricsStruct>();
     groupMetricsList.add(new TSDRMetricsStruct(BYTE_COUNT_GROUP, GET_BYTE_COUNT_GROUP));
     groupMetricsList.add(new TSDRMetricsStruct(PACKET_COUNT_GROUP, GET_PACKET_COUNT_GROUP));
     groupMetricsList.add(new TSDRMetricsStruct(REF_COUNT_GROUP, GET_REF_COUNT_GROUP));
     tsdrMetricsMap.put(GroupMetrics, groupMetricsList);
     //QueueMetrics
     List<TSDRMetricsStruct> queueMetricsList = new ArrayList<TSDRMetricsStruct>();
     queueMetricsList.add(new TSDRMetricsStruct(TRANSMISSION_ERRORS_QUEUE, GET_TRANSMISSION_ERRORS_QUEUE));
     queueMetricsList.add(new TSDRMetricsStruct(TRANSMITTED_BYTES_QUEUE, GET_TRANSMITTED_BYTES_QUEUE));
     queueMetricsList.add(new TSDRMetricsStruct(TRANSMITTED_PACKETS_QUEUE, GET_TRANSMITTED_PACKETS_QUEUE));
     tsdrMetricsMap.put(QueueMetrics, queueMetricsList);
  }
    /**
     * Obtain the tsdrMetricsMap.
     * @return
     */
    public static HashMap<String, List<TSDRMetricsStruct>> getTsdrMetricsMap() {
        return tsdrMetricsMap;
    }
}
