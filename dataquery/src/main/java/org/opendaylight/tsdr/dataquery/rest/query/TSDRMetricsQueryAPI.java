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
import org.opendaylight.controller.config.yang.config.TSDR_dataquery.impl.TSDRDataqueryModule;
import org.opendaylight.tsdr.dataquery.rest.nbi.TSDRNBIRestAPI;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.*;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.gettsdraggregatedmetrics.output.AggregatedMetrics;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.gettsdrmetrics.output.Metrics;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
@Path("/metrics")
public class TSDRMetricsQueryAPI {
    private static final Logger logger = LoggerFactory.getLogger(TSDRNBIRestAPI.class);
    @GET
    @Path("/{query}")
    @Produces("application/json")
    public Response get(@PathParam("query") String query,
                        @QueryParam("tsdrkey") String tsdrkey,
                        @QueryParam("from") String from,
                        @QueryParam("until") String until,
                        @QueryParam("maxDataPoints") String maxDataPoints,
                        @QueryParam("aggregation") String aggregation) throws ExecutionException, InterruptedException {
        TSDRQueryRequest request = new TSDRQueryRequest();
        request.setTsdrkey(tsdrkey);
        request.setFrom(from);
        request.setUntil(until);
        request.setMaxDataPoints(maxDataPoints);
        request.setAggregation(aggregation);
        return post(null,request);
    }

    @POST
    @Produces("application/json")
    public Response post(@Context UriInfo info, TSDRQueryRequest request) throws ExecutionException, InterruptedException {

        if (request.getMaxDataPoints() != null && request.getAggregation() != null) {
            final long from = TSDRNBIRestAPI.getTimeFromString(request.getFrom());
            final long until = TSDRNBIRestAPI.getTimeFromString(request.getUntil());
            final long maxDataPoints = request.getMaxDataPoints() != null ? Long.parseLong(request.getMaxDataPoints()) : 0;
            final GetTSDRAggregatedMetricsInputBuilder input = new GetTSDRAggregatedMetricsInputBuilder();
            input.setTSDRDataCategory(request.getTsdrkey());
            input.setStartTime(TSDRNBIRestAPI.getTimeFromString(request.getFrom()));
            input.setEndTime(TSDRNBIRestAPI.getTimeFromString(request.getUntil()));
            input.setInterval(Math.floorDiv(until - from, maxDataPoints) + 1);
            input.setAggregation(AggregationType.valueOf(request.getAggregation()));

            Future<RpcResult<GetTSDRAggregatedMetricsOutput>> metric = TSDRDataqueryModule.metricDataService.getTSDRAggregatedMetrics(input.build());

            if(!metric.get().isSuccessful()){
                Response.status(503).entity("{}").build();
            }

            List<AggregatedMetrics> metrics = metric.get().getResult().getAggregatedMetrics();
            TSDRMetricsQueryReply reply = new TSDRMetricsQueryReply(request.getTsdrkey(), metrics);

            return Response.status(201).entity(toJson(reply)).build();
        } else {
            GetTSDRMetricsInputBuilder input = new GetTSDRMetricsInputBuilder();
            input.setTSDRDataCategory(request.getTsdrkey());
            input.setStartTime(TSDRNBIRestAPI.getTimeFromString(request.getFrom()));
            input.setEndTime(TSDRNBIRestAPI.getTimeFromString(request.getUntil()));

            Future<RpcResult<GetTSDRMetricsOutput>> metric = TSDRDataqueryModule.metricDataService.getTSDRMetrics(input.build());

            if(!metric.get().isSuccessful()){
                Response.status(503).entity("{}").build();
            }

            List<Metrics> metrics = metric.get().getResult().getMetrics();
            TSDRMetricsQueryReply reply = new TSDRMetricsQueryReply(metrics);

            return Response.status(201).entity(toJson(reply)).build();
        }
    }

    public static final String toJson(Object obj){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(obj);
    }
}