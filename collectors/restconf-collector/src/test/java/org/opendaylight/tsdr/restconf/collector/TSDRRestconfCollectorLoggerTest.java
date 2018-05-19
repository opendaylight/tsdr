/*
 * Copyright (c) 2016 Saugo360 and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.restconf.collector;

import static org.mockito.Matchers.any;

import java.util.List;
import java.util.Timer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
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
    private TsdrCollectorSpiService tsdrCollectorSpiService;

    /**
     * a mock timer, it does nothing. We use this mock to avoid using an actual timer that calls the method every
     * 5 seconds.
     */
    private Timer timer;

    /**
     * called before each test, obtains an instance of the restconf collector logger, and provides it with mocks.
     */
    @Before
    public void setup() throws Exception {
        timer = Mockito.mock(Timer.class);

        tsdrCollectorSpiService = Mockito.mock(TsdrCollectorSpiService.class);
        Mockito.when(tsdrCollectorSpiService.insertTSDRLogRecord(any()))
                .thenReturn(RpcResultBuilder.success(new InsertTSDRLogRecordOutputBuilder().build()).buildFuture());
        Mockito.when(tsdrCollectorSpiService.insertTSDRMetricRecord(any()))
                .thenReturn(RpcResultBuilder.success(new InsertTSDRMetricRecordOutputBuilder().build()).buildFuture());

        loggerObject = new TSDRRestconfCollectorLogger(() -> timer, tsdrCollectorSpiService);

        loggerObject.init();
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
        loggerObject.run();

        ArgumentCaptor<InsertTSDRLogRecordInput> argumentCaptor
            = ArgumentCaptor.forClass(InsertTSDRLogRecordInput.class);
        Mockito.verify(tsdrCollectorSpiService).insertTSDRLogRecord(argumentCaptor.capture());

        List<TSDRLogRecord> logRecords = argumentCaptor.getValue().getTSDRLogRecord();

        Assert.assertEquals("/restconf/path1", logRecords.get(0).getNodeID());
        Assert.assertEquals("METHOD=GET,REMOTE_ADDRESS=10.0.0.1,BODY=body1", logRecords.get(0).getRecordFullText());
        Assert.assertEquals(0, (long)logRecords.get(0).getIndex());

        Assert.assertEquals("/restconf/path2", logRecords.get(1).getNodeID());
        Assert.assertEquals("METHOD=POST,REMOTE_ADDRESS=10.0.0.2,BODY=body2", logRecords.get(1).getRecordFullText());
        Assert.assertEquals(1, (long)logRecords.get(1).getIndex());

        Assert.assertEquals(2, logRecords.size());

        // Now, we try inserting one log to assert whether the queue and
        // the index have been reset
        Mockito.reset(tsdrCollectorSpiService);
        Mockito.when(tsdrCollectorSpiService.insertTSDRLogRecord(any()))
                .thenReturn(RpcResultBuilder.success(new InsertTSDRLogRecordOutputBuilder().build()).buildFuture());
        Mockito.when(tsdrCollectorSpiService.insertTSDRMetricRecord(any()))
                .thenReturn(RpcResultBuilder.success(new InsertTSDRMetricRecordOutputBuilder().build()).buildFuture());

        loggerObject.insertLog("PUT", "/restconf/path3", "10.0.0.3", "body3");
        loggerObject.run();

        Mockito.verify(tsdrCollectorSpiService).insertTSDRLogRecord(argumentCaptor.capture());

        logRecords = argumentCaptor.getValue().getTSDRLogRecord();

        Assert.assertEquals("/restconf/path3", logRecords.get(0).getNodeID());
        Assert.assertEquals("METHOD=PUT,REMOTE_ADDRESS=10.0.0.3,BODY=body3", logRecords.get(0).getRecordFullText());
        Assert.assertEquals(0, (long)logRecords.get(0).getIndex());

        Assert.assertEquals(1, logRecords.size());

        // Finally, we assert that if no more records were inserted, the collector
        // spi service won't be called
        Mockito.reset(tsdrCollectorSpiService);
        loggerObject.run();
        Mockito.verify(tsdrCollectorSpiService, Mockito.never()).insertTSDRLogRecord(Mockito.any());
    }

    /**
     * tests the shutdown process. Verifies that the timer is canceled after run is called after the shut down.
     */
    @Test
    public void shutDownTest() {
        loggerObject.close();
        loggerObject.run();

        Mockito.verify(timer).cancel();
    }
}
