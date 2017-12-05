/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.dataquery.rest.query;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Query request.
 *
 * @author Sharon Aicler(saichler@gmail.com)
 */
@XmlRootElement(name = "TSDRQueryRequest")
public class TSDRQueryRequest {
    public String tsdrkey;
    public String from;
    public String until;
    public String maxDataPoints;
    public String aggregation;

    public String getTsdrkey() {
        return tsdrkey;
    }

    public void setTsdrkey(String tsdrkey) {
        this.tsdrkey = tsdrkey;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getUntil() {
        return until;
    }

    public void setUntil(String until) {
        this.until = until;
    }

    public String getMaxDataPoints() {
        return maxDataPoints;
    }

    public void setMaxDataPoints(String maxDataPoints) {
        this.maxDataPoints = maxDataPoints;
    }

    public String getAggregation() {
        return aggregation;
    }

    public void setAggregation(String aggregation) {
        this.aggregation = aggregation;
    }
}
