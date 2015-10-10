package org.opendaylight.controller.config.yang.config.TSDR_datapurge.impl;

import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.PurgeTSDRRecordInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.PurgeTSDRRecordInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TSDRDatapurgeModule extends org.opendaylight.controller.config.yang.config.TSDR_datapurge.impl.AbstractTSDRDatapurgeModule {
 private static final Logger log = LoggerFactory
        .getLogger(TSDRDatapurgeModule.class);

public TSDRDatapurgeModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public TSDRDatapurgeModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.TSDR_datapurge.impl.TSDRDatapurgeModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {

        TSDRService storageService = getRpcRegistryDependency().getRpcService(TSDRService.class);
        //Please take reference of the following code to create a 'cron job' for automatic purging
        //of TSDR data
        /*PurgeTSDRRecordInputBuilder inputBuilder = new PurgeTSDRRecordInputBuilder();
        inputBuilder.setRetentionTime(System.currentTimeMillis());
        inputBuilder.setTSDRDataCategory(DataCategory.FLOWTABLESTATS);
        PurgeTSDRRecordInput input = inputBuilder.build();
        storageService.purgeTSDRRecord(input);
        log.info("YuLing===Flow Table Stats purged successfully");*/
        final class CloseResources implements AutoCloseable {

            @Override
            public void close() throws Exception {


            }
        }

        AutoCloseable ret = new CloseResources();
        log.info("TSDRDatapurge (instance {}) initialized.", ret);
        return ret;
    }

}
