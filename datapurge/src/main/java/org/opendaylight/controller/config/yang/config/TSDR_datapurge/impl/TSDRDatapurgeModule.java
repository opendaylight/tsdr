package org.opendaylight.controller.config.yang.config.TSDR_datapurge.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TSDRDatapurgeModule extends org.opendaylight.controller.config.yang.config.TSDR_datapurge.impl.AbstractTSDRDatapurgeModule {
 private static final Logger log = LoggerFactory
        .getLogger(TSDRDatapurgeModule.class);

public TSDRDatapurgeModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public TSDRDatapurgeModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.TSDR_datapurge.impl.TSDRDatapurgeModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        // TODO:implement

        final class CloseResources implements AutoCloseable {

            @Override
            public void close() throws Exception {


            }
        }

        AutoCloseable ret = new CloseResources();
        log.info("TSDRDatapurge (instance {}) initialized.", ret);
        return ret;
    }

}
