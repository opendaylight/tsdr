/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.collector.spi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.StoreTSDRLogRecordInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.StoreTSDRLogRecordOutputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.TsdrLogDataService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.StoreTSDRMetricRecordInput;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.StoreTSDRMetricRecordOutputBuilder;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.TsdrMetricDataService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRLogRecordInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRLogRecordOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRMetricRecordInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRMetricRecordOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecordBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrmetricrecord.input.TSDRMetricRecordBuilder;
import org.opendaylight.yangtools.yang.common.RpcError.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

/**
 * Unit tests for CollectorSpiImpl.
 *
 * @author <a href="mailto:saichler@gmail.com">Sharon Aicler</a>
 */
public class CollectorSpiImplTest {
    private final TsdrMetricDataService metricDataService = Mockito.mock(TsdrMetricDataService.class);
    private final TsdrLogDataService logDataService = Mockito.mock(TsdrLogDataService.class);
    private final CollectorSPIImpl impl = new CollectorSPIImpl(metricDataService,logDataService);

    private static TSDRMetricRecord createTSDRMetricRecord(String name, BigDecimal value) {
        return new TSDRMetricRecordBuilder().setTSDRDataCategory(DataCategory.EXTERNAL)
            .setTimeStamp(System.currentTimeMillis()).setNodeID("TestNode").setMetricName(name)
            .setMetricValue(value).setRecordKeys(Collections.singletonList(new RecordKeysBuilder()
                .setKeyName("Test").setKeyValue("Test").build())).build();
    }

    private static TSDRLogRecord createTSDRLogRecord(String text) {
        return new TSDRLogRecordBuilder().setTSDRDataCategory(DataCategory.EXTERNAL)
            .setTimeStamp(System.currentTimeMillis()).setNodeID("TestNode").setRecordFullText(text)
            .setRecordKeys(Collections.singletonList(new RecordKeysBuilder()
                .setKeyName("Test").setKeyValue("Test").build())).build();
    }

    @Test
    public void testInsertTSDRMetricRecord() throws InterruptedException, ExecutionException, TimeoutException {
        doReturn(RpcResultBuilder.success(new StoreTSDRMetricRecordOutputBuilder().build()).buildFuture())
            .when(metricDataService).storeTSDRMetricRecord(any());

        final TSDRMetricRecord inputMetricRecord = createTSDRMetricRecord("Test Metric", BigDecimal.valueOf(123));
        final ListenableFuture<RpcResult<InsertTSDRMetricRecordOutput>> resultFuture = impl.insertTSDRMetricRecord(
            new InsertTSDRMetricRecordInputBuilder().setCollectorCodeName("Test")
                .setTSDRMetricRecord(Collections.singletonList(inputMetricRecord)).build());

        assertTrue(resultFuture.get(5, TimeUnit.SECONDS).isSuccessful());

        ArgumentCaptor<StoreTSDRMetricRecordInput> storeMetricInputCaptor =
                ArgumentCaptor.forClass(StoreTSDRMetricRecordInput.class);
        verify(metricDataService).storeTSDRMetricRecord(storeMetricInputCaptor.capture());
        StoreTSDRMetricRecordInput storeMetricInput = storeMetricInputCaptor.getValue();

        assertEquals(1, storeMetricInput.getTSDRMetricRecord().size());
        org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input
            .TSDRMetricRecord storeMetricRecord = storeMetricInput.getTSDRMetricRecord().iterator().next();

        assertEquals(inputMetricRecord.getMetricName(), storeMetricRecord.getMetricName());
        assertEquals(inputMetricRecord.getMetricValue(), storeMetricRecord.getMetricValue());
        assertEquals(inputMetricRecord.getNodeID(), storeMetricRecord.getNodeID());
        assertEquals(inputMetricRecord.getRecordKeys(), storeMetricRecord.getRecordKeys());
        assertEquals(inputMetricRecord.getTimeStamp(), storeMetricRecord.getTimeStamp());
        assertEquals(inputMetricRecord.getTSDRDataCategory(), storeMetricRecord.getTSDRDataCategory());
    }

