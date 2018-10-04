/*
 * Copyright (c) 2016 Saugo360 and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.yang;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import com.google.common.util.concurrent.ListenableScheduledFuture;
import java.util.Arrays;
import java.util.EventListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.dom.api.DOMNotification;
import org.opendaylight.mdsal.dom.api.DOMNotificationService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.tsdr.spi.scheduler.SchedulerService;
import org.opendaylight.tsdr.yang.notification.YangNotificationCollectorLogger;
import org.opendaylight.tsdr.yang.utils.MockSchemaService;
import org.opendaylight.tsdr.yang.utils.SimpleDOMNotification;
import org.opendaylight.tsdr.yang.utils.SimpleNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRLogRecordOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRMetricRecordOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.tsdr.yang.collector.config.yang.notification.rev181005.notification.subscription.Notifications;
import org.opendaylight.yang.gen.v1.urn.opendaylight.tsdr.yang.collector.config.yang.notification.rev181005.notification.subscription.NotificationsBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;

/**
 * This class is responsible for testing the TSDRLogCollectorLogger class.
 *
 */
@Ignore
public class YangNotificationCollectorLoggerTest {
    private static final SchemaPath SIMPLE_NOTIFICATION_ID = SchemaPath.create(true, SimpleNotification.QNAME);


    private static class TestListener implements EventListener {
        /**
         * Logger instance.
         */
        private Logger  logger;

        protected Logger getLogger() {
            Logger log = logger;
            if (log == null) {
                log = mock(Logger.class);
                logger = log;
            }
            return log;
        }
    }

    /**
     * the log collector logger instance to test.
     */
    private YangNotificationCollectorLogger loggerObject;

    /**
     * a mock of the collector SPI service.
     */
    @Mock
    private TsdrCollectorSpiService tsdrCollectorSpiService;

    @Mock
    private SchedulerService schedulerService;

    @Mock
    private ListenableScheduledFuture<?> mockFuture;

    @Mock
    private DOMNotificationService domNotificationService;

    @Mock
    private ListenerRegistration<TestListener> reg;

    @Mock
    private DOMNotification domNotification;

    @Captor
    private ArgumentCaptor<Runnable> runnableCaptor;

    private DOMSchemaService domSchemaService;

    /**
     * called before each test, obtains an instance of the LOG collector logger, and provides it with mocks.
     */
    @Before
    public void setup() {
        domSchemaService = new MockSchemaService();
        MockitoAnnotations.initMocks(this);
        when(tsdrCollectorSpiService.insertTSDRLogRecord(any()))
                .thenReturn(RpcResultBuilder.success(new InsertTSDRLogRecordOutputBuilder().build()).buildFuture());
        when(tsdrCollectorSpiService.insertTSDRMetricRecord(any()))
                .thenReturn(RpcResultBuilder.success(new InsertTSDRMetricRecordOutputBuilder().build()).buildFuture());
        doReturn(mockFuture).when(schedulerService)
                .scheduleTaskAtFixedRate(runnableCaptor.capture(), anyLong(), anyLong());
        //when(domNotificationService.registerNotificationListener(any(), anyCollection())).thenReturn(reg);

        final Notifications notification = new NotificationsBuilder()
                .setNotification("(http://netconfcentral.org/ns/toaster?revision=2009-11-20)toasterRestocked")
                .build();

        loggerObject = new YangNotificationCollectorLogger(tsdrCollectorSpiService, schedulerService, domSchemaService,
                domNotificationService, Arrays.asList(notification));

    }

    /**
     * tests the storage process, inserts multiple logs into the loggerObject, and checks that the collector SPI is
     * getting called with the correct values each time, and that the caches are working correctly.
     */
    @Test
    public void testOnNotification() {
//        // First, we try inserting a log
//        long time = System.currentTimeMillis();
//        loggerObject.insertLog("org.opendaylight.test", time, "test", "this is a test");
//        loggerObject.insertLog("org.opendaylight.test2", time, "test2", "this is a second test");
//        runnableCaptor.getValue().run();
//
//        ArgumentCaptor<InsertTSDRLogRecordInput> argumentCaptor
//                = ArgumentCaptor.forClass(InsertTSDRLogRecordInput.class);
//        verify(tsdrCollectorSpiService).insertTSDRLogRecord(argumentCaptor.capture());
//
//        List<TSDRLogRecord> logRecords = argumentCaptor.getValue().getTSDRLogRecord();
//
//        assertEquals("org.opendaylight.test", logRecords.get(0).getNodeID());
//        assertEquals("LEVEL=test,MESSAGE=this is a test", logRecords.get(0).getRecordFullText());
//        assertEquals(0, (long) logRecords.get(0).getIndex());
//        assertEquals(time, (long) logRecords.get(0).getTimeStamp());
//
//        assertEquals("org.opendaylight.test2", logRecords.get(1).getNodeID());
//        assertEquals("LEVEL=test2,MESSAGE=this is a second test", logRecords.get(1).getRecordFullText());
//        assertEquals(1, (long) logRecords.get(1).getIndex());
//        assertEquals(time, (long) logRecords.get(1).getTimeStamp());
//
//        assertEquals(2, logRecords.size());

        loggerObject.onNotification(SimpleDOMNotification.buildEvent("Simple event"));
    }

    @After
    public void teardown() {
        loggerObject.close();
    }

    /**
     * tests the shutdown process. Verifies that the timer is canceled after run is called after the shut down.
     */
    @Test
    public void shutDownTest() {
        loggerObject.close();
        verify(mockFuture, times(1)).cancel(false);
        verify(reg, times(1)).close();
    }

}
