/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.netflow.parser;

/**
 * Encapsulates a v9 template key.
 *
 * @author Thomas Pantelis
 */
class TemplateKey {
    private final long sourceId;
    private final int templateId;

    TemplateKey(long sourceId, int templateId) {
        this.sourceId = sourceId;
        this.templateId = templateId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (sourceId ^ sourceId >>> 32);
        result = prime * result + templateId;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        TemplateKey other = (TemplateKey) obj;
        return sourceId == other.sourceId && templateId == other.templateId;
    }

    @Override
    public String toString() {
        return "TemmplateKey [sourceId=" + sourceId + ", templateId=" + templateId + "]";
    }
}
