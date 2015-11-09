package org.opendaylight.controller.config.yang.config.TSDR_datapurge.impl;

import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.tsdr.datapurge.TSDRPurgeServiceImpl;
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

    /**
     * createInstance() is used for plugging in logics when TSDRDatapurge
     * module is created.
     */

    @Override
    public java.lang.AutoCloseable createInstance() {
        log.debug("TSDR Purge Entering createIntance()");

        final TSDRPurgeServiceImpl tsdrPurgeServiceImpl = new TSDRPurgeServiceImpl(getDataBrokerDependency(), getRpcRegistryDependency());

        /*
         * Currently there are no rpc function to register
         */
        //final BindingAwareBroker.RpcRegistration<tsdrPurgeServiceImpl> rpcRegistration = getRpcRegistryDependency()
         //   .addRpcImplementation(TSDRPurgeServiceImpl.class,
          //  tsdrPurgeServiceImpl);

        final class CloseResources implements AutoCloseable {

            @Override
            public void close() throws Exception {
                log.info("TSDRDataPurge (instance {}) torn down.", this);
                tsdrPurgeServiceImpl.shutdown();
            }
        }
        AutoCloseable ret = new CloseResources();
        log.info("TSDRDataPurge (instance {}) initialized.", ret);
        return ret;
    }
}
