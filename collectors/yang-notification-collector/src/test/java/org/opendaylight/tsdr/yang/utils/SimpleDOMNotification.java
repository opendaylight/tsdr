/*
 * Copyright © 2018 Kontron Canada Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.yang.utils;

import org.opendaylight.controller.md.sal.dom.api.DOMNotification;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public final class SimpleDOMNotification implements DOMNotification {
    private static final SchemaPath SIMPLE_NOTIFICATION_ID = SchemaPath.create(true, SimpleNotification.QNAME);
    private static final YangInstanceIdentifier.NodeIdentifier SIMPLE_NOTIFICATION_ARG =
            new YangInstanceIdentifier.NodeIdentifier(SimpleNotification.QNAME);
    private static final YangInstanceIdentifier.NodeIdentifier DATA =
            new YangInstanceIdentifier.NodeIdentifier(QName.create(SimpleNotification.QNAME, "data").intern());

    private final ContainerNode body;

    private SimpleDOMNotification(final ContainerNode body) {
        this.body = body;
    }

    @Override
    public SchemaPath getType() {
        return SIMPLE_NOTIFICATION_ID;
    }

    @Override
    public ContainerNode getBody() {
        return body;
    }

    @Override
    public String toString() {
        return "SimpleDOMNotification [body=" + body + "]";
    }

    public static SimpleDOMNotification buildEvent(String data) {
        final ContainerNode topicNotification = Builders.containerBuilder()
                .withNodeIdentifier(SIMPLE_NOTIFICATION_ARG)
                .withChild(ImmutableNodes.leafNode(DATA, data))
                .build();
        return new SimpleDOMNotification(topicNotification);
    }

}