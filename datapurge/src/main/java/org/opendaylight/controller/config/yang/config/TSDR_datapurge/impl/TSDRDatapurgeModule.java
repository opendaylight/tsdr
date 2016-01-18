/*
 * Copyright (c) 2015 Shoaib Rao All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.TSDR_datapurge.impl;

import java.util.Hashtable;
import org.opendaylight.tsdr.datapurge.TSDRDataPurgeConfig;
import org.opendaylight.tsdr.datapurge.TSDRPurgeServiceImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TSDRDatapurgeModule extends org.opendaylight.controller.config.yang.config.TSDR_datapurge.impl.AbstractTSDRDatapurgeModule {
    private static final Logger log = LoggerFactory.getLogger(TSDRDatapurgeModule.class);
    private BundleContext bundleContext = null;

    public TSDRDatapurgeModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public TSDRDatapurgeModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.TSDR_datapurge.impl.TSDRDatapurgeModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        super.customValidation();
    }

    /**
     * createInstance() is used for plugging in logics when TSDRDatapurge
     * module is created.
     */

    @Override
    public java.lang.AutoCloseable createInstance() {
        log.debug("TSDR Purge Entering createIntance()");
        registerConfiguration();
        final TSDRPurgeServiceImpl tsdrPurgeServiceImpl = new TSDRPurgeServiceImpl(getDataBrokerDependency(), getRpcRegistryDependency());

        /*
         * Currently there are no rpc function to register
         */
        //final BindingAwareBroker.RpcRegistration<tsdrPurgeServiceImpl> rpcRegistration = getRpcRegistryDependency()
         //   .addRpcImplementation(TSDRPurgeServiceImpl.class,
          //  tsdrPurgeServiceImpl);

        final class CloseResources implements AutoCloseable {

            @Override
            public void close() throws Exception {
                log.info("TSDRDataPurge (instance {}) torn down.", this);
                tsdrPurgeServiceImpl.shutdown();
            }
        }
        AutoCloseable ret = new CloseResources();
        log.info("TSDRDataPurge (instance {}) initialized.", ret);
        return ret;
    }

    public void setBundleContext(BundleContext b){
        this.bundleContext = b;
    }

    private  void registerConfiguration(){
        Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put(Constants.SERVICE_PID, "tsdr.data.purge");
        bundleContext.registerService(ManagedService.class.getName(), TSDRDataPurgeConfig.getInstance() , properties);
    }
}
