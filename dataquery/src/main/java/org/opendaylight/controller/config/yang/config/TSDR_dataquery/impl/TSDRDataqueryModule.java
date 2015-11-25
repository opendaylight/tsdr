/*
 * Copyright (c) 2015 Scott Melton All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.TSDR_dataquery.impl;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.tsdr.dataquery.TSDRNBIServiceImpl;
import org.opendaylight.tsdr.dataquery.TSDRQueryServiceImpl;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.dataquery.impl.rev150219.TSDRDataqueryImplService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TSDRDataqueryModule extends AbstractTSDRDataqueryModule {

    private static final Logger log = LoggerFactory.getLogger(TSDRDataqueryModule.class);
    public static TSDRService tsdrService = null;
    public static TSDRQueryServiceImpl tsdrQueryServiceImpl;

    /**
     * Constructor
     *
     * @param identifier - the identifier
     * @param dependencyResolver - the dependencyResolver
     */
    public TSDRDataqueryModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    /**
     * Constructor
     *
     * @param identifier - the identifier
     * @param dependencyResolver - the dependencyResolver
     * @param oldModule - oldModule
     * @param oldInstance - oldInstance
     */
    public TSDRDataqueryModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            org.opendaylight.controller.config.yang.config.TSDR_dataquery.impl.TSDRDataqueryModule oldModule,
            java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    /**
     * Custom Validation logic.
     */
    @Override
    public void customValidation() {

    }

    /**
     * createInstance() is used for plugging in logic when TSDRDataquery module is
     * created.
     */
    @Override
    public java.lang.AutoCloseable createInstance() {
        /**
         * Get the tsdrService from the Registry so the Data Query API can query
         * the TSDR Data Storage Service.
         */
        tsdrService = this.getRpcRegistryDependency().getRpcService(TSDRService.class);
        tsdrQueryServiceImpl = new TSDRQueryServiceImpl(tsdrService, getRpcRegistryDependency());

        final TSDRNBIServiceImpl nbiService = new TSDRNBIServiceImpl(tsdrService, getRpcRegistryDependency());
        final RpcRegistration<TSDRDataqueryImplService> serviceRegistation = getRpcRegistryDependency()
                .addRpcImplementation(TSDRDataqueryImplService.class, nbiService);

        final class CloseResources implements AutoCloseable {

            @Override
            public void close() throws Exception {
                log.info("TSDRQueryService (instance {}) torn down.", this);
                serviceRegistation.close();
            }
        }

        AutoCloseable ret = new CloseResources();
        log.info("TSDRQueryService (instance {}) initialized.", ret);
        return ret;
    }
}
