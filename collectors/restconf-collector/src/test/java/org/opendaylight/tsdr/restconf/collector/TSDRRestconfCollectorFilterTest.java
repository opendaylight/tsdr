/*
 * Copyright (c) 2016 Saugo360 and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.restconf.collector;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

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
    private FilterChain filterChain;

    /**
     * called before each test. It initialized the filter, creates mocks, and sets sample values for the configuration
     * properties.
     */
    @Before
    public void setup() {
        filterObject = new TSDRRestconfCollectorFilter();

        tsdrRestconfCollectorLogger = Mockito.mock(TSDRRestconfCollectorLogger.class);
        TSDRRestconfCollectorLogger.setInstance(tsdrRestconfCollectorLogger);

        tsdrRestconfCollectorConfig = Mockito.mock(TSDRRestconfCollectorConfig.class);
        Mockito.when(tsdrRestconfCollectorConfig.getProperty("METHODS_TO_LOG")).thenReturn("POST,PUT,DELETE");
        Mockito.when(tsdrRestconfCollectorConfig.getProperty("PATHS_TO_LOG")).thenReturn("/operations/.*");
        Mockito.when(tsdrRestconfCollectorConfig.getProperty("REMOTE_ADDRESSES_TO_LOG")).thenReturn("127\\.0\\.0\\.1");
        Mockito.when(tsdrRestconfCollectorConfig.getProperty("CONTENT_TO_LOG")).thenReturn(".*loggable.*");
        TSDRRestconfCollectorConfig.setInstance(tsdrRestconfCollectorConfig);

        filterChain = Mockito.mock(FilterChain.class);
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

        Mockito.verify(tsdrRestconfCollectorLogger, Mockito.times(1)).insertLog("POST", "/operations/test", "127.0.0.1",
            "{loggable}");
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

        Mockito.verify(tsdrRestconfCollectorLogger, Mockito.never()).insertLog(Mockito.anyString(), Mockito.anyString(),
            Mockito.anyString(), Mockito.anyString());
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
            Mockito.when(httpRequest.getMethod()).thenReturn(method);
            Mockito.when(httpRequest.getPathInfo()).thenReturn(path);
            Mockito.when(httpRequest.getRemoteAddr()).thenReturn(remoteAddress);

            if (!content.equals("")) {
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(content.getBytes());
                ServletInputStream servletInputStream = Mockito.mock(ServletInputStream.class);
                Mockito.when(servletInputStream.read(Matchers.<byte[]>any())).thenAnswer(new Answer<Integer>() {
                    @Override
                    public Integer answer(InvocationOnMock invocationOnMock) throws Throwable {
                        Object[] args = invocationOnMock.getArguments();
                        byte[] output = (byte[]) args[0];
                        return byteArrayInputStream.read(output);
                    }
                });
                Mockito.when(httpRequest.getInputStream()).thenReturn(servletInputStream);
            }

            return httpRequest;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * called after each test to make sure that the TSDRRestconfCollectorLogger and TSDRRestconfCollectorConfig
     * instances are cleaned.
     */
    @After
    public void teardown() {
        filterObject = null;
        TSDRRestconfCollectorLogger.setInstance(null);
        TSDRRestconfCollectorConfig.setInstance(null);
    }
}
