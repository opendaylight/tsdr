/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.dataquery.rest.nbi;

import com.google.gson.Gson;
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
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.AggregationType;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.GetTSDRAggregatedMetricsInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.GetTSDRAggregatedMetricsOutput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.GetTSDRMetricsInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.GetTSDRMetricsOutput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.TsdrMetricDataService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.gettsdraggregatedmetrics.output.AggregatedMetrics;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.gettsdrmetrics.output.Metrics;
import org.opendaylight.yangtools.yang.common.RpcResult;

/**
 * Northbound REST endpoint.
 *
 * @author Sharon Aicler(saichler@gmail.com)
 **/
@Path("/nbi")
public class TSDRNbiRestAPI {
    private static TsdrMetricDataService metricDataService;

    public static void setMetricDataService(TsdrMetricDataService newMetricDataService) {
        metricDataService = newMetricDataService;
    }

    @GET
    @Path("/{render}")
    @Produces("application/json")
    public Response get(@PathParam("render") String render,
            @QueryParam("target") String target,
            @QueryParam("from") String from,
            @QueryParam("until") String until,
            @QueryParam("format") String format,
            @QueryParam("maxDataPoints") String maxDataPoints) throws ExecutionException, InterruptedException {
        //Example query from Grafana
        //Get render?target=EXTERNAL.Heap:Memory:Usage.Controller&from=-5min&until=now&format=json&maxDataPoints=1582
        TSDRNbiRequest request = new TSDRNbiRequest();
        request.setFormat(format);
        request.setFrom(from);
        request.setMaxDataPoints(maxDataPoints);
        request.setTarget(target);
        request.setUntil(until);
        return post(null,request);
    }

    @POST
    @Produces("application/json")
    public Response post(@Context UriInfo info, TSDRNbiRequest request)
            throws ExecutionException, InterruptedException {
        final TSDRNbiReply reply = new TSDRNbiReply();
        reply.setTarget(request.getTarget());

        final long from = getTimeFromString(request.getFrom());
        final long until = getTimeFromString(request.getUntil());
        final long maxDataPoints = request.getMaxDataPoints() != null ? Long.parseLong(request.getMaxDataPoints()) : 0;

        if (maxDataPoints < 1) {
            // Return the points without any aggregation
            final GetTSDRMetricsInputBuilder input = new GetTSDRMetricsInputBuilder();
            input.setTSDRDataCategory(request.getTarget());
            input.setStartTime(from);
            input.setEndTime(until);

            Future<RpcResult<GetTSDRMetricsOutput>> metric = metricDataService.getTSDRMetrics(input.build());
            if (!metric.get().isSuccessful()) {
                Response.status(503).entity("{}").build();
            }
            List<Metrics> metrics = metric.get().getResult().getMetrics();
            if (metrics != null) {
                for (Metrics m : metrics) {
                    reply.addDataPoint(m.getTimeStamp(), m.getMetricValue().doubleValue());
                }
            }
        } else {
            // Average the points
            final GetTSDRAggregatedMetricsInputBuilder input = new GetTSDRAggregatedMetricsInputBuilder();
            input.setTSDRDataCategory(request.getTarget());
            input.setStartTime(from);
            input.setEndTime(until);
            input.setInterval(Math.floorDiv(until - from, maxDataPoints) + 1);
            input.setAggregation(AggregationType.MEAN);

            Future<RpcResult<GetTSDRAggregatedMetricsOutput>> metric = metricDataService
                    .getTSDRAggregatedMetrics(input.build());
            if (metric == null) {
                return Response.status(501).entity("{}").build();
            }

            List<AggregatedMetrics> metrics = metric.get().getResult().getAggregatedMetrics();
            if (metrics != null) {
                for (AggregatedMetrics m : metrics) {
                    reply.addDataPoint(m.getTimeStamp(),
                            m.getMetricValue() != null ? m.getMetricValue().doubleValue() : null);
                }
            }
        }

        if (reply.getDatapoints().size() < 1) {
            return Response.status(201).entity("{}").build();
        }
        return Response.status(201).entity(toJson(new TSDRNbiReply[]{reply})).build();
    }

    public static long getTimeFromString(String str) {
        if (str == null) {
            return System.currentTimeMillis();
        }
        int index1 = str.indexOf("-");
        if (index1 != -1) {
            int index2 = str.indexOf("min");
            if (index2 != -1) {
                int min = Integer.parseInt(str.substring(index1 + 1, index2)
                        .trim());
                return System.currentTimeMillis() - min * 60000;
            }
            index2 = str.indexOf("h");
            if (index2 != -1) {
                int hours = Integer.parseInt(str.substring(index1 + 1, index2).trim());
                return System.currentTimeMillis() - hours * 3600000;
            }
            index2 = str.indexOf("d");
            if (index2 != -1) {
                int days = Integer.parseInt(str.substring(index1 + 1, index2).trim());
                return System.currentTimeMillis() - days * 86400000;
            }
        } else if (str.equals("now")) {
            return System.currentTimeMillis();
        }
        return Long.parseLong(str) * 1000;
    }

    public static final String toJson(Object obj) {
        Gson gson = new Gson();
        return gson.toJson(obj);
    }
}
