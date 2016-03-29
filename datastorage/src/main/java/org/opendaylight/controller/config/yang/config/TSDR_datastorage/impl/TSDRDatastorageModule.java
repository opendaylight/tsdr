/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.TSDR_datastorage.impl;


import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.tsdr.datastorage.TSDRStorageServiceImpl;
import org.opendaylight.tsdr.spi.persistence.TSDRBinaryPersistenceService;
import org.opendaylight.tsdr.spi.persistence.TSDRLogPersistenceService;
import org.opendaylight.tsdr.spi.persistence.TSDRMetricPersistenceService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.TsdrLogDataService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.TsdrMetricDataService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TSDRDataStorage Service is a MD-SAL service. This TSDRDataStorageModule contains
 * life cycle management methods for plugging in related logic when the service is
 * managed as a MD-SAL service.
 *
 * @author <a href="mailto:yuling_c@dell.com">YuLing Chen</a>
 *
 * Created: March 1, 2015
 */
public class TSDRDatastorageModule
    extends
    org.opendaylight.controller.config.yang.config.TSDR_datastorage.impl.AbstractTSDRDatastorageModule {

    private static final Logger log = LoggerFactory.getLogger(TSDRDatastorageModule.class);
    private BundleContext bundleContext = null;
    /**
     * Constructor.
     * @param identifier
     * @param dependencyResolver
    */
    public TSDRDatastorageModule(
        org.opendaylight.controller.config.api.ModuleIdentifier identifier,
        org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    /**
     * Constructor.
     * @param identifier
     * @param dependencyResolver
     * @param oldModule
     * @param oldInstance
     */
    public TSDRDatastorageModule(
            org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            org.opendaylight.controller.config.yang.config.TSDR_datastorage.impl
                .TSDRDatastorageModule oldModule,
            java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        super.customValidation();
    }

    /**
     * createInstance() is used for plugging in logics when TSDRDatastorage
     * module is created.
     */
    @Override
    public java.lang.AutoCloseable createInstance() {
        getDataBrokerDependency();
        log.debug("Entering createIntance()");
        /*
         * The implementation of TSDRStorageservice.
        */

        final TSDRStorageServiceImpl tsdrDataStorageServiceImpl = new TSDRStorageServiceImpl(null,null);
        new ServiceLocator(tsdrDataStorageServiceImpl);
        /*
         * Register the implementation class of TSDRDatastorage service in the
         * RPC registry.
         */
        final BindingAwareBroker.RpcRegistration<TSDRService> rpcTSDRServiceRegistration = getRpcRegistryDependency().addRpcImplementation(TSDRService.class, tsdrDataStorageServiceImpl);
        final BindingAwareBroker.RpcRegistration<TsdrMetricDataService> rpcMetricServiceRegistration = getRpcRegistryDependency().addRpcImplementation(TsdrMetricDataService.class, tsdrDataStorageServiceImpl);
        final BindingAwareBroker.RpcRegistration<TsdrLogDataService> rpcLogServiceRegistration = getRpcRegistryDependency().addRpcImplementation(TsdrLogDataService.class, tsdrDataStorageServiceImpl);

        final class CloseResources implements AutoCloseable {

            @Override
            public void close() throws Exception {
                log.info("TSDRDataStorage (instance {}) torn down.", this);
                // Call close() on data storage service to clean up the data store.
                tsdrDataStorageServiceImpl.close();
                rpcTSDRServiceRegistration.close();
                rpcMetricServiceRegistration.close();
                rpcLogServiceRegistration.close();
            }
        }

        AutoCloseable ret = new CloseResources();
        log.info("TSDRDataStorage (instance {}) initialized.", ret);
        return ret;
     }

    protected void setBundleContext(BundleContext context){
        this.bundleContext = context;
    }

    private class ServiceLocator extends Thread {
        public final TSDRStorageServiceImpl impl;
        public boolean metricServiceFound = false;
        public boolean logServiceFound = false;
        public boolean binaryServiceFound = false;

        public ServiceLocator(TSDRStorageServiceImpl impl){
            this.impl = impl;
            this.setDaemon(true);
            this.start();
        }

        public void run(){
            int count = 0;
            while(!metricServiceFound || !logServiceFound || !binaryServiceFound) {
                count++;
                log.info("Attempt #{} to find persistence services",count);
                if(!metricServiceFound) {
                    final ServiceReference<TSDRMetricPersistenceService> serviceReference = bundleContext.getServiceReference(TSDRMetricPersistenceService.class);
                    if (serviceReference != null) {
                        final TSDRMetricPersistenceService metricService = bundleContext.getService(serviceReference);
                        impl.setMetricPersistenceService(metricService);
                        metricServiceFound = true;
                        log.info("TSDR Metric Persistence Service {} Was Found.",impl.getClass().getSimpleName());
                    }else{
                        log.info("TSDR Metric Persistence Service Was not Found, will attempt in 2 seconds");
                    }
                }
                if(!logServiceFound) {
                    final ServiceReference<TSDRLogPersistenceService> serviceReference = bundleContext.getServiceReference(TSDRLogPersistenceService.class);
                    if (serviceReference != null) {
                        final TSDRLogPersistenceService logService = bundleContext.getService(serviceReference);
                        impl.setLogPersistenceService(logService);
                        logServiceFound = true;
                        log.info("TSDR Log Persistence Service {} Was Found.",impl.getClass().getSimpleName());
                    }else{
                        log.info("TSDR Log Persistence Service Was not Found, will attempt in 2 seconds");
                    }
                }
                if(!binaryServiceFound) {
                    final ServiceReference<TSDRBinaryPersistenceService> serviceReference = bundleContext.getServiceReference(TSDRBinaryPersistenceService.class);
                    if (serviceReference != null) {
                        final TSDRBinaryPersistenceService binaryService = bundleContext.getService(serviceReference);
                        impl.setBinaryPersistenceService(binaryService);
                        binaryServiceFound = true;
                        log.info("TSDR Binary Persistence Service {} Was Found.",impl.getClass().getSimpleName());
                    }else{
                        log.info("TSDR Binary Persistence Service Was not Found, will attempt in 2 seconds");
                    }
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    log.error("Interrupted",e);
                    break;
                }
            }
            if(metricServiceFound && logServiceFound && binaryServiceFound){
                log.info("All TSDR Persistence Services were found.");
            }
        }
    }
}
