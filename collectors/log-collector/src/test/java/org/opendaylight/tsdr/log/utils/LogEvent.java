/*
 * Copyright © 2018 Kontron Canada Inc and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.log.utils;

import java.util.Map;

import org.ops4j.pax.logging.spi.PaxLevel;
import org.ops4j.pax.logging.spi.PaxLocationInfo;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;


public class LogEvent implements PaxLoggingEvent {
    private PaxLevel paxLevel;
    private String loggerName;
    private String message;
    private Long timeStamp;

    public LogEvent(String loggerName, Long timeStamp, PaxLevel level, String message) {
        this.paxLevel = level;
        this.loggerName = loggerName;
        this.timeStamp = timeStamp;
        this.message = message;
    }

    @Override
    public PaxLocationInfo getLocationInformation() {
        return null;
    }

    @Override
    public PaxLevel getLevel() {
        return paxLevel;
    }

    @Override
    public String getLoggerName() {
        return loggerName;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getRenderedMessage() {
        return null;
    }

    @Override
    public String getThreadName() {
        return null;
    }

    @Override
    public String[] getThrowableStrRep() {
        return new String[0];
    }

    @Override
    public boolean locationInformationExists() {
        return false;
    }

    @Override
    public long getTimeStamp() {
        return timeStamp;
    }

    @Override
    public String getFQNOfLoggerClass() {
        return null;
    }

    @Override
    public Map getProperties() {
        return null;
    }
}
