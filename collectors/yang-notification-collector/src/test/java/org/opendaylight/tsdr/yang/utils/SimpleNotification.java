/*
 * Copyright © 2018 Kontron Canada Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.yang.utils;

import org.opendaylight.yangtools.yang.binding.Augmentable;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.QName;

public interface SimpleNotification
        extends
        DataObject,
        Augmentable<SimpleNotification>,
        Notification {
    QName QNAME = QName.create("urn:opendaylight:params:xml:ns:yang:simple:notification",
            "2018-10-09", "simple-notification").intern();
}

