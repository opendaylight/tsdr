package org.opendaylight.controller.config.yang.config.tsdr.controller.metrics.collector;

import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TSDRECSModule extends org.opendaylight.controller.config.yang.config.tsdr.controller.metrics.collector.AbstractTSDRECSModule implements AutoCloseable{

    private static final Logger logger = LoggerFactory.getLogger(TSDRECSModule.class);
    private TSDRService tsdrService = null;
    boolean running = true;

    public TSDRECSModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public TSDRECSModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.tsdr.controller.metrics.collector.TSDRECSModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        logger.info("Controller Metrics Collector started!");
        return this;
    }

    @Override
    public void close() throws Exception {
        running = false;
        logger.info("Controller Metrics Collector stopped!");
    }

    public TSDRService getTSDRService(){
        if(tsdrService==null){
            tsdrService = getRpcRegistryDependency().getRpcService(TSDRService.class);
        }
        return this.tsdrService;
    }

    public boolean isRunning() {
        return running;
    }

}
