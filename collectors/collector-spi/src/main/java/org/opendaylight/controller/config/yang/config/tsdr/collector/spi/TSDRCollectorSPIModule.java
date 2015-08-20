package org.opendaylight.controller.config.yang.config.tsdr.collector.spi;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.tsdr.collector.spi.CollectorSPIImpl;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TSDRCollectorSPIModule extends org.opendaylight.controller.config.yang.config.tsdr.collector.spi.AbstractTSDRCollectorSPIModule {
    private static final Logger logger = LoggerFactory.getLogger(TSDRCollectorSPIModule.class);
    public TSDRCollectorSPIModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public TSDRCollectorSPIModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.tsdr.collector.spi.TSDRCollectorSPIModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        CollectorSPIImpl service = new CollectorSPIImpl(getRpcRegistryDependency().getRpcService(TSDRService.class));
        final RpcRegistration<TsdrCollectorSpiService> addRpcImplementation = getRpcRegistryDependency().addRpcImplementation(TsdrCollectorSpiService.class, service);
        logger.info("TSDR Data Collector SPI Mudule initialized");
        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                addRpcImplementation.close();
            }
        };
    }
}
