/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.controller.config.yang.config.TSDR_datastorage.impl;


import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.tsdr.datastorage.TSDRStorageServiceImpl;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.StoreTSDRLogRecordInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.StoreTSDRLogRecordInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.TSDRService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.gettsdrmetrics.output.TSDRMetricRecordList;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrlogrecord.input.TSDRLogRecordBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrlog.RecordAttributes;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrlog.RecordAttributesBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
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

        /*testing of the getTSDRMetricsRector API*/
      /*  GetTSDRMetricsInputBuilder inputBuilder =  new GetTSDRMetricsInputBuilder();
        inputBuilder.setTSDRDataCategory(DataCategory.FLOWTABLESTATS);
        inputBuilder.setStartTime(Long.MIN_VALUE);
        inputBuilder.setEndTime(System.currentTimeMillis());
        GetTSDRMetricsInput input = inputBuilder.build();
        try{
        Future<RpcResult<GetTSDRMetricsOutput>>  output = tsdrDataStorageServiceImpl.getTSDRMetrics(input);
        List<TSDRMetricRecordList> metricRecords = output.get().getResult().getTSDRMetricRecordList();
        for (TSDRMetricRecordList mr : metricRecords) {
            log.info("YuLing== GetTSDRMetricsOutput.NodeID = " + mr.getNodeID());
            log.info("YuLing== GetTSDRMetricsOutput.metricName = " + mr.getMetricName());
            log.info("YuLing== GetTSDRMetricsOutput.metricValue = " + mr.getMetricValue());
            List<RecordKeys> recordKeyList = mr.getRecordKeys();
            for ( RecordKeys recordKey: recordKeyList){
                log.info("YuLing== GetTSDRMetricsOutput.keyName = " + recordKey.getKeyName());
                log.info("YuLing== GetTSDRMetricsOutput.keyValue = " + recordKey.getKeyValue());
            }
        }
        }catch(Exception e){
            log.error("Exception caught");
        }*/

        /*testing of the storeTSDRLogRecord API*/
      /*  LinkedList<TSDRLogRecord> recordList = new LinkedList<TSDRLogRecord>();
        TSDRLogRecordBuilder recordbuilder = new TSDRLogRecordBuilder();
        recordbuilder.setNodeID("10.0.0.0.2");
        recordbuilder.setTimeStamp(System.currentTimeMillis());
        recordbuilder.setTSDRDataCategory(DataCategory.NETFLOW);
        recordbuilder.setRecordFullText("This is a test of NetFlow data");
        RecordKeysBuilder recordKeysBuilder = new RecordKeysBuilder();
        recordKeysBuilder.setKeyName("portName");
        recordKeysBuilder.setKeyValue("port1");
        ArrayList<RecordKeys> recordKeyList = new ArrayList<RecordKeys>();
        recordKeyList.add(recordKeysBuilder.build());
        recordbuilder.setRecordKeys(recordKeyList);
        RecordAttributesBuilder attributeBuilder = new RecordAttributesBuilder();
        attributeBuilder.setName("srcAddr");
        attributeBuilder.setValue("10.8.8.3");
        List<RecordAttributes> attributeList = new ArrayList<RecordAttributes>();
        attributeList.add(attributeBuilder.build());
        recordbuilder.setRecordAttributes(attributeList);
        TSDRLogRecord record = recordbuilder.build();
        recordList.add(record);
          try{
          StoreTSDRLogRecordInputBuilder builder = new StoreTSDRLogRecordInputBuilder();
          builder.setTSDRLogRecord(recordList);
          StoreTSDRLogRecordInput input = builder.build();
           tsdrDataStorageServiceImpl.storeTSDRLogRecord(input);
           log.info("YuLing== storeTSDRLogRecord completed " );
          }catch(Exception e){
              log.error("Exception caught");
          }*/
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
