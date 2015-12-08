package org.opendaylight.controller.config.yang.config.tsdr.snmp.data.collector;


import java.util.Hashtable;
import org.opendaylight.tsdr.sdc.SNMPDataCollector;
import org.opendaylight.tsdr.sdc.TSDRSNMPConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.snmp.data.collector.rev151013.TsdrSnmpDataCollectorService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class TSDRSnmpDataCollectorModule extends org.opendaylight.controller.config.yang.config.tsdr.snmp.data.collector.AbstractTSDRSnmpDataCollectorModule {
    private static final Logger logger = LoggerFactory.getLogger(TSDRSnmpDataCollectorModule.class);

    boolean running = true;
    private SNMPDataCollector snmpCollector = null;
    private BundleContext bundleContext = null;
    public TSDRSnmpDataCollectorModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public TSDRSnmpDataCollectorModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.tsdr.snmp.data.collector.TSDRSnmpDataCollectorModule oldModule, java.lang.AutoCloseable oldInstance) {
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
        registerConfiguration();

        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
            snmpCollector.shutdown();
                running = false;
                logger.info("SNMP Data Collector stopped!");
            }
        };
    }

    public void setBundleContext(BundleContext c){
        this.bundleContext = c;
    }

    private  void registerConfiguration(){
        Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put(Constants.SERVICE_PID, "tsdr-snmp");
        bundleContext.registerService(ManagedService.class.getName(), TSDRSNMPConfig.getInstance() , properties);
    }
}
