/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.command;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.tsdr.entity.Metric;
import org.opendaylight.tsdr.service.TsdrJpaService;
import org.opendaylight.tsdr.service.impl.TsdrH2PersistenceServiceImpl;
import org.opendaylight.tsdr.spi.persistence.TsdrPersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This command is available only in default datastore wherein
 * user wants to try out the TSDR feature. This command helps
 * in purging all collected data.
 *
 * @author <a href="mailto:syedbahm@cisco.com">Basheeruddin Ahmed</a>
 *
 */

@Command(scope = "tsdr", name = "purgeAll", description = "Delete all metrics")
public class DeleteAllMetricsCommand extends OsgiCommandSupport {
    private final Logger
        log = LoggerFactory.getLogger(DeleteAllMetricsCommand.class);
    private TsdrPersistenceService persistenceService;

    public void setPersistenceService(TsdrPersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }


    @Override protected Object doExecute() throws Exception {
        if(persistenceService !=null) {
            log.info("purgeAll command invoked from console");
            ((TsdrH2PersistenceServiceImpl) persistenceService).getJpaService().deleteAll();
        }
        else {
            log.warn("purge all command: persistence service is found to be null.");
        }
        return null;
    }
}
