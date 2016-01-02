/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.dataquery.rest.query;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
@XmlRootElement(name = "TSDRQueryRequest")
public class TSDRQueryRequest {
    public String tsdrkey=null;
    public String from=null;
    public String until=null;

    public String getTsdrkey() {
        return tsdrkey;
    }

    public void setTsdrkey(String tsdrkey) {
        this.tsdrkey = tsdrkey;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getUntil() {
        return until;
    }

    public void setUntil(String until) {
        this.until = until;
    }
}
