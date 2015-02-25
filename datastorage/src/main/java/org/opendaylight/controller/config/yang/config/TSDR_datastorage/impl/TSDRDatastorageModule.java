/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.TSDR_datastorage.impl;


import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.tsdr.datastorage.TSDRStorageServiceImpl;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRService;
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

    private static final Logger log = LoggerFactory
        .getLogger(TSDRDatastorageModule.class);

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

    }

    @Override
    public boolean canReuse(Module oldModule) {
        return true;
    }

    /**
     * createInstance() is used for plugging in logics when TSDRDatastorage
     * module is created.
     */
    @Override
    public java.lang.AutoCloseable createInstance() {
        log.debug("Entering createIntance()");
        /*
         * The implementation of TSDRStorageservice.
        */
        final TSDRStorageServiceImpl tsdrDataStorageServiceImpl = new TSDRStorageServiceImpl();
        /*
         * Register the implementation class of TSDRDatastorage service in the
         * RPC registry.
         */
        final BindingAwareBroker.RpcRegistration<TSDRService> rpcRegistration = getRpcRegistryDependency()
            .addRpcImplementation(TSDRService.class,
                tsdrDataStorageServiceImpl);

        final class CloseResources implements AutoCloseable {

            @Override
            public void close() throws Exception {
                log.info("TSDRDataStorage (instance {}) torn down.", this);
                // Call close() on data storage service to clean up the data store.
                tsdrDataStorageServiceImpl.close();
            }
        }

        AutoCloseable ret = new CloseResources();
        log.info("TSDRDataStorage (instance {}) initialized.", ret);
        return ret;
     }

}
