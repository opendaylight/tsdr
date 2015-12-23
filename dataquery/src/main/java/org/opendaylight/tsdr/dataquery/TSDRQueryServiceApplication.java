/*
 * Copyright (c) 2015 Scott Melton All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.dataquery;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.core.Application;
import org.opendaylight.tsdr.dataquery.rest.GetLogRecordsAPI;
import org.opendaylight.tsdr.dataquery.rest.GetMetricsAPI;
import org.opendaylight.tsdr.dataquery.rest.TSDRLogQueryAPI;
import org.opendaylight.tsdr.dataquery.rest.TSDRMetricsQueryAPI;
import org.opendaylight.tsdr.dataquery.rest.TSDRNBIRestAPI;

/**
 * Jersey 1.17 Application - This class makes the Data Query API visible to the
 * JAX-RS implementation.
 *
 * @author <a href="mailto:smelton2@uccs.edu">Scott Melton</a>
 */
public class TSDRQueryServiceApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return new HashSet<Class<?>>(Arrays.asList(GetMetricsAPI.class, GetLogRecordsAPI.class, TSDRNBIRestAPI.class, TSDRMetricsQueryAPI.class, TSDRLogQueryAPI.class));
    }
}
