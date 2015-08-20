/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.collectors.cmc.mdsal;

import java.lang.reflect.Method;

import org.datasand.codec.AttributeDescriptor;
import org.datasand.codec.observers.IMethodFilterObserver;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
public class MDSALMethodFilter implements IMethodFilterObserver{

    @Override
    public boolean isValidModelMethod(Method m) {
        if (m.getName().equals("getImplementedInterface"))
            return false;
        if(m.getReturnType().isArray() && m.getReturnType().getComponentType().equals(boolean.class))
            return false;
        return true;
    }

    @Override
    public boolean isValidAttribute(AttributeDescriptor ad) {
        if(ad.getReturnType().getName().equals("org.opendaylight.yangtools.yang.binding.Identifier"))
            return false;
         return true;
    }

}
