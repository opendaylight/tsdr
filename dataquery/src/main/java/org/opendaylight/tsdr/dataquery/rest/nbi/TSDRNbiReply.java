/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.dataquery.rest.nbi;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

/**
 * Northbound reply.
 *
 * @author Sharon Aicler(saichler@gmail.com)
 */
@XmlRootElement(name = "TSDRNbiReply")
public class TSDRNbiReply {
    private String target;
    private final List<Object[]> datapoints = new ArrayList<>();

    public void addDataPoint(Long timestamp, Double value) {
        Object[] dp = new Object[2];
        String time = "" + timestamp;
        time = time.substring(0, 10);
        dp[1] = Long.parseLong(time);
        dp[0] = value;
        datapoints.add(dp);
    }

    @XmlTransient
    public List<Object[]> getDatapoints() {
        return datapoints;
    }

    public void setTarget(String target) {
        this.target = target;
    }
}
