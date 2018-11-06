/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.netflow.parser.ipfix;

import org.opendaylight.tsdr.netflow.parser.ipfix.InformationElementMappings.Converter;

/**
 * Encapsulates an IPFIX template field.
 *
 * @author Thomas Pantelis
 */
class TemplateField {
    private final String identifier;
    private final Converter dataConverter;
    private final int length;

    TemplateField(String identifier, Converter dataConverter, int length) {
        this.identifier = identifier;
        this.dataConverter = dataConverter;
        this.length = length;
    }

    String getIdentifier() {
        return identifier;
    }

    Converter getDataConverter() {
        return dataConverter;
    }

    int getLength() {
        return length;
    }

    @Override
    public String toString() {
        return "TemplateField [identifier=" + identifier + ", length=" + length + "]";
    }
}
