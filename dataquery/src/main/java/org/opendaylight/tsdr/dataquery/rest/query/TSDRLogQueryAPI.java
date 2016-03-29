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
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.GetTSDRLogRecordsInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.GetTSDRLogRecordsOutput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.gettsdrlogrecords.output.Logs;
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
@Path("/logs")
public class TSDRLogQueryAPI {
    private static final Logger logger = LoggerFactory.getLogger(TSDRNBIRestAPI.class);
    @GET
    @Path("/{query}")
    @Produces("application/json")
    public Response get(@PathParam("query") String query,
                        @QueryParam("tsdrkey") String tsdrkey,
                        @QueryParam("from") String from,
                        @QueryParam("until") String until) throws ExecutionException, InterruptedException {
        TSDRQueryRequest request = new TSDRQueryRequest();
        request.setTsdrkey(tsdrkey);
        request.setFrom(from);
        request.setUntil(until);
        return post(null,request);
    }

    @POST
    @Produces("application/json")
    public Response post(@Context UriInfo info, TSDRQueryRequest request) throws ExecutionException, InterruptedException {

        GetTSDRLogRecordsInputBuilder input = new GetTSDRLogRecordsInputBuilder();
        input.setTSDRDataCategory(request.getTsdrkey());
        input.setStartTime(TSDRNBIRestAPI.getTimeFromString(request.getFrom()));
        input.setEndTime(TSDRNBIRestAPI.getTimeFromString(request.getUntil()));

        Future<RpcResult<GetTSDRLogRecordsOutput>> metric = TSDRDataqueryModule.logDataService.getTSDRLogRecords(input.build());

        if(!metric.get().isSuccessful()){
            Response.status(503).entity("{}").build();
        }

        List<Logs> logs = metric.get().getResult().getLogs();
        TSDRLogQueryReply reply = new TSDRLogQueryReply(logs);

        return Response.status(201).entity(toJson(reply)).build();
    }

    public static final String toJson(Object obj){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(obj);
    }
}