/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.syslogs;

import java.lang.reflect.Field;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.yang.config.tsdr_syslog_collector.TSDRSyslogModule;
import org.opendaylight.controller.config.yang.config.tsdr_syslog_collector.TSDRSyslogModuleFactory;

/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class ModuleCodeCoverageTest {
    @Test
    public void codeCoverageModule(){
        TSDRSyslogModuleFactory mf = new TSDRSyslogModuleFactory();
        TSDRSyslogModule module1 = new TSDRSyslogModule(null,null);
        TSDRSyslogModule module2 = new TSDRSyslogModule(null,null);
        setupModuleWithMocks(module1);
        setupModuleWithMocks(module2);
        AutoCloseable c = module1.createInstance();
        module1.equals(new TSDRSyslogModule(null,null));
        module1.customValidation();
        module1.canReuseInstance(module2);
        module1.getRpcRegistry();
        module1.customValidation();
        module1.getLogger();
        module1.getIdentifier();
        module1.setRpcRegistry(null);
        module1.reuseInstance(c);
        module1.hashCode();
        try{
            module1.isSame(null);
        }catch(Exception err){}
        module1.isSame(module2);
    }

    public static final void setupModuleWithMocks(Object obj){
        try {
            org.opendaylight.controller.sal.binding.api.RpcProviderRegistry rpcProviderRegistry = Mockito.mock(org.opendaylight.controller.sal.binding.api.RpcProviderRegistry.class);
            ModuleIdentifier id = Mockito.mock(ModuleIdentifier.class);

            Field f =findField("rpcRegistryDependency",obj.getClass());
            f.set(obj,rpcProviderRegistry);

            f =findField("identifier",obj.getClass());
            f.set(obj,id);

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static final Field findField(String name,Class c){
        Field fields[] = c.getDeclaredFields();
        for(Field f:fields){
            if(f.getName().equals(name)){
                f.setAccessible(true);
                return f;
            }
        }
        if(c.getSuperclass()==null) {
            return null;
        }
        return findField(name,c.getSuperclass());
    }
}
