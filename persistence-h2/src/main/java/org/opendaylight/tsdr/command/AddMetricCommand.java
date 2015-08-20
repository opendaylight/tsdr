/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.command;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.tsdr.entity.Metric;
import org.opendaylight.tsdr.service.TsdrJpaService;
import org.opendaylight.tsdr.service.impl.TsdrH2PersistenceServiceImpl;
import org.opendaylight.tsdr.spi.persistence.TsdrPersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * This command is useful during debugging to check the
 * working of the JPA persistence store and
 * should not be exposed in the production environment
 * by not exposing the same in blueprint.xml
 *
 * @author <a href="mailto:syedbahm@cisco.com">Basheeruddin Ahmed</a>
 *
 */

@Command(scope = "tsdr", name = "add", description = "Adds a metric")
public class AddMetricCommand extends OsgiCommandSupport {
    private final Logger
        log = LoggerFactory.getLogger(AddMetricCommand.class);

    @Argument(index=0, name="Metric Name", required=true, description="name of the metric", multiValued=false)
    String metricName;
    @Argument(index=1, name="Metric Value", required=true, description="Value of metric (real number)", multiValued=false)
    Double metricValue;
    private TsdrPersistenceService persistenceService;

    public void setPersistenceService(TsdrPersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    @Override protected Object doExecute() throws Exception {
        if(persistenceService != null) {
            Metric metric = new Metric(new Date(), metricName, metricValue);
            ((TsdrH2PersistenceServiceImpl)persistenceService).getJpaService().add(metric);
            return null;
        }else{
            log.warn("AddMetricCommand: persistence service is found to be null.");
        }
        return null;
    }
}
