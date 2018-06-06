/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.dataquery.rest.web;

import javax.servlet.ServletException;
import org.opendaylight.aaa.web.ServletDetails;
import org.opendaylight.aaa.web.WebContext;
import org.opendaylight.aaa.web.WebContextBuilder;
import org.opendaylight.aaa.web.WebContextRegistration;
import org.opendaylight.aaa.web.WebContextSecurer;
import org.opendaylight.aaa.web.WebServer;
import org.opendaylight.aaa.web.servlet.ServletSupport;
import org.opendaylight.tsdr.dataquery.TSDRQueryServiceApplication;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.TsdrLogDataService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.TsdrMetricDataService;

/**
 * Initializer for web components.
 *
 * @author Thomas Pantelis
 */
public class WebInitializer {
    private final WebContextRegistration registraton;

    public WebInitializer(WebServer webServer, WebContextSecurer webContextSecurer, ServletSupport servletSupport,
            TsdrMetricDataService metricDataService, TsdrLogDataService logDataService) throws ServletException {
        WebContextBuilder webContextBuilder = WebContext.builder().contextPath("tsdr").supportsSessions(true)
            .addServlet(ServletDetails.builder().servlet(servletSupport.createHttpServletBuilder(
                    new TSDRQueryServiceApplication(metricDataService, logDataService)).build())
                .addUrlPattern("/*").build());

        webContextSecurer.requireAuthentication(webContextBuilder, "/*");

        registraton = webServer.registerWebContext(webContextBuilder.build());
    }

    public void close() {
        registraton.close();
    }
}
