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

import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.tsdr.dataquery.nontested.model.MetricRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A container for a list of MetricRecord objects. These objects loosely
 * represent the TSDR data objects returned from the TSDR data store.
 * This object is returned to the client in the Jersey Response object.
 *
 * @author <a href="mailto:smelton2@uccs.edu">Scott Melton</a>
 */

@XmlRootElement(name = "MetricsResponse")
public class MetricsResponse {
    private static final Logger log = LoggerFactory.getLogger(MetricsResponse.class);
    private String timeStamp;
    private List<MetricRecord> metricRecordList = null;

    public MetricsResponse() {
    }

    public MetricsResponse(String timeStamp, List<MetricRecord> metricRecordList) {
        this.timeStamp = timeStamp;
        this.metricRecordList = metricRecordList;
    }

    public void addMetricRecord(MetricRecord metricRecord) {
        if (metricRecord != null) {
            getMetricRecordList().add(metricRecord);
        } else {
            log.debug("MetricsResponse.addMetricRecord() Attempt to add null MetricRecord");
        }
    }

    public List<MetricRecord> getMetricRecordList() {
        if (metricRecordList == null) {
            metricRecordList = new ArrayList<MetricRecord>();
        }
        return metricRecordList;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public void setMetricRecordList(List<MetricRecord> metricRecordList) {
        this.metricRecordList = metricRecordList;
    }

    public String toMetricsResponseString() {
        return "MetricsResponse [timeStamp=" + timeStamp + ", metricRecordList=" + metricRecordList + "]";
    }
}
