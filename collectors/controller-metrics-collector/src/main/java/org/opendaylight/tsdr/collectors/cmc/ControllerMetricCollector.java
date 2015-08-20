/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.collectors.cmc;

import java.io.File;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.opendaylight.controller.config.yang.config.tsdr.controller.metrics.collector.TSDRCMCModule;
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
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class ControllerMetricCollector extends Thread{
    private static final Logger logger = LoggerFactory.getLogger(ControllerMetricCollector.class);
    private Object sigar = null;
    private URLClassLoader sigarClassLoader = null;
    private TSDRCMCModule module = null;
    private RpcProviderRegistry rpcRegistry = null;
    private static final String COLLECTOR_CODE_NAME = ControllerMetricCollector.class.getSimpleName();

    public ControllerMetricCollector(TSDRCMCModule _module, RpcProviderRegistry _rpcRegistry){
        this.module = _module;
        this.rpcRegistry = _rpcRegistry;
        this.sigar = newSigar();
        this.start();
    }

    /*
     * There is a problem with the Karaf/OSGI/Config Subsystem class loader and Sigar.
     * Sigar cannot be instantiated due to "java.lang.NoClassDefFoundError: Could not initialize class org.hyperic.sigar.Sigar"
     * Hence i had to revert to using a dedicated class loader and reflection to use the
     * functionality in the module.
     */
    private Object newSigar(){
        try {
            File sigarFile = new File("./system/org/fusesource/sigar/1.6.4/sigar-1.6.4.jar");
            sigarClassLoader = new URLClassLoader(new URL[]{sigarFile.toURL()});
            return sigarClassLoader.loadClass("org.hyperic.sigar.Sigar").newInstance();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            logger.error("Failed to instantiate Sigar");
        } catch(Error err){
            logger.error("Failed to instantiate Sigar");
        }
        return null;
    }

    public void run(){
       while(module.isRunning()){
           insertMemorySample();
           insertControllerCPUSample();
           insertMachineCPUSample();
           try{Thread.sleep(5000);}catch(Exception err){logger.error(err.getMessage(),err);}
       }
    }

    public Object getControllerCPU(){
        try{
            Method pidM = sigar.getClass().getMethod("getPid", (Class<?>[])null);
            long pid = (Long)pidM.invoke(sigar, (Object[])null);
            Method cpuM = sigar.getClass().getMethod("getProcCpu", new Class[]{long.class});
            Object procCPU = cpuM.invoke(sigar, new Object[]{pid});
            Method procM = procCPU.getClass().getMethod("getPercent",(Class<?>[])null);
            return procM.invoke(procCPU, (Object[])null);
        }catch(Exception err){
            logger.error("Failed to get Controller CPU, Sigar library is probably not installed",err);
        }
        return 0d;
    }

    public Object getMachineCPU(){
        try{
            Method cpuM = sigar.getClass().getMethod("getCpuPerc", (Class<?>[])null);
            Object cpu = cpuM.invoke(sigar, (Object[])null);
            Method combineM = cpu.getClass().getMethod("getCombined",(Class<?>[])null);
            double combine = (double)combineM.invoke(cpu, (Object[])null);
            return (long)(combine*100);
        }catch(Exception err){
            logger.error("Failed to get Machine CPU, Sigar library is probably not installed",err);
        }
        return 0l;
    }
    private void insertMemorySample(){
        TSDRMetricRecordBuilder b = new TSDRMetricRecordBuilder();
        b.setMetricName("Heap:Memory:Usage");
        b.setTSDRDataCategory(DataCategory.EXTERNAL);
        b.setNodeID("Controller");
        b.setRecordKeys(new ArrayList<RecordKeys>());
        b.setTimeStamp(System.currentTimeMillis());
        b.setMetricValue(new Counter64(new BigInteger(""+(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()))));
        InsertTSDRMetricRecordInputBuilder input = new InsertTSDRMetricRecordInputBuilder();
        List<TSDRMetricRecord> list = new LinkedList<>();
        list.add(b.build());
        input.setTSDRMetricRecord(list);
        input.setCollectorCodeName(COLLECTOR_CODE_NAME);
        module.getTSDRCollectorSPIService().insertTSDRMetricRecord(input.build());
    }

    private void insertControllerCPUSample(){
        TSDRMetricRecordBuilder b = new TSDRMetricRecordBuilder();
        b.setMetricName("CPU:Usage");
        b.setTSDRDataCategory(DataCategory.EXTERNAL);
        b.setNodeID("Controller");
        b.setRecordKeys(new ArrayList<RecordKeys>());
        b.setTimeStamp(System.currentTimeMillis());
        double cpuValue = (double)getControllerCPU();
        b.setMetricValue(new Counter64(new BigInteger(""+((int)cpuValue))));
        InsertTSDRMetricRecordInputBuilder input = new InsertTSDRMetricRecordInputBuilder();
        List<TSDRMetricRecord> list = new LinkedList<>();
        list.add(b.build());
        input.setTSDRMetricRecord(list);
        input.setCollectorCodeName(COLLECTOR_CODE_NAME);
        module.getTSDRCollectorSPIService().insertTSDRMetricRecord(input.build());
    }

    private void insertMachineCPUSample(){
        TSDRMetricRecordBuilder b = new TSDRMetricRecordBuilder();
        b.setMetricName("CPU:Usage");
        b.setTSDRDataCategory(DataCategory.EXTERNAL);
        b.setNodeID("Machine");
        b.setRecordKeys(new ArrayList<RecordKeys>());
        b.setTimeStamp(System.currentTimeMillis());
        long cpuValue = (long)getMachineCPU();
        b.setMetricValue(new Counter64(new BigInteger(""+((int)cpuValue))));
        InsertTSDRMetricRecordInputBuilder input = new InsertTSDRMetricRecordInputBuilder();
        List<TSDRMetricRecord> list = new LinkedList<>();
        list.add(b.build());
        input.setTSDRMetricRecord(list);
        input.setCollectorCodeName(COLLECTOR_CODE_NAME);
        module.getTSDRCollectorSPIService().insertTSDRMetricRecord(input.build());
    }
}
