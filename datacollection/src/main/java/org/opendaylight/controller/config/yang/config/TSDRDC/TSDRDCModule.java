/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.yang.config.TSDRDC;

/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.tsdr.datacollection.TSDRDOMCollector;

public class TSDRDCModule extends org.opendaylight.controller.config.yang.config.TSDRDC.AbstractTSDRDCModule {

    private TSDRDOMCollector domCollector = null;

    public TSDRDCModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public TSDRDCModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.TSDRDC.TSDRDCModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        domCollector = new TSDRDOMCollector(getDataBrokerDependency(),getRpcRegistryDependency());
        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
            }
        };
    }
    @Override
    public boolean canReuse(Module arg0) {
        return true;
    }
}
