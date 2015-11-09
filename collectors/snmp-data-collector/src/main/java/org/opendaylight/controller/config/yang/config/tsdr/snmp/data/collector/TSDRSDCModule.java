package org.opendaylight.controller.config.yang.config.tsdr.snmp.data.collector;


import org.opendaylight.tsdr.sdc.SNMPDataCollector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.snmp.data.collector.rev151013.TsdrSnmpDataCollectorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TSDRSDCModule extends org.opendaylight.controller.config.yang.config.tsdr.snmp.data.collector.AbstractTSDRSDCModule{
    
    private static final Logger logger = LoggerFactory.getLogger(TSDRSDCModule.class);

    boolean running = true;
    private SNMPDataCollector snmpCollector = null;

    public TSDRSDCModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public TSDRSDCModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.tsdr.snmp.data.collector.TSDRSDCModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        logger.info("SNMP Data Collector started!");
        snmpCollector = new SNMPDataCollector(getDataBrokerDependency(),getRpcRegistryDependency());
        getRpcRegistryDependency().addRpcImplementation(TsdrSnmpDataCollectorService.class, snmpCollector);
        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
            snmpCollector.shutdown();
                running = false;
                logger.info("SNMP Data Collector stopped!");
            }
        };
    }

}
