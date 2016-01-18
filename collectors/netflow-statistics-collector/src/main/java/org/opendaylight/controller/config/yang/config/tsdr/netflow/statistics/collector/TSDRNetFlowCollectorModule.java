/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 * Copyright (c) 2015 xFlow Research Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.tsdr.netflow.statistics.collector;

import java.io.IOException;
import org.opendaylight.tsdr.netflow.TSDRNetflowCollectorImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TSDR NetFlow Collector Module.
 *
 * @author <a href="mailto:muhammad.umair@xflowresearch.com">Umair Bhatti</a>
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 *
 * Created: December 1, 2015
 */
public class TSDRNetFlowCollectorModule extends org.opendaylight.controller.config.yang.config.tsdr.netflow.statistics.collector.AbstractTSDRNetFlowCollectorModule {
    private static final Logger log = LoggerFactory
            .getLogger(TSDRNetFlowCollectorModule.class);
    public TSDRNetFlowCollectorModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public TSDRNetFlowCollectorModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.tsdr.netflow.statistics.collector.TSDRNetFlowCollectorModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        super.customValidation();
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        getDataBrokerDependency();
        TSDRNetflowCollectorImpl impl = null;
        try {
            impl = new TSDRNetflowCollectorImpl(getRpcRegistryDependency().getRpcService(TsdrCollectorSpiService.class));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ModuleAutoCloseable(impl);
    }

    private class ModuleAutoCloseable implements AutoCloseable{
        private final TSDRNetflowCollectorImpl impl;
        public ModuleAutoCloseable(TSDRNetflowCollectorImpl impl){
            this.impl = impl;
        }
        @Override
        public void close() throws Exception {
            impl.shutdown();
            log.info("TSDR NetFlow Data Collector (instance {}) torn down.", this);
        }
    }

}
