package org.opendaylight.controller.config.yang.config.tsdr.snmp.data.collector;

import org.opendaylight.tsdr.sdc.SNMPDataCollector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TSDRSDCModule extends org.opendaylight.controller.config.yang.config.tsdr.snmp.data.collector.AbstractTSDRSDCModule implements AutoCloseable{
    private static final Logger logger = LoggerFactory.getLogger(TSDRSDCModule.class);
    private TsdrCollectorSpiService collectorSPIService = null;
    boolean running = true;

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
        // TODO:implement
        new SNMPDataCollector(this, getRpcRegistryDependency());
        logger.info("TSDR SNMP Data Collector initialized!");
        return this;
    }

    @Override
    public void close() throws Exception {
        running = false;
        logger.info("TSDR SNMP Data Collector Stopped!");
    }

    public TsdrCollectorSpiService getTSDRCollectorSPIService(){
        if(collectorSPIService==null){
            collectorSPIService = getRpcRegistryDependency().getRpcService(TsdrCollectorSpiService.class);
        }
        return this.collectorSPIService;
    }

    public boolean isRunning() {
        return running;
    }

}
