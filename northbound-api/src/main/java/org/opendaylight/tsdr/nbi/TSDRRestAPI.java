/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.nbi;

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

import org.opendaylight.controller.config.yang.config.tsdr.northbound.api.TSDRNBIModule;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetMetricInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetMetricOutput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.getmetric.output.Metrics;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
@Path("/query")
public class TSDRRestAPI {
    private static final Logger logger = LoggerFactory.getLogger(TSDRRestAPI.class);
    @GET
    @Path("/{render}")
    @Produces("application/json")
    public Response get(@PathParam("render") String render,
            @QueryParam("target") String target,
            @QueryParam("from") String from,
            @QueryParam("until") String until,
            @QueryParam("format") String format,
            @QueryParam("maxDataPoints") String maxDataPoints) {
        //Example query from Grafana
        //Get render?target=EXTERNAL.Heap:Memory:Usage.Controller&from=-5min&until=now&format=json&maxDataPoints=1582
        TSDRRequest request = new TSDRRequest();
        request.setFormat(format);
        request.setFrom(from);
        request.setMaxDataPoints(maxDataPoints);
        request.setTarget(target);
        request.setUntil(until);
        return post(null,request);
    }

    @POST
    @Produces("application/json")
    public Response post(@Context UriInfo info, TSDRRequest request) {
        TSDRReply reply = new TSDRReply();
        reply.setTarget(request.getTarget());
        GetMetricInputBuilder input = new GetMetricInputBuilder();
        input.setName(request.getTarget());
        input.setFrom(getTimeFromString(request.getFrom()));
        input.setUntil(getTimeFromString(request.getUntil()));
        long maxDataPoints = 0;
        try {
            maxDataPoints = Long.parseLong(request.getMaxDataPoints());
        } catch (Exception err) {
        }
        if (maxDataPoints == 0){
            return Response.status(201).entity(reply).build();
        }
        Future<RpcResult<GetMetricOutput>> metric = TSDRNBIModule.tsdrService.getMetric(input.build());
        try {
            List<Metrics> metrics = metric.get().getResult().getMetrics();
            if (metrics == null || metrics.size() == 0) {
                return Response.status(201).entity(reply).build();
            }
            int skip = 1;
            if (metrics.size() > maxDataPoints) {
                skip = (int) (metrics.size() / maxDataPoints);
            }
            reply.setTarget(request.getTarget());
            int count = 0;
            for (Metrics m : metrics) {
                if (count % skip == 0) {
                    reply.addDataPoint(m.getTime(), m.getValue().doubleValue());
                }
                count++;
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Failed to execute request",e);
        }

        return Response.status(201).entity(new TSDRReply[]{reply}).build();
    }

    public static long getTimeFromString(String t) {
        if (t == null)
            return System.currentTimeMillis();
        int index1 = t.indexOf("-");
        if (index1 != -1) {
            int index2 = t.indexOf("min");
            if (index2 != -1) {
                int min = Integer.parseInt(t.substring(index1 + 1, index2)
                        .trim());
                return System.currentTimeMillis() - (min * 60000);
            }
            index2 = t.indexOf("h");
            if (index2 != -1) {
                int h = Integer
                        .parseInt(t.substring(index1 + 1, index2).trim());
                return System.currentTimeMillis() - (h * 3600000);
            }
            index2 = t.indexOf("d");
            if (index2 != -1) {
                int d = Integer
                        .parseInt(t.substring(index1 + 1, index2).trim());
                return System.currentTimeMillis() - (d * 86400000);
            }
        } else if (t.equals("now")) {
            return System.currentTimeMillis();
        }
        return Long.parseLong(t) * 1000;
    }
}
