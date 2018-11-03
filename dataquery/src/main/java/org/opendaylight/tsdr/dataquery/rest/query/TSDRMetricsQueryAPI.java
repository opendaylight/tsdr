/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.dataquery.rest.query;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import org.opendaylight.tsdr.dataquery.rest.nbi.TSDRNbiRestAPI;
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
 * Metrics query REST endpoint.
 *
 * @author Sharon Aicler(saichler@gmail.com)
 * @author Scott Melton (smelton2@uccs.edu)
 */
@Path("/metrics")
public class TSDRMetricsQueryAPI {

    private static final Logger LOG = LoggerFactory.getLogger(TSDRMetricsQueryAPI.class);
    private final TsdrMetricDataService metricDataService;

    public TSDRMetricsQueryAPI(TsdrMetricDataService newMetricDataService) {
        metricDataService = newMetricDataService;
    }

    @GET
    @Path("/{query}")
    @Produces("application/json")
    public Response get(@PathParam("query") String query, @QueryParam("tsdrkey") String tsdrkey,
            @QueryParam("from") String from, @QueryParam("until") String until,
            @QueryParam("maxDataPoints") String maxDataPoints, @QueryParam("aggregation") String aggregation)
            throws ExecutionException, InterruptedException {

        TSDRQueryRequest request = new TSDRQueryRequest();
        request.setTsdrkey(tsdrkey);
        request.setFrom(from);
        request.setUntil(until);
        request.setMaxDataPoints(maxDataPoints);
        request.setAggregation(aggregation);

        return post(null, request);
    }

    @POST
    @Produces("application/json")
    public Response post(@Context UriInfo info, TSDRQueryRequest request)
            throws ExecutionException, InterruptedException {

        long from = 0;
        long until = 0;
        final String fromString = request.getFrom();
        final String untilString = request.getUntil();

        try {
            from = TSDRNbiRestAPI.getTimeFromString(fromString);
        } catch (NumberFormatException ex) {
            String errStr = "Invalid request format. Cannot parse start time == " + fromString;
            return Response.status(Status.BAD_REQUEST).entity(toJson(errStr)).build();
        }

        try {
            until = TSDRNbiRestAPI.getTimeFromString(untilString);
        } catch (NumberFormatException ex) {
            String errStr = "Invalid request format. Cannot parse end time == " + untilString;
            return Response.status(Status.BAD_REQUEST).entity(toJson(errStr)).build();
        }

        if (request.getMaxDataPoints() != null && request.getAggregation() != null) {

            long maxDataPoints = 0;
            try {
                maxDataPoints = Long.parseLong(request.getMaxDataPoints());
            } catch (NumberFormatException ex) {
                String errStr = "Invalid request format. Cannot parse maxDataPoints == " + request.getMaxDataPoints();
                return Response.status(Status.BAD_REQUEST).entity(toJson(errStr)).build();
            }

            final GetTSDRAggregatedMetricsInputBuilder input = new GetTSDRAggregatedMetricsInputBuilder();
            input.setTSDRDataCategory(request.getTsdrkey());
            input.setStartTime(from);
            input.setEndTime(until);
            input.setInterval(Math.floorDiv(until - from, maxDataPoints) + 1);
            input.setAggregation(AggregationType.valueOf(request.getAggregation()));

            Future<RpcResult<GetTSDRAggregatedMetricsOutput>> metric = metricDataService
                    .getTSDRAggregatedMetrics(input.build());

            if (metric == null || !metric.get().isSuccessful()) {
                String errStr = "Error retrieving aggregated metrics from " + fromString + " to " + untilString;
                return Response.status(Status.OK).entity(errStr).build();
            }

            List<AggregatedMetrics> metrics = metric.get().getResult().getAggregatedMetrics();
            TSDRMetricsQueryReply reply = new TSDRMetricsQueryReply(request.getTsdrkey(), metrics);

            return Response.status(Status.BAD_REQUEST).entity(toJson(reply)).build();
        } else {
            GetTSDRMetricsInputBuilder input = new GetTSDRMetricsInputBuilder();
            input.setTSDRDataCategory(request.getTsdrkey());
            input.setStartTime(from);
            input.setEndTime(until);

            Future<RpcResult<GetTSDRMetricsOutput>> metric = metricDataService.getTSDRMetrics(input.build());

            if (metric == null || !metric.get().isSuccessful()) {
                String errStr = "Error retrieving metrics from " + fromString + " to " + untilString;
                return Response.status(Status.BAD_REQUEST).entity(errStr).build();
            }

            List<Metrics> metrics = metric.get().getResult().getMetrics();
            TSDRMetricsQueryReply reply = new TSDRMetricsQueryReply(metrics);

            return Response.status(Status.OK).entity(toJson(reply)).build();
        }
    }

    public static final String toJson(Object obj) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(obj);
    }
}
