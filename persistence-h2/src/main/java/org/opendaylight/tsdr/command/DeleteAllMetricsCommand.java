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
import org.opendaylight.tsdr.service.TsdrJpaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This command is useful during debugging to check the
 * working of the JPA persistence store and
 * should not be exposed in the production environment
 * by not exposing the same in blueprint.xml
 *
 * @author <a href="mailto:syedbahm@cisco.com">Basheeruddin Ahmed</a>
 *
 */

@Command(scope = "metric", name = "deleteAll", description = "Delete all metrics")
public class DeleteAllMetricsCommand extends OsgiCommandSupport {
    private final Logger
        log = LoggerFactory.getLogger(DeleteAllMetricsCommand.class);
    private TsdrJpaService persistenceService;

    public void setPersistenceService(TsdrJpaService persistenceService) {
        this.persistenceService = persistenceService;
    }


    @Override protected Object doExecute() throws Exception {
        if(persistenceService !=null) {
            persistenceService.deleteAll();
        }else{
           log.warn("DeleteAllMetricsCommand: persistence service is found to be null.");
        }
        return null;
    }
}
