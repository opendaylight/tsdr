package org.opendaylight.controller.config.yang.config.tsdr.northbound.api;

import org.opendaylight.tsdr.nbi.TSDRNBIServiceImpl;
import org.opendaylight.tsdr.nbi.rest.TSDRRestAdapter;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.northbound.api.rev150820.TsdrNorthboundApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TSDRNBIModule extends org.opendaylight.controller.config.yang.config.tsdr.northbound.api.AbstractTSDRNBIModule
        implements AutoCloseable{

    private boolean running = true;
    private TSDRService tsdrService = null;
    private TSDRRestAdapter restAdapter = null;
    private Logger logger = LoggerFactory.getLogger(TSDRNBIModule.class);

    public TSDRNBIModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public TSDRNBIModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.tsdr.northbound.api.TSDRNBIModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        getRpcRegistryDependency().addRpcImplementation(TsdrNorthboundApiService.class,new TSDRNBIServiceImpl(getTSDRService(), getRpcRegistryDependency()));
        restAdapter = new TSDRRestAdapter(this);
        logger.info("TSDR NBI RESTfull adapter initialized");
        return this;
    }

    @Override
    public void close() throws Exception {
        logger.info("Shuting down the rest adapter.");
        this.running = false;
        this.restAdapter.shutdown();
    }

    public boolean isRunning() {
        return running;
    }

    public TSDRService getTSDRService(){
        if(tsdrService==null){
            tsdrService = getRpcRegistryDependency().getRpcService(TSDRService.class);
        }
        return this.tsdrService;
    }
}
