/*
 * Copyright (c) 2015 Scott Melton All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.dataquery.rest.model;

import javax.xml.bind.annotation.XmlRootElement;

//
//Class MetricsRequest structure, container for the TSDR request attributes
//{
//   "input": {
//       "metric-id": {
//          {"name":"categoryName", "value": "FlowStats"},
//          { "name": "NodeID", "value": "Openflow:1"},
//          {"name": "TableID", "value": "Table1"},
//          {"name": "FlowID", "value": "114"}
//        }
//        "start-time" : "13 Oct 2015 00:00:00 PST"
//        "end-time" : "14 Oct 2015 00:00:00 PST"
//   }
//}
//

/**
 * Search criteria supplied in MetricsRequest.
 * MetricRequest and these MetridId criteria are marshalled into
 * GetTSDRMetricsInput in the TSDRQueryServiceImpl where the data storage
 * call is made.
 *
 * @author <a href="mailto:smelton2@uccs.edu">Scott Melton</a>
 */
@XmlRootElement(name = "MetricId")
public class MetricId {
    private String categoryName;
    private String nodeId;
    private String tableId;
    private String flowId;

    public MetricId() {
    }

    /**
     * Constructor
     * @param categoryName -The category of the query.
     * @param nodeId - The nodeId of the query.
     * @param tableId - The tableId of the query.
     * @param flowId - The flowId of the query.
     */
    public MetricId(String categoryName, String nodeId, String tableId, String flowId) {
        this.categoryName = categoryName;
        this.nodeId = nodeId;
        this.tableId = tableId;
        this.flowId = flowId;
    }

    public String toMetricIdString() {
        return "MetricId [categoryName=" + categoryName + ", nodeId=" + nodeId + ", tableId=" + tableId + ", flowId="
                + flowId + "]";
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getTableId() {
        return tableId;
    }

    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }
}
