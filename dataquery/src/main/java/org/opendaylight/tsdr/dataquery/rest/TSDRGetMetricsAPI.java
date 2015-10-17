/**
 * Copyright (c) 2015 Scott Melton All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.dataquery.rest;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
//import org.opendaylight.controller.config.yang.config.tsdr.northbound.api.TSDRNBIModule;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetMetricInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetMetricOutput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetTSDRMetricsInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetTSDRMetricsInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetTSDRMetricsOutput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.getmetric.output.Metrics;
import org.opendaylight.yangtools.yang.common.RpcResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:smelton2@uccs.edu">Scott Melton</a>
 */

@Path("/dataquery")
public class TSDRGetMetricsAPI {
    private static final Logger log = LoggerFactory.getLogger(TSDRGetMetricsAPI.class);

    // curl -GiS
    // http://localhost:8181/tsdr/dataquery/getmetrics?CategoryName=FlowStats&NodeID=openflow:1&TableID=Table1&FlowID=114&startTime=-5min&stopTime=now
    // TODO: scottm Changing to use a list of metric-ids

    @GET
    @Path("/{getmetrics}")
    @Produces("application/json")
    public Response getMetrics(@PathParam("getmetrics") String path, @QueryParam("CategoryName") String categoryName,
            @QueryParam("NodeID") String nodeID, @QueryParam("TableID") String tableID,
            @QueryParam("FlowID") String flowID, @QueryParam("startTime") String startTime,
            @QueryParam("stopTime") String stopTime) {

        TSDRMetricsRequest request = new TSDRMetricsRequest();
        request.setCategoryName(categoryName);
        request.setNodeID(nodeID);
        request.setTableID(tableID);
        request.setFlowID(flowID);
        request.setStartTime(startTime);
        request.setStopTime(stopTime);

        log.debug("DataQuery REST Request type: " + path);
        log.debug("DataQuery REST Request CategoryName: " + categoryName);
        log.debug("DataQuery REST Request NodeID: " + nodeID);
        log.debug("DataQuery REST Request TableID: " + tableID);
        log.debug("DataQuery REST Request FlowID: " + flowID);
        log.debug("DataQuery REST Request startTime: " + startTime);
        log.debug("DataQuery REST Request stopTime: " + stopTime);

        return processGetMetrics(null, request);
    }

    public Response processGetMetrics(@Context UriInfo info, TSDRMetricsRequest request) {
        TSDRMetricsReply reply = new TSDRMetricsReply();
        GetTSDRMetricsInputBuilder input = new GetTSDRMetricsInputBuilder();//

        // need utility code to choose the DataCategory, does it exist already?
        // ask YuLing
        input.setTSDRDataCategory(DataCategory.FLOWSTATS);

        // public Future<RpcResult<GetTSDRMetricsOutput>>
        // getTSDRMetrics(GetTSDRMetricsInput input){
        //
        // TSDRReply reply = new TSDRReply();
        // GetTSDRMetricInputBuilder input = new GetMetricInputBuilder();
        // input.setName(request.getTarget());
        // input.setFrom(getTimeFromString(request.getFrom()));
        // input.setUntil(getTimeFromString(request.getUntil()));
        // long maxDataPoints = 0;
        // try {
        // maxDataPoints = Long.parseLong(request.getMaxDataPoints());
        // } catch (Exception err) {
        // }
        // if (maxDataPoints == 0){
        // return Response.status(201).entity(reply).build();
        // }
        // Future<RpcResult<GetMetricOutput>> metric =
        // TSDRNBIModule.tsdrService.getMetric(input.build());
        // try {
        // List<Metrics> metrics = metric.get().getResult().getMetrics();
        // if (metrics == null || metrics.size() == 0) {
        // return Response.status(201).entity(reply).build();
        // }
        // int skip = 1;
        // if (metrics.size() > maxDataPoints) {
        // skip = (int) (metrics.size() / maxDataPoints);
        // }
        // reply.setTarget(request.getTarget());
        // int count = 0;
        // for (Metrics m : metrics) {
        // if (count % skip == 0) {
        // reply.addDataPoint(m.getTime(), m.getValue().doubleValue());
        // }
        // count++;
        // }
        // } catch (InterruptedException | ExecutionException e) {
        // logger.error("Failed to execute request",e);
        // }
        //
        // return Response.status(201).entity(new TSDRReply[]{reply}).build();

        return Response.status(201).build();
    }

