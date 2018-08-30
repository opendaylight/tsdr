/*
 * Copyright © 2018 Kontron Canada Inc and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.log;

import com.google.common.annotations.VisibleForTesting;
import org.ops4j.pax.logging.spi.PaxAppender;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for getting karaf logs and send them to
 * the TsdrLogCollectorLogger.
 *
 * @author Matthieu Cauffiez
 *
 */
public class TsdrLogCollector implements PaxAppender {
    private static final Logger LOG = LoggerFactory
            .getLogger(TsdrLogCollector.class);
    private ThreadLocal<Boolean> inLogAppender = new ThreadLocal<>();

    private final TsdrLogCollectorLogger logCollectorLogger;
    private final TsdrLogCollectorConfig logCollectorConfig;

    public TsdrLogCollector(TsdrLogCollectorLogger tsdrLogCollectorLogger,
                            TsdrLogCollectorConfig tsdrLogCollectorConfig) {
        logCollectorLogger = tsdrLogCollectorLogger;
        logCollectorConfig = tsdrLogCollectorConfig;
    }

    public void doAppend(PaxLoggingEvent event) {
        if (inLogAppender.get() == null) {
            try {
                inLogAppender.set(Boolean.TRUE);
                appendToTsdr(event);
            }
            finally {
                inLogAppender.remove();
            }
        }
    }

    private void appendToTsdr(PaxLoggingEvent event) {
        if (isIgnored(event.getLoggerName())) {
            LOG.debug("{} LOG is ignored by the log collector", event.getLoggerName());
            return;
        }
        logCollectorLogger.insertLog(event.getLoggerName(), event.getTimeStamp(),
                event.getLevel().toString(), event.getMessage());

    }

    @VisibleForTesting
    boolean isIgnored(String loggerName) {
        if (loggerName == null) {
            return true;
        }
        return logCollectorConfig.getIgnoredCategories()
                .stream().anyMatch(category -> category.matcher(loggerName).matches());
    }

}