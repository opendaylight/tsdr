/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.netflow.parser;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.LoggerFactory;

/**
 * Caches by TemplateKey.
 *
 * @author Thomas Pantelis
 */
public class TemplateCache<T> {
    private final Map<TemplateKey, T> templateMap = new ConcurrentHashMap<>();

    public T get(long sourceId, int templateId) {
        return templateMap.get(new TemplateKey(sourceId, templateId));
    }

    public void put(long sourceId, int templateId, T template) {
        final TemplateKey key = new TemplateKey(sourceId, templateId);
        templateMap.put(key, template);

        LoggerFactory.getLogger(getClass()).debug("Added template - key: {}, {}", key, template);
    }
}
