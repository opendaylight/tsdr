/**
 * Copyright (c) 2015 Scott Melton All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.dataquery;

import java.util.concurrent.Future;

import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetTSDRMetricsInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.GetTSDRMetricsOutput;

import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TSDR Query Service implementation class.
 *
 * This class is used by the REST data query framework to retrieve data from the
 * Storage Service.
 *
 * @author <a href="mailto:smelton2@uccs.edu">Scott Melton</a>
 */
public class TSDRQueryServiceImpl {
    private static final Logger log = LoggerFactory.getLogger(TSDRQueryServiceImpl.class);

    public Future<RpcResult<GetTSDRMetricsOutput>> getTSDRMetrics(GetTSDRMetricsInput input) {
        log.debug("entering TSDRQueryService.getTSDRMetrics()");
        return null;
    }
}
