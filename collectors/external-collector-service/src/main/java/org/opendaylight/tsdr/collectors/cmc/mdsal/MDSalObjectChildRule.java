/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.collectors.cmc.mdsal;

import org.datasand.codec.AttributeDescriptor;
import org.datasand.codec.TypeDescriptor;
import org.datasand.codec.observers.IChildAttributeObserver;
import org.opendaylight.yangtools.yang.binding.DataObject;
/**
 * @author - Sharon Aicler (saichler@cisco.com)
 */
public class MDSalObjectChildRule implements IChildAttributeObserver{

    @Override
    public boolean isChildAttribute(AttributeDescriptor ad) {
        return DataObject.class.isAssignableFrom(ad.getReturnType());
    }

    @Override
    public boolean isChildAttribute(TypeDescriptor td) {
        return DataObject.class.isAssignableFrom(td.getTypeClass());
    }

    @Override
    public boolean supportAugmentation(AttributeDescriptor ad) {
        return DataObject.class.isAssignableFrom(ad.getReturnType());
    }

    @Override
    public boolean supportAugmentation(TypeDescriptor td) {
        return DataObject.class.isAssignableFrom(td.getTypeClass());
    }
}
