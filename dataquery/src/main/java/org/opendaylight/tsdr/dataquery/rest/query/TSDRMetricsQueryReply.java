/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.dataquery.rest.query;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.gettsdraggregatedmetrics.output.AggregatedMetrics;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.gettsdrmetrics.output.Metrics;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;

/**
 * Metrics query reply.
 *
 * @author Sharon Aicler(saichler@gmail.com)
 */
@XmlRootElement(name = "TSDRMetricsQueryReply")
@SuppressFBWarnings("URF_UNREAD_FIELD")
public class TSDRMetricsQueryReply {

    private final List<MetricRecord> metricRecords = new ArrayList<>();

    @SuppressWarnings("unused")
    private final int recordCount;

    public TSDRMetricsQueryReply(List<Metrics> metricList) {
        this.recordCount = metricList.size();
        for (Metrics m : metricList) {
            metricRecords.add(new MetricRecord(m));
        }
    }

    public TSDRMetricsQueryReply(String tsdrDataCategory, List<AggregatedMetrics> metricList) {
        this.recordCount = 0;
        for (AggregatedMetrics m : metricList) {
            metricRecords.add(new MetricRecord(tsdrDataCategory, m));
        }
    }

    public static class MetricRecord {
        @SuppressWarnings("unused")
        private final String metricName;

        @SuppressWarnings("unused")
        private final BigDecimal metricValue;

        @SuppressWarnings("unused")
        private final String timeStamp;

        @SuppressWarnings("unused")
        private final String nodeID;

        @SuppressWarnings("unused")
        private final String tsdrDataCategory;

        private final List<MetricRecordKeys> recordKeys = new ArrayList<>();

        public MetricRecord(Metrics mr) {
            this.metricName = mr.getMetricName();
            this.metricValue = mr.getMetricValue();
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(mr.getTimeStamp());
            this.timeStamp = calendar.getTime().toString();
            this.nodeID = mr.getNodeID();
            this.tsdrDataCategory = mr.getTSDRDataCategory().name();
            if (mr.getRecordKeys() != null) {
                for (RecordKeys rk : mr.getRecordKeys()) {
                    recordKeys.add(new MetricRecordKeys(rk));
                }
            }
        }

        public MetricRecord(String tsdrDataCategory, AggregatedMetrics mr) {
            this.metricValue = mr.getMetricValue();
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(mr.getTimeStamp());
            this.timeStamp = calendar.getTime().toString();
            this.tsdrDataCategory = tsdrDataCategory;
            this.nodeID = null;
            this.metricName = null;
        }
    }

    public static class MetricRecordKeys {
        private final String keyName;
        private final String keyValue;

        public MetricRecordKeys(RecordKeys keys) {
            this.keyName = keys.getKeyName();
            this.keyValue = keys.getKeyValue();
        }

        public String getKeyName() {
            return keyName;
        }

        public String getKeyValue() {
            return keyValue;
        }
    }
}
