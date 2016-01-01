/*
 * Copyright (c) 2015 Shoaib Rao All rights reserved.
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.datapurge;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TSDR Purge service implementation class.
 *
 * <p>
 * Schedules a recurring purge task.
 * </p>
 *
 * @author <a href="mailto:rao.shoaib@gmail.com">Shoaib Rao</a>
 *
 *         Created: Oct 19, 2015
 *         
 * @author <a href="mailto:yulingchen54@gmail.com">YuLing Chen</a>
 * 
 *         Modified: Dec 31, 2015
 */
public class TSDRPurgeServiceImpl {
    private TSDRService tsdrService = null;
    private RpcProviderRegistry rpcRegistry = null;
    private DataBroker dataBroker = null;
    private static final Logger log = LoggerFactory
            .getLogger(TSDRPurgeServiceImpl.class);

    public TSDRPurgeServiceImpl(DataBroker _dataBroker,
            RpcProviderRegistry _rpcRegistry) {
        this.dataBroker = _dataBroker;
        this.rpcRegistry = _rpcRegistry;
        if (tsdrService == null) {
            tsdrService = this.rpcRegistry.getRpcService(TSDRService.class);
        } 
        PurgingScheduler.getInstance().initAndScheduleTask(_rpcRegistry);;
    }

    public void shutdown() {
        log.debug("shutting Down TSDRPurgeServiceImpl");
        PurgingScheduler.getInstance().cancelScheduledTask();
    }

    public boolean isRunning() {
       return PurgingScheduler.getInstance().isRunning();
    }

}
