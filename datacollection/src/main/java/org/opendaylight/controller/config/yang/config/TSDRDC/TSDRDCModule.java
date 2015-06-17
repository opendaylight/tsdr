package org.opendaylight.controller.config.yang.config.TSDRDC;

import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.tsdr.datacollection.TSDRDOMCollector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdrdc.rev140523.TSDRDCService;

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
        getRpcRegistryDependency().addRpcImplementation(TSDRDCService.class,domCollector);
        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                domCollector.shutdown();
            }
        };
    }

}
