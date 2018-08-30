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
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.ListenableScheduledFuture;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.List;
import javax.servlet.FilterChain;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.tsdr.spi.scheduler.SchedulerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRLogRecordInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.osgi.service.cm.ConfigurationException;

/**
 * This class is responsible for testing the TSDRRestconfCollectorFilter class.
 *
 * @author <a href="mailto:a.alhamali93@gmail.com">AbdulRahman AlHamali</a>
 *
 *         Created: Dec 16th, 2016
 *
 */
public class TSDRRestconfCollectorFilterTest {

    /**
     * the filter object on which we want to test.
     */
    private TSDRRestconfCollectorFilter filterObject;

    /**
     * an object to mock the restconf collector logger.
     */
    private TSDRRestconfCollectorLogger tsdrRestconfCollectorLogger;

    /**
     * an object to mock the restconf collector config.
     */
    private TSDRRestconfCollectorConfig tsdrRestconfCollectorConfig;

    /**
     * an object to mock the filter chain.
     */
    @Mock
    private FilterChain filterChain;

    @Mock
    private SchedulerService schedulerService;

    @Mock
    private ListenableScheduledFuture<?> mockFuture;

    @Mock
    private TsdrCollectorSpiService tsdrCollectorSpiService;

    @Captor
    private ArgumentCaptor<Runnable> runnableCaptor;

    /**
     * called before each test. It initialized the filter, creates mocks, and sets sample values for the configuration
     * properties.
     */
    @Before
    public void setup() throws ConfigurationException {
        MockitoAnnotations.initMocks(this);

        filterObject = new TSDRRestconfCollectorFilter();
        doReturn(RpcResultBuilder.<Void>success().buildFuture())
                .when(tsdrCollectorSpiService).insertTSDRLogRecord(any());

        doReturn(mockFuture).when(schedulerService)
                .scheduleTaskAtFixedRate(runnableCaptor.capture(), anyLong(), anyLong());

        tsdrRestconfCollectorLogger = new TSDRRestconfCollectorLogger(tsdrCollectorSpiService, schedulerService);
        tsdrRestconfCollectorLogger.init();

        tsdrRestconfCollectorConfig = new TSDRRestconfCollectorConfig();
        Hashtable<String, String> props = new Hashtable<>();
        props.put("METHODS_TO_LOG", "POST,PUT,DELETE");
        props.put("PATHS_TO_LOG", "/operations/.*");
        props.put("REMOTE_ADDRESSES_TO_LOG", "127\\.0\\.0\\.1");
        props.put("CONTENT_TO_LOG", ".*loggable.*");
        tsdrRestconfCollectorConfig.updated(props);

    }


    /**
     * tests the case when a request that passes the criteria is received.
     * the request should be inserted
     */
    @Test
    public void doFilterTestWithRequestThatPassesCriteria() {

        HttpServletRequest httpRequest = prepareRequest("POST", "/operations/test", "127.0.0.1", "{loggable}");

        filterObject.init(null);
        filterObject.doFilter(httpRequest, null, filterChain);
        filterObject.destroy();

        runnableCaptor.getValue().run();

        ArgumentCaptor<InsertTSDRLogRecordInput> argumentCaptor =
                ArgumentCaptor.forClass(InsertTSDRLogRecordInput.class);
        Mockito.verify(tsdrCollectorSpiService).insertTSDRLogRecord(argumentCaptor.capture());

        List<TSDRLogRecord> logRecords = argumentCaptor.getValue().getTSDRLogRecord();
        assertEquals("# of TSDRLogRecord", logRecords.size(), 1);
        assertEquals("getNodeID", "/operations/test", logRecords.get(0).getNodeID());
        assertEquals("getRecordFullText", "METHOD=POST,REMOTE_ADDRESS=127.0.0.1,BODY={loggable}",
                logRecords.get(0).getRecordFullText());
    }

    /**
     * tests the cases when requests that do not pass the criteria are received.
     * none of them should not be inserted
     */
    @Test
    public void doFilterTestWithRequestThatDoesNotPassCriteria() {

        filterObject.init(null);

        filterObject.doFilter(prepareRequest("GET", "/config/test", "127.0.0.2", "{something}"), null, filterChain);
        filterObject.doFilter(prepareRequest("POST", "/config/test", "127.0.0.2", "{something}"), null, filterChain);
        filterObject.doFilter(prepareRequest("POST", "/operations/test", "127.0.0.2", "{something}"), null,
            filterChain);
        filterObject.doFilter(prepareRequest("POST", "/operations/test", "127.0.0.1", "{something}"), null,
            filterChain);

        filterObject.destroy();

        runnableCaptor.getValue().run();

        Mockito.verify(tsdrCollectorSpiService, Mockito.never()).insertTSDRLogRecord(any());
    }

    /**
     * prepares a mock http request to be received as a paramter by doFilter.
     * @param method the http method of the request
     * @param path the relative url of the request
     * @param remoteAddress the address from which the request generated
     * @param content the body of the request
     * @return the generated mock http request
     */
    private HttpServletRequest prepareRequest(String method, String path, String remoteAddress, String content) {
        try {
            HttpServletRequest httpRequest = Mockito.mock(HttpServletRequest.class);
            when(httpRequest.getMethod()).thenReturn(method);
            when(httpRequest.getPathInfo()).thenReturn(path);
            when(httpRequest.getRemoteAddr()).thenReturn(remoteAddress);

            if (!content.equals("")) {
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(content.getBytes());
                ServletInputStream servletInputStream = Mockito.mock(ServletInputStream.class);
                when(servletInputStream.read(Matchers.<byte[]>any())).thenAnswer(invocationOnMock -> {
                    Object[] args = invocationOnMock.getArguments();
                    byte[] output = (byte[]) args[0];
                    return byteArrayInputStream.read(output);
                });
                when(httpRequest.getInputStream()).thenReturn(servletInputStream);
            }

            return httpRequest;
        } catch (IOException e) {
            return null;
        }
    }

    @After
    public void tearDown() {
        tsdrRestconfCollectorLogger.close();
    }


}
