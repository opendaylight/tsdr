/*
 * Copyright (c) 2015 Scott Melton All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.dataquery.rest.model;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * The results returned from the TSDR Data Storage Service for a single
 * LogRecordsSourceId.
 *
 * @author <a href="mailto:smelton2@uccs.edu">Scott Melton</a>
 */

@XmlRootElement(name = "LogRecordResult")
public class LogRecordResult {
    private String sourceIp;
    private String protocolType;
    private String fullText;

    public String getSourceIp() {
        return sourceIp;
    }

    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
    }

    public String getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(String protocolType) {
        this.protocolType = protocolType;
    }

    public String getFullText() {
        return fullText;
    }

    public void setFullText(String fullText) {
        this.fullText = fullText;
    }
}