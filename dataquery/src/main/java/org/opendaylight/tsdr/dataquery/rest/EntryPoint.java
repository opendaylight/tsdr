/*
 * Copyright (c) 2015 Scott Melton All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package src.main.java.org.opendaylight.tsdr.dataquery.rest;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * This class holds the JAX-RS HttpServlet entry point implementation for
 * the TSDR Query Service.
 *
 * JAX-RS is defined in JSR311.  It is the Java API for RESTful Service
 * specification.  Jersey is the Reference implementation of the JAX_RS
 * specification.  Jersey provides additional functionality for using
 * and managing RESTful web services, See TSDRQueryServiceApplication.java.
 *
 * HttpServlet provides the entry points to the TSDR Query Service.
 * These entry points represent the http verbs that are supported. GET, POST etc.
 * The ServletContainer discovers the implementation of these verbs by searching
 * this file.
 *
 * This class is specified in ./resources/WEB-INF/web.xml.
 * The REST Domain or Web-ContextPath URI is defined in ./dataquery/pom.xml.
 * The subdomains or servlet-mappings URI are defined in ./resources/WEB-INF/web.xml.
 *
 * Example:
 *           GET http://localhost:8181/tsdr/getmetric
 *           /tsdr  is the domain (Web-ContextPath)
 *           /getmetric  is the subdomain (servlet-mapping)
 *
 * Additional information for JAX-RS future releases can be found JCP JSR 339.
 *
 * @author <a href="mailto:smelton2@uccs.edu">Scott Melton</a>
 *
 * TODO: stm: The JAX-RS Web-ContextPath is defined in the dataquery web.xml
 * as a reduced scope POC for the TSDR REST interface.  When other TSDR projects
 * start to use REST, this should be moved up to the ./tsdr/pom.xml level.
 */

public class EntryPoint extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(EntryPoint.class);

    private static final String DOMAIN_SCOPE_REQUIRED = "Domain scope required";
    private static final String NOT_IMPLEMENTED = "not_implemented";
    private static final String UNAUTHORIZED = "unauthorized";

    static final String GETMETRIC_ENDPOINT = "/getmetric";

    // TSDRQueryServiceImpl queryService = new TSDRQueryServiceImpl();

    @Override
    public void init(ServletConfig config) throws ServletException {
        log.debug("EntryPoint.init()");
    }

    /*
     * URI domain /tsdr URI subdomains: /getmetric
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        log.debug("EntryPoint.doGet()");
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.debug("EntryPoint.doPost()");
        processRequest(request, response);
    }

    private void processRequest(HttpServletRequest request, HttpServletResponse response) {
        log.debug("EntryPoint.processRequest()");

        // HttpServletResponse response = queryService.executeDataStorageRequest(queryService.parseHttpRequestToTSDRRequest());

        // POC code
        if (request.getServletPath().equals(GETMETRIC_ENDPOINT)) {
            log.debug("subdomain: " + GETMETRIC_ENDPOINT);

            String data = "<html><body>Valid subdomain " + request.getServletPath() + "/body></html>";
            write(request, response, data);
        }

        String data = "<html><body>Invalid subdomain " + request.getServletPath() + "/body></html>";
        write(request, response, data);
    }

    private void write(HttpServletRequest request, HttpServletResponse response, String data) {
        try {
            PrintWriter pw = response.getWriter();
            pw.print(data);
            pw.flush();
            pw.close();
        } catch (Exception ex) {
            log.debug("write exception");
        }
    }
}
