/*
 * Copyright (c) 2015 Scott Melton All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.dataquery.rest;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.opendaylight.tsdr.dataquery.TSDRQueryServiceImpl;
import org.opendaylight.tsdr.dataquery.rest.model.MetricId;
import org.opendaylight.tsdr.dataquery.rest.model.MetricRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TSDR Data Query API - Metrics
 *
 * (I am working on a survey research paper on Data Query Services, specifically
 * RESTful queries of Time Series data.
 * Real comments will come after the correct design is established.)
 *
 * @author <a href="mailto:smelton2@uccs.edu">Scott Melton</a>
 */

// @GET  - specifies the http GET command (GET, POST, PUT etc)
// @Path - specifies the URI path from the Web-ContextPath(/tsdr)
//         in the org.apache.felix plugin definition.
//
// REST Query:
//
// http://(controller):8181/<Web-ContextPath>/@Path?queryparameters=0
//
// Unit test:
// Use curl with the following flags:
// -G --get  forces curl to do a GET
// -i --include  (HTTP) Include the HTTP-header in the output.
//
// curl -Gi http://localhost:8181/tsdr/query/metrics/getmetrics?
//          "categoryName=FlowStats&NodeID=openflow:1&TableID=Table1&FlowID=114
//           &startTime=1448853158&endTime=1448853558"
//
// Quotes around "queryParams" are required by curl.

@Path("/query/metrics")
public class GetMetricsAPI {
    private static final Logger log = LoggerFactory.getLogger(GetMetricsAPI.class);

    @Context
    UriInfo uriInfo; // Contains the REST query and path parameters

    @GET
    @Path("/{getmetrics}")  // /Web-ContextPath/path
    @Produces("application/xml")  // data type returned to the client
    public Response getMetrics() {

        // Build the Data Query requests using the client rest query parameters.
        List<MetricsRequest> metricsRequest = buildMetricRequests();

        // Make the TSDR call to get metric data and process it into the Data Query model.
        List<MetricsResponse> responseList = processGetMetrics(metricsRequest);
    // Log the response data from TSDR.
        for (MetricsResponse metricsResponse : responseList) {
            log.debug(metricsResponse.toMetricsResponseString());

        }

        // The correct response needs to be generified for marshalling.
        GenericEntity<List<MetricsResponse>> entity = new GenericEntity<List<MetricsResponse>>(responseList) {
        };

        // For testing always return a valid response.
        // TODO: Future work should do error handling and notifications to the client.
        return Response.ok(entity).build();
    }

    /**
     * Parse the client rest query parameters and build the Data Query requests.
     * These requests will be marshalled into TSDR requests in TSDRQueryServiceImpl.
     *
     * @param queryParams - rest query parameters supplied by the client
     * @return metricsRequestList - list of requests that define calls to the
     * TSDR Data Storage Service.
     */
    private List<MetricsRequest> buildMetricRequests() {
        MultivaluedMap<String, String> queryParams = this.uriInfo.getQueryParameters();
        List<MetricsRequest> metricsRequestList = new ArrayList<MetricsRequest>();

        MetricsRequest metricsRequest = new MetricsRequest();
        List<MetricId> metricIdList = new ArrayList<MetricId>();
        MetricId metricId = new MetricId();

        if (queryParams.getFirst("startTime") != null) {
            metricsRequest.setStartTime(Long.parseLong(queryParams.getFirst("startTime"))*1000);
        }
        if (queryParams.getFirst("endTime") != null) {
            metricsRequest.setEndTime(Long.parseLong(queryParams.getFirst("endTime"))*1000);
        }
        if (queryParams.getFirst("categoryName") != null) {
            metricId.setCategoryName(queryParams.getFirst("categoryName"));
        }
        if (queryParams.getFirst("NodeID") != null) {
            metricId.setNodeId(queryParams.getFirst("NodeID"));
        }
        if (queryParams.getFirst("TableID") != null) {
            metricId.setTableId(queryParams.getFirst("TableID"));
        }
        if (queryParams.getFirst("FlowID") != null) {
            metricId.setFlowId(queryParams.getFirst("FlowID"));
        }

        metricIdList.add(metricId);
        metricsRequest.setMetricIdList(metricIdList);
        metricsRequestList.add(metricsRequest);

        for (MetricsRequest metricRequest : metricsRequestList) {
            log.debug(metricRequest.toString());
        }

        return metricsRequestList;
    }

    /**
     * Execute and process the requests here. The TSDR data should arrive here
     * in the Data Query model. All cross model data marshalling between TSDR
     * data objects and Data Query data objects should be done in the
     * TSDRQueryServiceImpl.
     *
     * @param metricsRequests - List of requests that define calls to the
     *     TSDR Data Storage Service.
     * @return metricsResponseList - List of Data Query objects created from
     *     the results of the TSDR Data Storage SPI calls.
     */
    public static List<MetricsResponse> processGetMetrics(List<MetricsRequest> metricsRequests) {
        List<MetricsResponse> metricsResponseList = new ArrayList<MetricsResponse>();
        MetricsResponse metricsResponse = new MetricsResponse();

        List<MetricRecord> metricRecordList = TSDRQueryServiceImpl.getTSDRMetrics(metricsRequests);

        metricsResponse.setMetricRecordList(metricRecordList);
        metricsResponseList.add(metricsResponse);

        return metricsResponseList;
    }
}

//curl -GiS http://localhost:8181/tsdr/query/metrics/getmetrics?"categoryName=FlowStats&NodeID=openflow:1&TableID=Table1&FlowID=114&startTime=1448853158&endTime=1448853558"
