/*
 * Copyright (c) 2015 Scott Melton All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.dataquery.rest.model;

import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//{
//    "input": {
//        "logsource-id": {
//          {"name": "category", "value": "SYSLOG"},
//          {"name": "ip-address", "value": "10.86.3.13"}
//        },
//        "start-time" : "11 Oct 2015 00:00:00 PST" ,
//        "end-time" : "12 Oct 2015 00:00:00 PST"
//    }
//}

/**
 * @author <a href="mailto:smelton2@uccs.edu">Scott Melton</a>
 */
@XmlRootElement(name = "LogRecordSourceId")
public class LogRecordSourceId {
    private static final Logger log = LoggerFactory.getLogger(LogRecordSourceId.class);

    private final String categoryName;
    private final String ipAddress;

    /**
     * Constructor
     * @param categoryName -  The category of the query.
     * @param ipAddress - IP address
     */
    public LogRecordSourceId(String categoryName, String ipAddress) {
        this.categoryName = categoryName;
        this.ipAddress = ipAddress;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getIpAddress() {
        return ipAddress;
    }
}
