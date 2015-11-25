/*
 * Copyright (c) 2015 Scott Melton All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.dataquery.rest.model;

import java.math.BigDecimal;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * MetricsRecord is used to contain the return results from the TSDR query that
 * will be returned to the Data Query client.
 *
 * MetricsRecord is the Data Query equivalent to the TSDRMetricRecordList
 * returned inside the GetTSDRMetricsOutput of the TSDR Data Storage Service call:
 *
 * @author <a href="mailto:smelton2@uccs.edu">Scott Melton</a>
 */
@XmlRootElement(name = "MetricRecord")
public class MetricRecord {
    private String metricName;
    private BigDecimal metricValue;
    private BigDecimal timeStamp;
    private String nodeId;
    private String categoryName;
    private List<RecordKeysCombination> recordKeys;

    public MetricRecord() {
    }

    /**
     * Constructor
     * @param metricName -The metricName of the query.
     * @param metricValue -The metricValue of the query.
     * @param timeStamp -The timeStamp of the query.
     * @param nodeId -The nodeId of the query.
     * @param categoryName -The categoryName of the query.
     * @param tableId -The tableId of the query.
     * @param flowId -The flowId of the query.
     * @param recordKeys -The recordKeys of the query.
     */
    public MetricRecord(String metricName, BigDecimal metricValue, BigDecimal timeStamp, String nodeId,
            String categoryName, String tableId, String flowId, List<RecordKeysCombination> recordKeys) {
        this.metricName = metricName;
        this.metricValue = metricValue;
        this.timeStamp = timeStamp;
        this.nodeId = nodeId;
        this.categoryName = categoryName;
        this.recordKeys = recordKeys;
    }

    public String toMetricRecordString() {
        return "MetricRecord [metricName=" + metricName + ", metricValue=" + metricValue + ", timeStamp=" + timeStamp
                + ", nodeId=" + nodeId + ", categoryName=" + categoryName + ", recordKeys=" + recordKeys + "]";
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public BigDecimal getMetricValue() {
        return metricValue;
    }

    public void setMetricValue(BigDecimal metricValue) {
        this.metricValue = metricValue;
    }

    public BigDecimal getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(BigDecimal timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public List<RecordKeysCombination> getRecordKeys() {
        return recordKeys;
    }

    public void setRecordKeys(List<RecordKeysCombination> recordKeys) {
        this.recordKeys = recordKeys;
    }

}
