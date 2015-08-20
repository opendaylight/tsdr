/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.collectors.cmc;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.controller.config.yang.config.tsdr.controller.metrics.collector.TSDRECSModule;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.StoreTSDRMetricRecordInputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class ExternalCollectorServiceImpl implements ICollectorService{

    private static Logger log = LoggerFactory.getLogger(ExternalCollectorServiceImpl.class);
    private TSDRECSModule module = null;

    public ExternalCollectorServiceImpl(TSDRECSModule _module){
        this.module = _module;
        CollectorService.getInstance().setLocalCollectorService(this);
        CollectorService.getInstance().initAsServer();
        log.info("External Collector Service Initialized");
    }

    @Override
    public void store(TSDRMetricRecord record) {
        List<TSDRMetricRecord> list = new ArrayList<TSDRMetricRecord>(1);
        list.add(record);
        store(record);
    }

    @Override
    public void store(List<TSDRMetricRecord> recordList) {
        StoreTSDRMetricRecordInputBuilder b = new StoreTSDRMetricRecordInputBuilder();
        b.setTSDRMetricRecord(recordList);
        module.getTSDRService().storeTSDRMetricRecord(b.build());
    }
}
