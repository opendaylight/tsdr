package org.opendaylight.controller.config.yang.config.tsdr.configuration;

import org.opendaylight.tsdr.configuration.TSDRConfiguration;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Hashtable;

public class TSDRConfigModule extends org.opendaylight.controller.config.yang.config.tsdr.configuration.AbstractTSDRConfigModule {
    private static final Logger logger = LoggerFactory.getLogger(TSDRConfigModule.class);
    private BundleContext context = null;

    public TSDRConfigModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public TSDRConfigModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.tsdr.configuration.TSDRConfigModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        registerTSDRConfiguration();
        logger.info("TSDR configuration Mudule initialized");
        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
            }
        };
    }

    public void setBundleContext(BundleContext bundleContext){
        this.context = bundleContext;
    }

    private  void registerTSDRConfiguration(){
        Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put(Constants.SERVICE_PID, "tsdr");
        context.registerService(ManagedService.class.getName(), TSDRConfiguration.getInstance() , properties);
    }
}
