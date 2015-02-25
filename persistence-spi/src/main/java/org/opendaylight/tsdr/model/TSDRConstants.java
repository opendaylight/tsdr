/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.model;

/**
 * This class stores the constants that are shared among TSDR components.
 *
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 *
 * Created: Feb 24, 2015
 *
 */
public class TSDRConstants {
    /**
     * Keys used in a TSDR Record.
    */
    public static final String FLOW_TABLE_KEY_NAME = "TableID";
    public static final String FLOW_KEY_NAME = "FlowID";
    public static final String INTERNFACE_KEY_NAME = "PortID";
    public static final String QUEUE_KEY_NAME = "QueueID";
    public static final String GROUP_KEY_NAME = "GroupID";
    public static final String METER_KEY_NAME = "MeterID";
    public static final String BUCKET_KEY_NAME = "BucketID";
}
