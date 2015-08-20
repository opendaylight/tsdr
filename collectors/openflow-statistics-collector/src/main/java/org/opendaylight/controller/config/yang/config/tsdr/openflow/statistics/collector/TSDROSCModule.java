package org.opendaylight.controller.config.yang.config.tsdr.openflow.statistics.collector;

import org.opendaylight.tsdr.osc.TSDRDOMCollector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.openflow.statistics.collector.rev150820.TsdrOpenflowStatisticsCollectorService;

public class TSDROSCModule extends org.opendaylight.controller.config.yang.config.tsdr.openflow.statistics.collector.AbstractTSDROSCModule {

    private TSDRDOMCollector domCollector = null;

    public TSDROSCModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public TSDROSCModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.tsdr.openflow.statistics.collector.TSDROSCModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        domCollector = new TSDRDOMCollector(getDataBrokerDependency(),getRpcRegistryDependency());
        getRpcRegistryDependency().addRpcImplementation(TsdrOpenflowStatisticsCollectorService.class,domCollector);
        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                domCollector.shutdown();
            }
        };
    }
}
