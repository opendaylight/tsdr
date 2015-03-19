/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Date;

/**
 * Represents the Metric table in store's persistent entry
 *
 * Automated id generated will be the primary key of the table
 *
 *
 * @author <a href="mailto:syedbahm@cisco.com">Basheeruddin Ahmed</a>
 *
 */

@Entity
public class Metric {
    @Id
    @GeneratedValue
    private Long id;
    private Date metricTimeStamp;
    private String metricName;
    private double metricValue;
    private String metricCategory;
    private String nodeId;
    private String info;



    public Metric() {
    }

    public Metric(Date timeStamp, String metricName, double metricValue) {
        super();
        this.metricTimeStamp = timeStamp;
        this.metricName = metricName;
        this.metricValue = metricValue;
    }

    public String getMetricName() {
        return metricName;
    }
    public void setMetricName(String name) {
        this.metricName = name;
    }

    public Date getMetricTimeStamp() {
        return metricTimeStamp;
    }

    public void setMetricTimeStamp(Date timeStamp) {
        this.metricTimeStamp = timeStamp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setMetricValue(double metricValue) {
        this.metricValue = metricValue;
    }
    public double getMetricValue() {
        return metricValue;
    }


    public String getMetricCategory() {
        return metricCategory;
    }

    public void setMetricCategory(String metricCategory) {
        this.metricCategory = metricCategory;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
}
