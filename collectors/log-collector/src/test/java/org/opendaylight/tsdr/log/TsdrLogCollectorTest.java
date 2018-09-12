/*
 * Copyright © 2018 Kontron Canada Inc and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.log;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.tsdr.log.utils.LogEvent;
import org.opendaylight.tsdr.log.utils.LogLevel;

/**
 * This class is responsible for testing the TSDRLogCollector class.
 *
 */
public class TsdrLogCollectorTest {
    /**
     * Config file name.
     */
    private static final String FILE_NAME = "./etc/tsdr-log-collector.properties";

    /**
     * the log collector logger instance to test.
     */
    @Mock
    private TsdrLogCollectorLogger tsdrLogCollectorLogger;

    /**
     * called before each test, obtains an instance of the LOG collector logger, and provides it with mocks.
     */
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);


    }

    @Test
    public void testDoAppend() {
        long time = System.currentTimeMillis();
        final TsdrLogCollector tsdrLogCollector =
                new TsdrLogCollector(tsdrLogCollectorLogger, new TsdrLogCollectorConfig(FILE_NAME));

        // create a log
        LogEvent logEvent = new LogEvent("org.opendaylight.plop.test", time,
                new LogLevel(), "this is a test");

        //Append a log
        tsdrLogCollector.doAppend(logEvent);

        verify(tsdrLogCollectorLogger, times(1)).insertLog(logEvent.getLoggerName(),
                logEvent.getTimeStamp(), logEvent.getLevel().toString(), logEvent.getMessage());
    }


    @Test
    public void testIgnoredLoggerCategories() {
        final TsdrLogCollector tsdrLogCollector =
                new TsdrLogCollector(tsdrLogCollectorLogger, new TsdrLogCollectorConfig(FILE_NAME));

        assertTrue(tsdrLogCollector.isIgnored("org.opendaylight.tsdr.test"));
        assertFalse(tsdrLogCollector.isIgnored("org.opendaylight.test.test"));
    }

}
