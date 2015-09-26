package org.opendaylight.controller.config.yang.config.TSDR_datapurge.impl;
public class TSDRDatastorageModule extends org.opendaylight.controller.config.yang.config.TSDR_datapurge.impl.AbstractTSDRDatastorageModule {
    public TSDRDatastorageModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public TSDRDatastorageModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.TSDR_datapurge.impl.TSDRDatastorageModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        // TODO:implement
        // To do: to start a cron job to periodically call purging API from TSDR SErvice
        // Take reference of DataCollection Service TSDRDOMCollector StoringThread.store()
        throw new java.lang.UnsupportedOperationException();
    }

}
