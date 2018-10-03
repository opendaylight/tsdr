/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.netflow.parser.v9;

/**
 * Encapsulates a v9 template field.
 *
 * @author Thomas Pantelis
 */
class TemplateField {
    private final int type;
    private final int length;

    TemplateField(int type, int length) {
        this.type = type;
        this.length = length;
    }

    int getType() {
        return type;
    }

    int getLength() {
        return length;
    }

    @Override
    public String toString() {
        return "TemplateField [type=" + type + ", length=" + length + "]";
    }
}
