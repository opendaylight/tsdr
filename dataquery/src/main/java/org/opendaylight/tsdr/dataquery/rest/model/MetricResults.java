/*
 * Copyright (c) 2015 Scott Melton All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.dataquery.rest.model;

import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.gettsdrmetrics.output.Metrics;

/**
 * A container for the multiple results returned from TSDR in TSDR objects from
 * a single REST request.  These results will be combined into a MetricsResponse
 * class and returned to the client with the Jersey Response class.
 *
 * @author <a href="mailto:smelton2@uccs.edu">Scott Melton</a>
 */

@XmlRootElement(name = "MetricResults")
public class MetricResults {
    private final Long timeStamp;
    private final List<Metrics> metricRecordList;

    public MetricResults(Long timeStamp, List<Metrics> metricRecordList) {
        super();
        this.timeStamp = timeStamp;
        this.metricRecordList = metricRecordList;
    }

    public Long getTimeStamp() {
        return timeStamp;
    }

    public List<Metrics> getMetricRecordList() {
        return metricRecordList;
    }
}
