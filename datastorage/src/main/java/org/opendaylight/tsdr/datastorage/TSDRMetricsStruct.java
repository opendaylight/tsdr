/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.datastorage;
/**
 * This class contains the structure of each metric that needs to be
 * defined in TSDRMetricsMap for the Data Storage service to convert
 * from OpenFlow statistics data to TSDR data model.
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 *
 *    Created: March 15, 2015
 *
 */
public class TSDRMetricsStruct {

    /*
     * MetricName such as PacketCount on flow level.
     */
    private String metricName = "";
    /*
     * MethodName such as getPacketCount() in FlowStatistics class.
     */
    private String methodName = "";

    /**
     * Constructor that takes the metricName and methodName
     * @param metricName
     * @param methodName
     */
    public TSDRMetricsStruct(String metricName, String methodName){
        super();
        this.setMetricName(metricName);
        this.setMethodName(methodName);
    }

    public String getMetricName() {
        return metricName;
    }
    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }
    public String getMethodName() {
        return methodName;
    }
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

}