    @Test
    public void testInsertTSDRMetricRecordFailure() throws InterruptedException, ExecutionException, TimeoutException {
        String failureMessage = "Failed to store record";
        doReturn(RpcResultBuilder.failed().withError(ErrorType.APPLICATION, failureMessage).buildFuture())
            .when(metricDataService).storeTSDRMetricRecord(any());

        final ListenableFuture<RpcResult<InsertTSDRMetricRecordOutput>> resultFuture = impl.insertTSDRMetricRecord(
            new InsertTSDRMetricRecordInputBuilder().setCollectorCodeName("Test")
                .setTSDRMetricRecord(Collections.singletonList(
                        createTSDRMetricRecord("Test Metric", BigDecimal.valueOf(123)))).build());

        final RpcResult<InsertTSDRMetricRecordOutput> result = resultFuture.get(5, TimeUnit.SECONDS);
        assertFalse(result.isSuccessful());
        assertEquals(1, result.getErrors().size());
        assertEquals(failureMessage, result.getErrors().iterator().next().getMessage());
        assertEquals(ErrorType.APPLICATION, result.getErrors().iterator().next().getErrorType());
    }

    @Test
    public void testInsertTSDRLogRecord() throws InterruptedException, ExecutionException, TimeoutException {
        doReturn(RpcResultBuilder.success(new StoreTSDRLogRecordOutputBuilder().build()).buildFuture())
            .when(logDataService).storeTSDRLogRecord(any());

        TSDRLogRecord inputLogRecord = createTSDRLogRecord("Hello World");
        final ListenableFuture<RpcResult<InsertTSDRLogRecordOutput>> resultFuture = impl.insertTSDRLogRecord(
            new InsertTSDRLogRecordInputBuilder().setCollectorCodeName("Test")
                .setTSDRLogRecord(Collections.singletonList(inputLogRecord)).build());

        assertTrue(resultFuture.get(5, TimeUnit.SECONDS).isSuccessful());

        ArgumentCaptor<StoreTSDRLogRecordInput> storeLogInputCaptor =
                ArgumentCaptor.forClass(StoreTSDRLogRecordInput.class);
        verify(logDataService).storeTSDRLogRecord(storeLogInputCaptor.capture());
        StoreTSDRLogRecordInput storeLogInput = storeLogInputCaptor.getValue();

        assertEquals(1, storeLogInput.getTSDRLogRecord().size());
        org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecord
            storeLogRecord = storeLogInput.getTSDRLogRecord().iterator().next();

        assertEquals(inputLogRecord.getNodeID(), storeLogRecord.getNodeID());
        assertEquals(inputLogRecord.getRecordAttributes(), storeLogRecord.getRecordAttributes());
        assertEquals(inputLogRecord.getRecordFullText(), storeLogRecord.getRecordFullText());
        assertEquals(inputLogRecord.getRecordKeys(), storeLogRecord.getRecordKeys());
        assertEquals(inputLogRecord.getTimeStamp(), storeLogRecord.getTimeStamp());
        assertEquals(inputLogRecord.getTSDRDataCategory(), storeLogRecord.getTSDRDataCategory());
    }

    @Test
    public void testInsertTSDRLogRecordFailure() throws InterruptedException, ExecutionException, TimeoutException {
        String failureMessage = "Failed to store record";
        doReturn(RpcResultBuilder.failed().withError(ErrorType.APPLICATION, failureMessage).buildFuture())
            .when(logDataService).storeTSDRLogRecord(any());

        final ListenableFuture<RpcResult<InsertTSDRLogRecordOutput>> resultFuture = impl.insertTSDRLogRecord(
            new InsertTSDRLogRecordInputBuilder().setCollectorCodeName("Test")
                .setTSDRLogRecord(Collections.singletonList(createTSDRLogRecord("Hello World"))).build());

        final RpcResult<InsertTSDRLogRecordOutput> result = resultFuture.get(5, TimeUnit.SECONDS);
        assertFalse(result.isSuccessful());
        assertEquals(1, result.getErrors().size());
        assertEquals(failureMessage, result.getErrors().iterator().next().getMessage());
        assertEquals(ErrorType.APPLICATION, result.getErrors().iterator().next().getErrorType());
    }
}
