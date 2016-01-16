/*
 * Copyright (c) 2015 Scott Melton All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.dataquery.nontested.model;

import javax.xml.bind.annotation.XmlRootElement;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.gettsdrmetrics.output.Metrics;

/**
 * A container for the results returned from TSDR.  These results will
 * be transitioned to a Jersey Response class and returned to the client.
 *
  * @author <a href="mailto:smelton2@uccs.edu">Scott Melton</a>
 */

@XmlRootElement(name = "MetricResult")
public class MetricResult {
    private final Long timeStamp;
    private final Metrics metricRecordList;

    public MetricResult(Long timeStamp, Metrics metricRecordList) {
        this.timeStamp = timeStamp;
        this.metricRecordList = metricRecordList;
    }

    public Long getTimeStamp() {
        return timeStamp;
    }

    public Metrics getMetricRecord() {
        return metricRecordList;
    }
}
