/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.netflow.parser.ipfix;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Encapsulates an IPFIX template.
 *
 * @author Thomas Pantelis
 */
final class Template {
    private final List<TemplateField> fields;
    private final int estimatedLength;
    private final long timestamp;

    private Template(List<TemplateField> fields, long timestamp) {
        this.fields = ImmutableList.copyOf(fields);
        this.timestamp = timestamp;
        estimatedLength = fields.stream().mapToInt(rec -> rec.getLength() == 65535 ? 2 : rec.getLength()).sum();
    }

    Collection<TemplateField> getFields() {
        return fields;
    }

    int getEstimatedLength() {
        return estimatedLength;
    }

    long getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "Template [timestamp=" + timestamp + ", estimatedLength=" + estimatedLength + ", fields=" + fields + "]";
    }

    static class Builder {
        private final List<TemplateField> records = new ArrayList<>();
        private final long timestamp;

        Builder(long timestamp) {
            this.timestamp = timestamp;
        }

        void addField(TemplateField field) {
            records.add(field);
        }

        Template build() {
            return new Template(records, timestamp);
        }
    }
}
