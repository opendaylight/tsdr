/**
 * Copyright (c) 2015 Scott Melton All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.dataquery.rest;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author <a href="mailto:smelton2@uccs.edu">Scott Melton</a>
 */

/**
 * Request input: metric-id CategoryName == FlowStats NodeID == openflow:1
 * TableID = Table1 FlowID = 114 String start-time String end-time
 */
// {
// "input": {
// "metric-id": {
// {"name":"categoryName", "value": "FlowStats"},
// { "name": "NodeID", "value": "Openflow:1"},
// {"name": "TableID", "value": "Table1"},
// {"name": "FlowID", "value": "114"}
// }
// "start-time" : "13 Oct 2015 00:00:00 PST"
// "end-time" : "14 Oct 2015 00:00:00 PST"
// }
// }

@XmlRootElement(name = "TSDRMetricsRequest")
public class TSDRMetricsRequest {
    private String categoryName;
    private String nodeID;
    private String tableID;
    private String flowID;
    private String startTime;
    private String stopTime;

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getNodeID() {
        return nodeID;
    }

    public void setNodeID(String nodeID) {
        this.nodeID = nodeID;
    }

    public String getTableID() {
        return tableID;
    }

    public void setTableID(String tableID) {
        this.tableID = tableID;
    }

    public String getFlowID() {
        return flowID;
    }

    public void setFlowID(String flowID) {
        this.flowID = flowID;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String from) {
        this.startTime = from;
    }

    public String getStopTime() {
        return stopTime;
    }

    public void setStopTime(String until) {
        this.stopTime = until;
    }
}
