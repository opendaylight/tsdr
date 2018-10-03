/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.netflow.parser;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Caches V9 flowsrt templates.
 *
 * @author Thomas Pantelis
 */
class FlowsetTemplateCache {
    private static final Logger LOG = LoggerFactory.getLogger(FlowsetTemplateCache.class);

    private final Map<TemmplateKey, Map<Integer, Integer>> templateMap = new ConcurrentHashMap<>();

    Map<Integer, Integer> get(long sourceId, int templateId) {
        return templateMap.get(new TemmplateKey(sourceId, templateId));
    }

    void put(long sourceId, int templateId, Map<Integer, Integer> template) {
        final TemmplateKey key = new TemmplateKey(sourceId, templateId);
        templateMap.put(key, Collections.unmodifiableMap(template));

        LOG.debug("Added flowset template - key: {}, {}", key, template);
    }

    private static class TemmplateKey {
        private final long sourceId;
        private final int templateId;

        TemmplateKey(long sourceId, int templateId) {
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

            TemmplateKey other = (TemmplateKey) obj;
            return sourceId == other.sourceId && templateId == other.templateId;
        }

        @Override
        public String toString() {
            return "TemmplateKey [sourceId=" + sourceId + ", templateId=" + templateId + "]";
        }
    }
}
