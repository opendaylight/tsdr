/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.dataquery.rest;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
@XmlRootElement(name = "TSDRRequest")
public class TSDRRequest {
    public String from=null;
    public String until=null;
    public String target=null;
    public String maxDataPoints=null;
    public String format=null;
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
    public String getTarget() {
        return target;
    }
    public void setTarget(String target) {
        this.target = target;
    }
    public String getMaxDataPoints() {
        return maxDataPoints;
    }
    public void setMaxDataPoints(String maxDataPoints) {
        this.maxDataPoints = maxDataPoints;
    }
    public String getFormat() {
        return format;
    }
    public void setFormat(String format) {
        this.format = format;
    }
}
