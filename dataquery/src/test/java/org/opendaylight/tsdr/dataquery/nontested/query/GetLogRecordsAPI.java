/*
 * Copyright (c) 2015 Scott Melton All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.dataquery.nontested.query;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.opendaylight.tsdr.dataquery.nontested.model.LogRecordSourceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/query/logrecords")
public class GetLogRecordsAPI {
    private static final Logger log = LoggerFactory.getLogger(GetLogRecordsAPI.class);

    @Context
    UriInfo uriInfo; // Contains the REST query and path parameters supplied by Jersey

    @GET
    @Path("/{getlogrecords}")  // /Web-ContextPath/path
    @Produces("application/xml")  // data type returned to the client
    public Response getLogRecords() {

        //ToDo: will add integration with backend in the next patchset.
        // Build the Data Query requests using the client rest query parameters.
        //        List<LogRecordsRequest> logRecord = buildLogRecordsRequests();

        //
        //        for (MultivaluedMap.Entry<String, List<String>> entry : uriInfo.getQueryParameters().entrySet()) {
        //            log.debug("Key: %s, Value: %s\n", entry.getKey(), entry.getValue());
        //        }
        //
        //        List<LogRecordsRequest> requests = new ArrayList<LogRecordsRequest>();
        //
        //        LogRecordsRequest request = new LogRecordsRequest();
        //
        //        LogRecordSourceId logRecordSourceId = new LogRecordSourceId(category, ipAddress);
        //        request.addLogRecordSourceId(logRecordSourceId);
        //        requests.add(request);
        //
        //        return processGetLogRecords(null, request);
        return null;
    }

    /**
     * Parse the client rest query parameters and build the Data Query requests.
     * These requests will be marshalled into TSDR requests in TSDRQueryServiceImpl.
     *
     * @param queryParams - rest client attributes
     * @return LogRecordList - LogRecordList
     */
    private List<LogRecordSourceId> buildLogRecordSourceIdRequests() {
        MultivaluedMap<String, String> queryParams = this.uriInfo.getQueryParameters();
        List<LogRecordSourceId> logRecordSourceIdList = new ArrayList<LogRecordSourceId>();

        //ToDo: Will add integration with backend in the next patch set.
        //        LogRecordSourceId logRecordSourceId = new LogRecordSourceId();
        //        List<MetricId> metricIdList = new ArrayList<MetricId>();
        //        MetricId metricId = new MetricId();

        //        if (queryParams.getFirst("startTime") != null) {
        //            logRecordSourceId.setStartTime(Long.parseLong(queryParams.getFirst("startTime")));
        //        }
        //        if (queryParams.getFirst("endTime") != null) {
        //            logRecordSourceId.setEndTime(Long.parseLong(queryParams.getFirst("endTime")));
        //        }
        //        if (queryParams.getFirst("categoryName") != null) {
        //            metricId.setCategoryName(queryParams.getFirst("categoryName"));
        //        }
        //        if (queryParams.getFirst("NodeID") != null) {
        //            metricId.setNodeId(queryParams.getFirst("NodeID"));
        //        }
        //        if (queryParams.getFirst("TableID") != null) {
        //            metricId.setTableId(queryParams.getFirst("TableID"));
        //        }
        //        if (queryParams.getFirst("FlowID") != null) {
        //            metricId.setFlowId(queryParams.getFirst("FlowID"));
        //        }

        //        metricIdList.add(metricId);
        //        LogRecordSourceId.setMetricIdList(metricIdList);
        //        LogRecordSourceIdList.add(LogRecordSourceId);
        //
        //        for (LogRecordSourceId metricRequest : LogRecordSourceIdList) {
        //            log.debug(metricRequest.toString());
        //        }
        //
        return null;
    }
}
