/*
 * Copyright (c) 2015 Scott Melton All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.dataquery;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import javax.ws.rs.core.Application;
import org.opendaylight.tsdr.dataquery.rest.nbi.TSDRNbiRestAPI;
import org.opendaylight.tsdr.dataquery.rest.query.TSDRLogQueryAPI;
import org.opendaylight.tsdr.dataquery.rest.query.TSDRMetricsQueryAPI;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.TsdrLogDataService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.TsdrMetricDataService;

/**
 * Jersey 1.17 Application - This class makes the Data Query API visible to the
 * JAX-RS implementation.
 *
 * @author <a href="mailto:smelton2@uccs.edu">Scott Melton</a>
 */
public class TSDRQueryServiceApplication extends Application {
    private final TsdrMetricDataService metricDataService;
    private final TsdrLogDataService logDataService;

    public TSDRQueryServiceApplication(TsdrMetricDataService metricDataService, TsdrLogDataService logDataService) {
        this.metricDataService = metricDataService;
        this.logDataService = logDataService;
    }

    @Override
    public Set<Object> getSingletons() {
        return ImmutableSet.of(new TSDRNbiRestAPI(metricDataService), new TSDRMetricsQueryAPI(metricDataService),
                new TSDRLogQueryAPI(logDataService));
    }
}
