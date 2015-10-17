/**
 * Copyright (c) 2015 Scott Melton All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.dataquery.rest;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author <a href="mailto:smelton2@uccs.edu">Scott Melton</a>
 */

// {
// "input": {
// "logsource-id": {
// {"name": "category", "value": "SYSLOG"},
// {"name": "ip-address", "value": "10.86.3.13"}
// },
// "start-time" : "11 Oct 2015 00:00:00 PST" ,
// "end-time" : "12 Oct 2015 00:00:00 PST"
// }
// }

@XmlRootElement(name = "TSDRLogRecordsRequest")
public class TSDRLogRecordsRequest {
    private String category;
    private String ipAddress;
    private String startTime;
    private String stopTime;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getStopTime() {
        return stopTime;
    }

    public void setStopTime(String stopTime) {
        this.stopTime = stopTime;
    }
}
