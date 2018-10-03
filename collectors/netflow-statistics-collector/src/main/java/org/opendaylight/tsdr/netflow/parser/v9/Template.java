/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.netflow.parser.v9;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates a v9 template.
 *
 * @author Thomas Pantelis
 */
final class Template {
    private final List<TemplateField> field;
    private final int totalLength;

    private Template(List<TemplateField> records) {
        this.field = ImmutableList.copyOf(records);
        totalLength = records.stream().mapToInt(rec -> rec.getLength()).sum();
    }

    Iterable<TemplateField> getFields() {
        return field;
    }

    int getTotalLength() {
        return totalLength;
    }

    @Override
    public String toString() {
        return "Template [totalLength=" + totalLength + ", field=" + field + "]";
    }

    static class Builder {
        private final List<TemplateField> records = new ArrayList<>();

        void addField(int type, int length) {
            records.add(new TemplateField(type, length));
        }

        Template build() {
            return new Template(records);
        }
    }
}
