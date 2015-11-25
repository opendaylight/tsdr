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

import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.tsdr.dataquery.rest.model.MetricId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//
//Class MetricsRequest structure, container for the TSDR request attributes
// {
//     "input": {
//         "metric-id": {
//            {"name":"categoryName", "value": "FlowStats"},
//            { "name": "NodeID", "value": "Openflow:1"},
//            {"name": "TableID", "value": "Table1"},
//            {"name": "FlowID", "value": "114"}
//          }
//          "start-time" : "13 Oct 2015 00:00:00 PST"
//          "end-time" : "14 Oct 2015 00:00:00 PST"
//     }
// }
//

/**
 * MetricsRequest is a container for a single REST query to the TSDR data store.
 * This query is delimited by the start and end time of the query and
 * a list of query criteria (MetricId).  MetricsRequest will be marshalled into
 * the TSDR GetTSDRMetricsInput request object.
 *
 * @author <a href="mailto:smelton2@uccs.edu">Scott Melton</a>
 */
@XmlRootElement(name = "MetricsRequest")
public class MetricsRequest {
    private static final Logger log = LoggerFactory.getLogger(MetricsRequest.class);

    private String subDomain;
    private Long startTime;
    private Long endTime;
    private List<MetricId> metricIdList = null;

    public MetricsRequest() {
    }

    public MetricsRequest(String subDomain, Long startTime, Long endTime, List<MetricId> metricIdList) {
        this.subDomain = subDomain;
        this.startTime = startTime;
        this.endTime = endTime;
        this.metricIdList = metricIdList;
    }

    public void addMetricId(MetricId metricId) {
        if (metricId != null) {
            getMetricIdList().add(metricId);
        } else {
            log.debug("TSDR Data Query MetricsRequest.addMetricId() rejected a null object.");
        }
    }

    public List<MetricId> getMetricIdList() {
        if (this.metricIdList == null) {
            metricIdList = new ArrayList<MetricId>();
        }
        return metricIdList;
    }

    public void setMetricIdList(List<MetricId> metricIdList) {
        this.metricIdList = metricIdList;
    }

    public String getSubDomain() {
        return subDomain;
    }

    public String toMetricsRequestString() {
        return "MetricsRequest [subDomain=" + subDomain + ", startTime=" + startTime + ", endTime=" + endTime
                + ", metricIdList=" + metricIdList + "]";
    }

    public void setSubDomain(String subDomain) {
        this.subDomain = subDomain;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }
}
