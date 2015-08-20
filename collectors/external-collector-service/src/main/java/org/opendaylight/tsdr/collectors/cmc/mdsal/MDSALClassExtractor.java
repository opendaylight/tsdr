/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.collectors.cmc.mdsal;

import org.datasand.codec.TypeDescriptor;
import org.datasand.codec.observers.IClassExtractorObserver;
import org.opendaylight.yangtools.yang.binding.DataObject;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
public class MDSALClassExtractor implements IClassExtractorObserver{

    @Override
    public Class<?> getObjectClass(Object obj) {
        if(obj instanceof DataObject){
            return ((DataObject)obj).getImplementedInterface();
        }
        return obj.getClass();
    }

    @Override
    public Class<?> getBuilderClass(TypeDescriptor td) {
        if(DataObject.class.isAssignableFrom(td.getTypeClass())){
            try{
                return getClass().getClassLoader().loadClass(td.getTypeClassName()+"Builder");
            }catch(Exception err){}
        }
        return null;
    }

    @Override
    public String getBuilderMethod(TypeDescriptor td) {
        if(DataObject.class.isAssignableFrom(td.getTypeClass())){
            return "build()";
        }
        return null;
    }
}
