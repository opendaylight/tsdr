/*
 * Copyright (c) 2015 Scott Melton All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package src.main.java.org.opendaylight.tsdr.dataquery.rest;

import javax.ws.rs.core.Application;

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
public class QueryServiceApplication extends Application {
    private static Logger log = LoggerFactory.getLogger(QueryServiceApplication.class);
}
