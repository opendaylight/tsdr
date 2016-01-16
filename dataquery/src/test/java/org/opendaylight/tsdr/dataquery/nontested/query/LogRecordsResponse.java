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

import org.opendaylight.tsdr.dataquery.nontested.model.LogRecordResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Container for a list of LogRecordResult objects.
 *
 * @author <a href="mailto:smelton2@uccs.edu">Scott Melton</a>
 */

//{
//    "logrecords:results": {
//        "timestamp": "10/15/2015 13:00:00",
//        {
//            "source-IP": "10.0.0.1",
//            "protocol-type": HTTP
//        },
//        "FullText":"HTTP packets sent from source-IP 10.0.0.1 and destination-IP 10.0.0.2"
//
//    }
//}

@XmlRootElement(name = "LogRecordsResponse")
public class LogRecordsResponse {
    private static final Logger log = LoggerFactory.getLogger(LogRecordsResponse.class);
    private String timeStamp;
    private List<LogRecordResult> logRecordsResultList = null;

    public LogRecordsResponse() {
    }

    public LogRecordsResponse(String timeStamp, List<LogRecordResult> logRecordsResultList) {
        this.timeStamp = timeStamp;
        this.logRecordsResultList = logRecordsResultList;
    }

    /**
     * @param logRecordsResult - logRecordsResult
     */
    public void addLogRecordResult(LogRecordResult logRecordsResult) {
        if (logRecordsResult != null) {
            getLogRecordReplyList().add(logRecordsResult);
        } else {
            log.debug("LogRecordsRequest.addLogRecordResult() Attempt to add null MetricResult");
        }
    }

    /**
     * @return LogRecordResult
     */
    public List<LogRecordResult> getLogRecordReplyList() {
        if (this.logRecordsResultList == null) {
            logRecordsResultList = new ArrayList<LogRecordResult>();
        }
        return logRecordsResultList;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }
}
