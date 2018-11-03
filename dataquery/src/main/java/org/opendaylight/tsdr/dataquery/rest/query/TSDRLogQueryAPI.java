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
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.GetTSDRLogRecordsInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.GetTSDRLogRecordsOutput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.TsdrLogDataService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.gettsdrlogrecords.output.Logs;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Log query REST endpoint.
 *
 * @author Sharon Aicler(saichler@gmail.com)
 * @author Scott Melton (smelton2@uccs.edu)
 **/
@Path("/logs")
public class TSDRLogQueryAPI {

    private static final Logger LOG = LoggerFactory.getLogger(TSDRLogQueryAPI.class);
    private final TsdrLogDataService logDataService;

    public TSDRLogQueryAPI(TsdrLogDataService newLogDataService) {
        logDataService = newLogDataService;
    }

    @GET
    @Path("/{query}")
    @Produces("application/json")
    public Response get(@PathParam("query") String query, @QueryParam("tsdrkey") String tsdrkey,
            @QueryParam("from") String from, @QueryParam("until") String until)
            throws ExecutionException, InterruptedException {

        TSDRQueryRequest request = new TSDRQueryRequest();
        request.setTsdrkey(tsdrkey);
        request.setFrom(from);
        request.setUntil(until);

        return post(null, request);
    }

    @POST
    @Produces("application/json")
    public Response post(@Context UriInfo info, TSDRQueryRequest request)
            throws ExecutionException, InterruptedException {

        GetTSDRLogRecordsInputBuilder input = new GetTSDRLogRecordsInputBuilder();
        input.setTSDRDataCategory(request.getTsdrkey());

        final String fromString = request.getFrom();
        final String untilString = request.getUntil();

        try {
            input.setStartTime(TSDRNbiRestAPI.getTimeFromString(fromString));
        } catch (NumberFormatException ex) {
            String errStr = "Invalid request format. Cannot parse start time == " + fromString;
            return Response.status(Response.Status.BAD_REQUEST).entity(toJson(errStr)).build();
        }

        try {
            input.setEndTime(TSDRNbiRestAPI.getTimeFromString(untilString));
        } catch (NumberFormatException ex) {
            String errStr = "Invalid request format. Cannot parse end time == " + untilString;
            return Response.status(Response.Status.BAD_REQUEST).entity(toJson(errStr)).build();
        }

        Future<RpcResult<GetTSDRLogRecordsOutput>> metric = logDataService.getTSDRLogRecords(input.build());

        if (metric == null || !metric.get().isSuccessful()) {
            String errStr = "Error retrieving metrics from " + fromString + " to " + untilString;
            return Response.status(Status.SERVICE_UNAVAILABLE).entity(errStr).build();
        }

        List<Logs> logs = metric.get().getResult().getLogs();
        TSDRLogQueryReply reply = new TSDRLogQueryReply(logs);

        return Response.status(Status.OK).entity(toJson(reply)).build();
    }

    public static final String toJson(Object obj) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(obj);
    }
}
