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
import javax.ws.rs.core.UriInfo;
import org.opendaylight.controller.config.yang.config.TSDR_dataquery.impl.TSDRDataqueryModule;
import org.opendaylight.tsdr.dataquery.rest.nbi.TSDRNBIRestAPI;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.AggregationType;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetTSDRAggregatedMetricsInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetTSDRAggregatedMetricsOutput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetTSDRMetricsInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetTSDRMetricsOutput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.gettsdraggregatedmetrics.output.AggregatedMetrics;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.gettsdrmetrics.output.Metrics;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                        @QueryParam("interval") Long interval,
                        @QueryParam("aggregation") String aggregation) throws ExecutionException, InterruptedException {
        TSDRQueryRequest request = new TSDRQueryRequest();
        request.setTsdrkey(tsdrkey);
        request.setFrom(from);
        request.setUntil(until);
        request.setInterval(interval);
        request.setAggregation(aggregation);
        return post(null,request);
    }

    @POST
    @Produces("application/json")
    public Response post(@Context UriInfo info, TSDRQueryRequest request) throws ExecutionException, InterruptedException {

        if (request.getInterval() != null && request.getAggregation() != null) {
            final GetTSDRAggregatedMetricsInputBuilder input = new GetTSDRAggregatedMetricsInputBuilder();
            input.setTSDRDataCategory(request.getTsdrkey());
            input.setStartTime(TSDRNBIRestAPI.getTimeFromString(request.getFrom()));
            input.setEndTime(TSDRNBIRestAPI.getTimeFromString(request.getUntil()));
            input.setInterval(request.getInterval());
            input.setAggregation(AggregationType.valueOf(request.getAggregation()));

            Future<RpcResult<GetTSDRAggregatedMetricsOutput>> metric = TSDRDataqueryModule.tsdrService.getTSDRAggregatedMetrics(input.build());

            List<AggregatedMetrics> metrics = metric.get().getResult().getAggregatedMetrics();
            TSDRMetricsQueryReply reply = new TSDRMetricsQueryReply(request.getTsdrkey(), metrics);

            return Response.status(201).entity(toJson(reply)).build();
        } else {
            GetTSDRMetricsInputBuilder input = new GetTSDRMetricsInputBuilder();
            input.setTSDRDataCategory(request.getTsdrkey());
            input.setStartTime(TSDRNBIRestAPI.getTimeFromString(request.getFrom()));
            input.setEndTime(TSDRNBIRestAPI.getTimeFromString(request.getUntil()));

            Future<RpcResult<GetTSDRMetricsOutput>> metric = TSDRDataqueryModule.tsdrService.getTSDRMetrics(input.build());

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