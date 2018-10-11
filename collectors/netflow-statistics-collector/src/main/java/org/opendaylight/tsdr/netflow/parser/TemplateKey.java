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
    private final String sourceIP;

    TemplateKey(long sourceId, int templateId, String sourceIP) {
        this.sourceId = sourceId;
        this.templateId = templateId;
        this.sourceIP = sourceIP;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (sourceId ^ sourceId >>> 32);
        result = prime * result + templateId;
        result = prime * result + sourceIP.hashCode();
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
        return sourceId == other.sourceId && templateId == other.templateId && sourceIP.equals(other.sourceIP);
    }

    @Override
    public String toString() {
        return "TemplateKey [sourceId=" + sourceId + ", templateId=" + templateId + ", sourceIP=" + sourceIP + "]";
    }
}
