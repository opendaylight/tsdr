/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.netflow.parser;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a temporary cache of packet data where parsing couldn't be completed due to a missing template. If the
 * template isn't received within a period of time, the data is discarded.
 *
 * @author Thomas Pantelis
 */
public class MissingTemplateCache {
    private static final Logger LOG = LoggerFactory.getLogger(MissingTemplateCache.class);
    private static final long EXPIRY_TIME_IN_SEC = TimeUnit.MINUTES.toSeconds(1);

    private final LoadingCache<TemplateKey, List<NetflowPacketParser>> parserCache = CacheBuilder.newBuilder()
        .expireAfterWrite(EXPIRY_TIME_IN_SEC, TimeUnit.SECONDS).removalListener(
            (RemovalListener<TemplateKey, List<NetflowPacketParser>>)notification -> {
                if (notification.getCause() == RemovalCause.EXPIRED) {
                    LOG.warn("Template {} was not received within {} seconds - {} cached netflow packets were dropped",
                        notification.getKey(), EXPIRY_TIME_IN_SEC, notification.getValue().size());
                }
            })
        .build(CacheLoader.from(key -> Collections.synchronizedList(new ArrayList<>())));

    private final Function<TemplateKey, Boolean> templatePresenceFunction;

    public MissingTemplateCache(Function<TemplateKey, Boolean> templatePresenceFunction) {
        this.templatePresenceFunction = templatePresenceFunction;
    }

    public void put(long sourceId, int templateId, String sourceIP, NetflowPacketParser parser) {
        TemplateKey key = new TemplateKey(sourceId, templateId, sourceIP);
        LOG.debug("Adding missing template {}", key);
        parserCache.getUnchecked(key).add(parser);
    }

    public void checkTemplates() {
        if (parserCache.size() == 0) {
            return;
        }

        Iterator<Entry<TemplateKey, List<NetflowPacketParser>>> iter = parserCache.asMap().entrySet().iterator();
        while (iter.hasNext()) {
            Entry<TemplateKey, List<NetflowPacketParser>> entry = iter.next();

            LOG.debug("Checking missing template: {}", entry.getKey());

            if (templatePresenceFunction.apply(entry.getKey())) {
                iter.remove();

                List<NetflowPacketParser> parserList = entry.getValue();

                LOG.debug("Template: {} found - invoking {} parsers", entry.getKey(), parserList.size());

                synchronized (parserList) {
                    parserList.forEach(NetflowPacketParser::parseRecords);
                }
            }
        }
    }
}
