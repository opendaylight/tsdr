/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.dataquery.rest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.gettsdrmetrics.output.Metrics;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;

/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/

@XmlRootElement(name = "TSDRMetricsQueryReply")
public class TSDRMetricsQueryReply {

    private final String metricName;
    private final BigDecimal metricValue;
    private final String timeStamp;
    private final String nodeID;
    private final String tsdrDataCategory;
    private final List<MetricRecordKeys> recordKeys = new ArrayList<>();

    public TSDRMetricsQueryReply(Metrics mr){
        this.metricName = mr.getMetricName();
        this.metricValue = mr.getMetricValue();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(mr.getTimeStamp());
        this.timeStamp = c.getTime().toString();
        this.nodeID = mr.getNodeID();
        this.tsdrDataCategory = mr.getTSDRDataCategory().name();
        if(mr.getRecordKeys()!=null){
            for(RecordKeys rk:mr.getRecordKeys()){
                recordKeys.add(new MetricRecordKeys(rk));
            }
        }
    }

    public String getMetricName() {
        return metricName;
    }

    public BigDecimal getMetricValue() {
        return metricValue;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public String getNodeID() {
        return nodeID;
    }

    public String getTSDRDataCategory() {
        return tsdrDataCategory;
    }

    public List<MetricRecordKeys> getRecordKeys() {
        return recordKeys;
    }

    public static class MetricRecordKeys {
        private final String keyName;
        private final String keyValue;

        public MetricRecordKeys(RecordKeys r) {
            this.keyName = r.getKeyName();
            this.keyValue = r.getKeyValue();
        }

        public String getKeyName() {
            return keyName;
        }

        public String getKeyValue() {
            return keyValue;
        }
    }
}
