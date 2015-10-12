package org.opendaylight.controller.config.yang.config.tsdr.netflow.statistics.collector;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        // TODO:implement
        final class CloseResources implements AutoCloseable {

            @Override
            public void close() throws Exception {
                log.info("TSDR NetFlow Data Collector (instance {}) torn down.", this);
            }
        }
        AutoCloseable ret = new CloseResources();
        log.info("NetFlow Data Colletor Initialized");
        return ret;
    }

}
