package org.opendaylight.controller.config.yang.config.tsdr.northbound.api;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.tsdr.nbi.TSDRNBIServiceImpl;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.northbound.api.rev150820.TsdrNorthboundApiService;

public class TSDRNBIModule extends org.opendaylight.controller.config.yang.config.tsdr.northbound.api.AbstractTSDRNBIModule {
    public static TSDRService tsdrService = null;
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
        tsdrService = this.getRpcRegistryDependency().getRpcService(TSDRService.class);        
        final RpcRegistration<TsdrNorthboundApiService> service = getRpcRegistryDependency().addRpcImplementation(TsdrNorthboundApiService.class,new TSDRNBIServiceImpl(tsdrService, getRpcRegistryDependency()));
        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                service.close();
            }
        };
    }
}
