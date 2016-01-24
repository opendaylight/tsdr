/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.dataquery.rest.query;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.gettsdrlogrecords.output.Logs;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrlog.RecordAttributes;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;

/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/

@XmlRootElement(name = "TSDRMetricsQueryReply")
public class TSDRLogQueryReply {
    private final int recordCount;
    private final List<LogRecords> logRecords = new ArrayList<>();

    public TSDRLogQueryReply(List<Logs> logs){
        this.recordCount = logs.size();
        for (Logs l : logs) {
            logRecords.add(new LogRecords(l));
        }
    }

    public static class LogRecords {
        private final String recordFullText;
        private final String timeStamp;
        private final String nodeID;
        private final String tsdrDataCategory;
        private final List<LogRecordKeys> recordKeys = new ArrayList<>();
        private final List<LogRecordAttributes> recordAttributes = new ArrayList<>();

        public LogRecords(Logs mr) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(mr.getTimeStamp());
            this.timeStamp = c.getTime().toString();
            this.nodeID = mr.getNodeID();
            this.tsdrDataCategory = mr.getTSDRDataCategory().name();
            this.recordFullText = mr.getRecordFullText();
            if (mr.getRecordKeys() != null) {
                for (RecordKeys rk : mr.getRecordKeys()) {
                    recordKeys.add(new LogRecordKeys(rk));
                }
            }
            if (mr.getRecordAttributes() != null) {
                for (RecordAttributes rk : mr.getRecordAttributes()) {
                    recordAttributes.add(new LogRecordAttributes(rk));
                }
            }
        }
    }
    public static class LogRecordKeys {
        private final String keyName;
        private final String keyValue;

        public LogRecordKeys(RecordKeys r) {
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

    public static class LogRecordAttributes {
        private final String name;
        private final String value;

        public LogRecordAttributes(RecordAttributes r) {
            this.name = r.getName();
            this.value = r.getValue();
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }
    }
}
