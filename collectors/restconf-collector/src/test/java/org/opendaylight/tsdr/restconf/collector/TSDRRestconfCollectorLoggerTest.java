/*
 * Copyright (c) 2016 Saugo360 and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.restconf.collector;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import com.google.common.util.concurrent.ListenableScheduledFuture;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.tsdr.spi.scheduler.SchedulerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRLogRecordInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRLogRecordOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRMetricRecordOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

/**
 * This class is responsible for testing the TSDRRestconfCollectorLogger class.
 *
 * @author <a href="mailto:a.alhamali93@gmail.com">AbdulRahman AlHamali</a>
 *
 *         Created: Dec 16th, 2016
 *
 */
public class TSDRRestconfCollectorLoggerTest {

    /**
     * the restconf collector logger instance to test.
     */
    private TSDRRestconfCollectorLogger loggerObject;

    /**
     * a mock of the collector SPI service.
     */
    @Mock
    private TsdrCollectorSpiService tsdrCollectorSpiService;

    @Mock
    private SchedulerService schedulerService;

    @Mock
    private ListenableScheduledFuture<?> mockFuture;

    @Captor
    private ArgumentCaptor<Runnable> runnableCaptor;

    /**
     * called before each test, obtains an instance of the restconf collector logger, and provides it with mocks.
     */
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        doReturn(RpcResultBuilder.<Void>success().buildFuture())
            .when(tsdrCollectorSpiService).insertTSDRLogRecord(any());

        doReturn(mockFuture).when(schedulerService).scheduleTaskAtFixedRate(any(), anyLong(), anyLong());

        loggerObject = new TSDRRestconfCollectorLogger(tsdrCollectorSpiService, schedulerService);

        verify(schedulerService).scheduleTaskAtFixedRate(runnableCaptor.capture(), anyLong(), anyLong());
    }

    /**
     * tests the storage process, inserts multiple logs into the loggerObject, and checks that the collector SPI is
     * getting called with the correct values each time, and that the caches are working correctly.
     */
    @Test
    public void testStorageProcess() {
        // First, we try inserting two logs

        loggerObject.insertLog("GET", "/restconf/path1", "10.0.0.1", "body1");
        loggerObject.insertLog("POST", "/restconf/path2", "10.0.0.2", "body2");
        runnableCaptor.getValue().run();

        ArgumentCaptor<InsertTSDRLogRecordInput> argumentCaptor
            = ArgumentCaptor.forClass(InsertTSDRLogRecordInput.class);
        verify(tsdrCollectorSpiService).insertTSDRLogRecord(argumentCaptor.capture());

        List<TSDRLogRecord> logRecords = argumentCaptor.getValue().getTSDRLogRecord();

        assertEquals("/restconf/path1", logRecords.get(0).getNodeID());
        assertEquals("METHOD=GET,REMOTE_ADDRESS=10.0.0.1,BODY=body1", logRecords.get(0).getRecordFullText());
        assertEquals(0, (long)logRecords.get(0).getIndex());

        assertEquals("/restconf/path2", logRecords.get(1).getNodeID());
        assertEquals("METHOD=POST,REMOTE_ADDRESS=10.0.0.2,BODY=body2", logRecords.get(1).getRecordFullText());
        assertEquals(1, (long)logRecords.get(1).getIndex());

        assertEquals(2, logRecords.size());

        // Now, we try inserting one log to assert whether the queue and
        // the index have been reset
        Mockito.reset(tsdrCollectorSpiService);
        when(tsdrCollectorSpiService.insertTSDRLogRecord(any()))
                .thenReturn(RpcResultBuilder.success(new InsertTSDRLogRecordOutputBuilder().build()).buildFuture());
        when(tsdrCollectorSpiService.insertTSDRMetricRecord(any()))
                .thenReturn(RpcResultBuilder.success(new InsertTSDRMetricRecordOutputBuilder().build()).buildFuture());

        loggerObject.insertLog("PUT", "/restconf/path3", "10.0.0.3", "body3");
        runnableCaptor.getValue().run();

        verify(tsdrCollectorSpiService).insertTSDRLogRecord(argumentCaptor.capture());

        logRecords = argumentCaptor.getValue().getTSDRLogRecord();

        assertEquals("/restconf/path3", logRecords.get(0).getNodeID());
        assertEquals("METHOD=PUT,REMOTE_ADDRESS=10.0.0.3,BODY=body3", logRecords.get(0).getRecordFullText());
        assertEquals(0, (long)logRecords.get(0).getIndex());

        assertEquals(1, logRecords.size());

        // Finally, we assert that if no more records were inserted, the collector
        // spi service won't be called
        Mockito.reset(tsdrCollectorSpiService);

        runnableCaptor.getValue().run();
        verify(tsdrCollectorSpiService, Mockito.never()).insertTSDRLogRecord(Mockito.any());
    }

    /**
     * tests the shutdown process. Verifies that the timer is canceled after run is called after the shut down.
     */

    @Test
    public void shutDownTest() {
        loggerObject.close();
        verify(mockFuture, times(1)).cancel(false);
    }
}