    // curl -i -X GET
    // http://localhost:8181/tsdr/dataquery/getlogrecords?Category=SYSLOG&startTime=-5min&stopTime=now
//
//    @GET
//    @Path("/{getlogrecords}")
//    @Produces("application/json")
//    public Response getLogRecords(@PathParam("getlogrecords") String getlogrecords,
//            @QueryParam("category") String category, @QueryParam("ipAddress") String ipAddress,
//            @QueryParam("startTime") String startTime, @QueryParam("stopTime") String stopTime) {
//
//        TSDRLogRecordsRequest request = new TSDRLogRecordsRequest();
//        request.setCategory(category);
//        request.setIpAddress(ipAddress);
//        request.setStartTime(startTime);
//        request.setStopTime(stopTime);
//
//        log.info("DataQuery Request type: " + getlogrecords);
//        log.info("DataQuery Input Parameter category: " + category);
//        log.info("DataQuery Input Parameter ipAddress: " + ipAddress);
//        log.info("DataQuery Input Parameter startTime: " + startTime);
//        log.info("DataQuery Input Parameter stopTime: " + stopTime);
//
//        return processGetLogRecords(null, request);
//    }

    public Response processGetLogRecords(@Context UriInfo info, TSDRLogRecordsRequest request) {
        TSDRLogRecordsReply reply = new TSDRLogRecordsReply();
        GetTSDRMetricsInputBuilder input = new GetTSDRMetricsInputBuilder();//

        // need utility code to choose the DataCategory, does it exist already?
        // ask YuLing
        input.setTSDRDataCategory(DataCategory.FLOWSTATS);

        // public Future<RpcResult<GetTSDRMetricsOutput>>
        // getTSDRMetrics(GetTSDRMetricsInput input){
        //
        // TSDRReply reply = new TSDRReply();
        // GetTSDRMetricInputBuilder input = new GetMetricInputBuilder();
        // input.setName(request.getTarget());
        // input.setFrom(getTimeFromString(request.getFrom()));
        // input.setUntil(getTimeFromString(request.getUntil()));
        // long maxDataPoints = 0;
        // try {
        // maxDataPoints = Long.parseLong(request.getMaxDataPoints());
        // } catch (Exception err) {
        // }
        // if (maxDataPoints == 0){
        // return Response.status(201).entity(reply).build();
        // }
        // Future<RpcResult<GetMetricOutput>> metric =
        // TSDRNBIModule.tsdrService.getMetric(input.build());
        // try {
        // List<Metrics> metrics = metric.get().getResult().getMetrics();
        // if (metrics == null || metrics.size() == 0) {
        // return Response.status(201).entity(reply).build();
        // }
        // int skip = 1;
        // if (metrics.size() > maxDataPoints) {
        // skip = (int) (metrics.size() / maxDataPoints);
        // }
        // reply.setTarget(request.getTarget());
        // int count = 0;
        // for (Metrics m : metrics) {
        // if (count % skip == 0) {
        // reply.addDataPoint(m.getTime(), m.getValue().doubleValue());
        // }
        // count++;
        // }
        // } catch (InterruptedException | ExecutionException e) {
        // logger.error("Failed to execute request",e);
        // }
        //
        // return Response.status(201).entity(new TSDRReply[]{reply}).build();

        return Response.status(201).build();
    }
    // private static final String FLOW_STATS_TABLE_NAME = "FlowMetrics";
    // private static final String FLOW_TABLE_STATS_TABLE_NAME =
    // "FlowTableMetrics";
    // private static final String INTERFACE_METRICS_TABLE_NAME =
    // "InterfaceMetrics";
    // private static final String QUEUE_METRICS_TABLE_NAME = "QueueMetrics";
    // private static final String GROUP_METRICS_TABLE_NAME = "GroupMetrics";
    // private static final String METER_METRICS_TABLE_NAME = "MeterMetrics";
    //
    // private static String getTableNameFrom(DataCategory datacategory){
    // if ( datacategory == DataCategory.FLOWSTATS){
    // return FLOW_STATS_TABLE_NAME;
    // }else if ( datacategory == DataCategory.FLOWTABLESTATS){
    // return FLOW_TABLE_STATS_TABLE_NAME;
    // }else if ( datacategory == DataCategory.FLOWGROUPSTATS){
    // return GROUP_METRICS_TABLE_NAME;
    // }else if (datacategory == DataCategory.PORTSTATS){
    // return INTERFACE_METRICS_TABLE_NAME;
    // }else if (datacategory == DataCategory.QUEUESTATS){
    // return QUEUE_METRICS_TABLE_NAME;
    // }else if (datacategory == DataCategory.FLOWMETERSTATS){
    // return METER_METRICS_TABLE_NAME;
    // }
    //
    // return "";
    // }
    //
    // TSDRReply reply = new TSDRReply();
    // GetTSDRMetricInputBuilder input = new GetMetricInputBuilder();
    // input.setName(request.getTarget());
    // input.setFrom(getTimeFromString(request.getFrom()));
    // input.setUntil(getTimeFromString(request.getUntil()));
    // long maxDataPoints = 0;
    // try {
    // maxDataPoints = Long.parseLong(request.getMaxDataPoints());
    // } catch (Exception err) {
    // }
    // if (maxDataPoints == 0){
    // return Response.status(201).entity(reply).build();
    // }
    // Future<RpcResult<GetMetricOutput>> metric =
    // TSDRNBIModule.tsdrService.getMetric(input.build());
    // try {
    // List<Metrics> metrics = metric.get().getResult().getMetrics();
    // if (metrics == null || metrics.size() == 0) {
    // return Response.status(201).entity(reply).build();
    // }
    // int skip = 1;
    // if (metrics.size() > maxDataPoints) {
    // skip = (int) (metrics.size() / maxDataPoints);
    // }
    // reply.setTarget(request.getTarget());
    // int count = 0;
    // for (Metrics m : metrics) {
    // if (count % skip == 0) {
    // reply.addDataPoint(m.getTime(), m.getValue().doubleValue());
    // }
    // count++;
    // }
    // } catch (InterruptedException | ExecutionException e) {
    // logger.error("Failed to execute request",e);
    // }
    //
    // return Response.status(201).entity(new TSDRReply[]{reply}).build();

    public static long getTimeFromString(String t) {
        if (t == null)
            return System.currentTimeMillis();
        int index1 = t.indexOf("-");
        if (index1 != -1) {
            int index2 = t.indexOf("min");
            if (index2 != -1) {
                int min = Integer.parseInt(t.substring(index1 + 1, index2).trim());
                return System.currentTimeMillis() - (min * 60000);
            }
            index2 = t.indexOf("h");
            if (index2 != -1) {
                int h = Integer.parseInt(t.substring(index1 + 1, index2).trim());
                return System.currentTimeMillis() - (h * 3600000);
            }
            index2 = t.indexOf("d");
            if (index2 != -1) {
                int d = Integer.parseInt(t.substring(index1 + 1, index2).trim());
                return System.currentTimeMillis() - (d * 86400000);
            }
        } else if (t.equals("now")) {
            return System.currentTimeMillis();
        }
        return Long.parseLong(t) * 1000;
    }
}