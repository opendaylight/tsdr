/*
 * Copyright (c) 2015 Tata Consultancy Services, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.sdc;

import java.io.File;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.opendaylight.controller.config.yang.config.tsdr.snmp.data.collector.TSDRSDCModule;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Counter64;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRMetricRecordInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * @author Prajaya Talwar(prajaya.talwar@tcs.com)
 **/
public class SNMPDataCollector extends Thread{
    private static final Logger logger = LoggerFactory.getLogger(SNMPDataCollector.class);
    private TSDRSDCModule module = null;
    private RpcProviderRegistry rpcRegistry = null;
    private static final String COLLECTOR_CODE_NAME = SNMPDataCollector.class.getSimpleName();

    public SNMPDataCollector(TSDRSDCModule _module, RpcProviderRegistry _rpcRegistry){
        this.module = _module;
        this.rpcRegistry = _rpcRegistry;
        this.start();
    }

    public void run(){
       //Implementation to start from here 
    }
}
