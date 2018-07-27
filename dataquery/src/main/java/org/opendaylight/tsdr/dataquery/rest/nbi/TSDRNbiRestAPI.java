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
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.AggregationType;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.GetTSDRAggregatedMetricsInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.GetTSDRAggregatedMetricsOutput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.GetTSDRMetricsInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.GetTSDRMetricsOutput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.TsdrMetricDataService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.gettsdraggregatedmetrics.output.AggregatedMetrics;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.gettsdrmetrics.output.Metrics;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Northbound REST endpoint.
 *
 * @author Sharon Aicler(saichler@gmail.com)
 * @author Scott Melton (smelton2@uccs.edu)
 **/
@Path("/nbi")
public class TSDRNbiRestAPI {
    private static final Logger LOG = LoggerFactory.getLogger(TSDRNbiRestAPI.class);

    private final TsdrMetricDataService metricDataService;

    public TSDRNbiRestAPI(TsdrMetricDataService metricDataService) {
        this.metricDataService = metricDataService;
    }

    @GET
    @Path("/render")
    @Produces("application/json")
    public Response get(@PathParam("render") String render, @QueryParam("target") String target,
            @QueryParam("from") String from, @QueryParam("until") String until, @QueryParam("format") String format,
            @QueryParam("maxDataPoints") String maxDataPoints) throws ExecutionException, InterruptedException {

        // Example query from Grafana
        // Get
        // render?target=EXTERNAL.Heap:Memory:Usage.Controller&from=-5min&until=now&format=json&maxDataPoints=1582

        TSDRNbiRequest request = new TSDRNbiRequest();
        request.setFormat(format);
        request.setFrom(from);
        request.setMaxDataPoints(maxDataPoints);
        request.setTarget(target);
        request.setUntil(until);

        return execute(request);
    }

    @POST
    @Path("/render")
    @Produces("application/json")
    @Consumes("application/x-www-form-urlencoded")
    public Response post(@FormParam("target") String target, @FormParam("from") String from,
            @FormParam("until") String until, @FormParam("format") String format,
            @FormParam("maxDataPoints") String maxDataPoints) throws ExecutionException, InterruptedException {

        TSDRNbiRequest request = new TSDRNbiRequest();
        request.setFormat(format);
        request.setFrom(from);
        request.setMaxDataPoints(maxDataPoints);
        request.setTarget(target);
        request.setUntil(until);

        return execute(request);
    }

    private Response execute(TSDRNbiRequest request) throws ExecutionException, InterruptedException {

        final TSDRNbiReply reply = new TSDRNbiReply();
        reply.setTarget(request.getTarget());

        long from = 0;
        long until = 0;
        final String fromString = request.getFrom();
        final String untilString = request.getUntil();

        try {
            from = getTimeFromString(fromString);
        } catch (NumberFormatException ex) {
            String errStr = "Invalid request format. Cannot parse start time == " + fromString;
            LOG.error(errStr);
            return Response.status(Response.Status.BAD_REQUEST).entity(toJson(errStr)).build();
        }

        try {
            until = getTimeFromString(untilString);
        } catch (NumberFormatException ex) {
            String errStr = "Invalid request format. Cannot parse end time == " + untilString;
            LOG.error(errStr);
            return Response.status(Response.Status.BAD_REQUEST).entity(toJson(errStr)).build();
        }

        long maxDataPoints = 0;
        try {
            maxDataPoints = Long.parseLong(request.getMaxDataPoints());
        } catch (NumberFormatException ex) {
            String errStr = "Invalid request format. Cannot parse maxDataPoints == " + request.getMaxDataPoints();
            LOG.error(errStr);
            return Response.status(Response.Status.BAD_REQUEST).entity(toJson(errStr)).build();
        }

        if (maxDataPoints < 1) {
            // Return the points without any aggregation
            final GetTSDRMetricsInputBuilder input = new GetTSDRMetricsInputBuilder();
            input.setTSDRDataCategory(request.getTarget());
            input.setStartTime(from);
            input.setEndTime(until);

            Future<RpcResult<GetTSDRMetricsOutput>> metric = metricDataService.getTSDRMetrics(input.build());

            if (metric == null || !metric.get().isSuccessful()) {
                String errStr = "Error retrieving metrics from " + fromString + " to " + untilString;
                LOG.error(errStr);
                return Response.status(Status.SERVICE_UNAVAILABLE).entity(errStr).build();
            }

            final GetTSDRMetricsOutput result = metric.get().getResult();

            if (result == null) {
                String errStr = "Metric result is null from " + fromString + " to " + untilString;
                LOG.error(errStr);
                return Response.status(Status.SERVICE_UNAVAILABLE).entity(errStr).build();
            } else {
                List<Metrics> metrics = result.getMetrics();
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

            if (metric == null || !metric.get().isSuccessful()) {
                String errStr = "Error retrieving metrics from " + fromString + " to " + untilString;
                LOG.error(errStr);
                return Response.status(Status.SERVICE_UNAVAILABLE).entity(errStr).build();
            }

            List<AggregatedMetrics> metrics = metric.get().getResult().getAggregatedMetrics();

            if (metrics == null) {
                String errStr = "Error retrieving aggregated metrics. from " + fromString + " to " + untilString;
                LOG.error(errStr);
                return Response.status(Status.SERVICE_UNAVAILABLE).entity(errStr).build();
            } else {
                for (AggregatedMetrics m : metrics) {
                    reply.addDataPoint(m.getTimeStamp(),
                            m.getMetricValue() != null ? m.getMetricValue().doubleValue() : null);
                }
            }
        }

        if (reply.getDatapoints().size() < 1) {
            String errStr = "No data points were found from " + fromString + " to " + untilString;
            return Response.status(Status.OK).entity(errStr).build();
        }

        return Response.status(Status.OK).entity(toJson(new TSDRNbiReply[] { reply })).build();
    }

    public static long getTimeFromString(String str) throws NumberFormatException {
        if (str == null) {
            return System.currentTimeMillis();
        }
        int index1 = str.indexOf("-");
        if (index1 != -1) {
            int index2 = str.indexOf("min");
            if (index2 != -1) {
                int min = Integer.parseInt(str.substring(index1 + 1, index2).trim());
                return System.currentTimeMillis() - min * 60000L;
            }
            index2 = str.indexOf("h");
            if (index2 != -1) {
                int hours = Integer.parseInt(str.substring(index1 + 1, index2).trim());
                return System.currentTimeMillis() - hours * 3600000L;
            }
            index2 = str.indexOf("d");
            if (index2 != -1) {
                int days = Integer.parseInt(str.substring(index1 + 1, index2).trim());
                return System.currentTimeMillis() - days * 86400000L;
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
