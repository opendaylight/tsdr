/*
 * Copyright (c) 2016 Saugo360.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.restconf.collector;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.regex.Pattern;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for intercepting restconf requests and logging them if they conform to the criteria
 * specified in the configuration. Much of the code in the class is added to create an input stream that can be consumed
 * again by other ServletFilters or by the Restconf Servlet itself. More information about this issue could be found
 * here: http://wetfeetblog.com/servlet-filer-to-log-request-and-response-details-and-payload/431
 *
 * @author <a href="mailto:a.alhamali93@gmail.com">AbdulRahman AlHamali</a>
 *
 *         Created: Dec 16th, 2016
 *
 */
public class TSDRRestconfCollectorFilter implements Filter {

    /**
     * the logger of the class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * a reference to the restconf collector logger singleton.
     */
    private TSDRRestconfCollectorLogger tsdrRestconfCollectorLogger;

    /**
     * a reference to the restconf collector config singleton.
     */
    private TSDRRestconfCollectorConfig tsdrRestconfCollectorConfig;

    /**
     * called when the filter is first initialized, it obtains the instances of the restconf collector logger and
     * the restconf collector config.
     * @param filterConfig the configuration of the filter (not used)
     */
    @Override
    public void init(FilterConfig filterConfig) {
        tsdrRestconfCollectorLogger = TSDRRestconfCollectorLogger.getInstance();
        tsdrRestconfCollectorConfig = TSDRRestconfCollectorConfig.getInstance();
    }

    /**
     * called each time a request is sent to the restconf, the function checks whether the request satisfies the
     * criteria specified in the configuration, and decides whether to log it or not, then passes the request to the
     * rest of the chain.
     * @param request the request received
     * @param response the response from the servlet
     * @param chain the chain of filters registered on the servlet
     */
    @Override
    @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {

        try {
            HttpServletRequest httpServletRequest = (HttpServletRequest)request;

            BufferedRequestWrapper bufferedRequest = new BufferedRequestWrapper(httpServletRequest);

            String method = httpServletRequest.getMethod();
            String path = httpServletRequest.getPathInfo();
            String address = httpServletRequest.getRemoteAddr();
            String body = bufferedRequest.getRequestBody();

            if (requestPassesCriteria(method, path, address, body)) {
                tsdrRestconfCollectorLogger.insertLog(method, path, address, body);
            }

            chain.doFilter(bufferedRequest, response);

        } catch (IOException | ServletException e) {
            LOG.error("doFilter failed", e);
        }
    }

    /**
     * called when the filter is destroyed to clean up.
     */
    @Override
    public void destroy() {
    }

    /**
     * called with the parameters of the request to check if the match the criteria specified in the configuration.
     * @param method the http method
     * @param path the relative url of the request
     * @param address the address from which the request originated
     * @param body the content of the request
     * @return returns true if the request passes the criteria
     */
    private boolean requestPassesCriteria(String method, String path, String address, String body) {
        String methods = tsdrRestconfCollectorConfig.getProperty("METHODS_TO_LOG");
        if (!Arrays.asList(methods.split(",")).contains(method)) {
            return false;
        }

        String paths = tsdrRestconfCollectorConfig.getProperty("PATHS_TO_LOG");
        if (!Pattern.compile(paths).matcher(path).matches()) {
            return false;
        }

        String addresses = tsdrRestconfCollectorConfig.getProperty("REMOTE_ADDRESSES_TO_LOG");
        if (!Pattern.compile(addresses).matcher(address).matches()) {
            return false;
        }

        String content = tsdrRestconfCollectorConfig.getProperty("CONTENT_TO_LOG");
        if (!Pattern.compile(content).matcher(body).matches()) {
            return false;
        }

        return true;
    }

    /**
     * a wrapper for the request that provides a body that could be consumed multiple times.
     */
    private static final class BufferedRequestWrapper extends HttpServletRequestWrapper {

        /**
         * a byte array that caches the body of the request.
         */
        private byte[] buffer = null;

        /**
         * when the constructor is called, it reads the body of the request and caches it in the byte buffer.
         */
        BufferedRequestWrapper(HttpServletRequest req) throws IOException {
            super(req);

            // Read InputStream and store its content in a buffer.
            InputStream is = req.getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];

            int letti;
            while ((letti = is.read(buf)) > 0) {
                baos.write(buf, 0, letti);
            }

            this.buffer = baos.toByteArray();
        }

        /**
         * whenever getInputStream is called, it returns a new stream, filled with the data of the byte buffer, that
         * way the input stream will be reusable by other filters on the way.
         */
        @Override
        public ServletInputStream getInputStream() {
            return new BufferedServletInputStream(new ByteArrayInputStream(this.buffer));
        }

        /**
         * reads the input stream and stores it in a string that it returns.
         * @return returns a string containing the body of the request
         */
        @SuppressFBWarnings("DM_DEFAULT_ENCODING")
        String getRequestBody() throws IOException  {
            BufferedReader reader = new BufferedReader(new InputStreamReader(this.getInputStream()));

            String line = null;
            StringBuilder inputBuffer = new StringBuilder();

            do {
                line = reader.readLine();
                if (line != null) {
                    inputBuffer.append(line.trim());
                }
            } while (line != null);

            reader.close();
            return inputBuffer.toString().trim();
        }

    }

    /**
     * represents a reusable input stream.
     */
    private static final class BufferedServletInputStream extends ServletInputStream {

        /**
         * the internal stream.
         */
        private final ByteArrayInputStream bais;

        BufferedServletInputStream(ByteArrayInputStream bais) {
            this.bais = bais;
        }

        /**
         * when called, called the same function of the internal stream.
         */
        @Override
        public int available() {
            return this.bais.available();
        }

        /**
         * when called, called the same function of the internal stream.
         */
        @Override
        public int read() {
            return this.bais.read();
        }

        /**
         * when called, called the same function of the internal stream.
         */
        @Override
        public int read(byte[] buf, int off, int len) {
            return this.bais.read(buf, off, len);
        }
    }
}
