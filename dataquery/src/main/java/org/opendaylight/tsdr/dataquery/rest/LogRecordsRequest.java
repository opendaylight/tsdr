/*
 * Copyright (c) 2015 Scott Melton All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.dataquery.rest;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.opendaylight.tsdr.dataquery.rest.model.LogRecordSourceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:smelton2@uccs.edu">Scott Melton</a>
 */
@XmlRootElement(name = "LogRecordsRequest")
public class LogRecordsRequest {
    private static final Logger log = LoggerFactory.getLogger(LogRecordsRequest.class);
    private String subDomain;
    private String startTime;
    private String endTime;

    private List<LogRecordSourceId> logRecordSourceIdList = null;

    public LogRecordsRequest() {
    }

    public LogRecordsRequest(String startTime, String endTime, List<LogRecordSourceId> logRecordSourceIdList) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.logRecordSourceIdList = logRecordSourceIdList;
    }

    public void addLogRecordSourceId(LogRecordSourceId logRecordSourceId) {

        if (logRecordSourceId != null) {
            getLogRecordSourceIdList().add(logRecordSourceId);
        } else {
            log.debug("LogRecordsRequest.addLogRecordSourceId() was asked to add a null object to the list.");
        }
    }

    public List<LogRecordSourceId> getLogRecordSourceIdList() {
        if (this.logRecordSourceIdList == null) {
            logRecordSourceIdList = new ArrayList<LogRecordSourceId>();
        }
        return logRecordSourceIdList;
    }

    public String getSubDomain() {
        return subDomain;
    }

    public void setSubDomain(String subDomain) {
        this.subDomain = subDomain;
    }

    public void setLogRecordSourceIdList(List<LogRecordSourceId> logRecordSourceIdList) {
        this.logRecordSourceIdList = logRecordSourceIdList;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }
}
