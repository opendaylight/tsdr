/*
 * Copyright (c) 2015 Scott Melton All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.dataquery.rest;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

/*
 * Placeholder for the dataquery request processor filter.
 *
 * @author <a href="mailto:smelton2@uccs.edu">Scott Melton</a>
 */
public class RequestProcessorAlternate implements ContainerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestProcessorAlternate.class);

    private final String OPTIONS = "OPTIONS";
    private final String ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";
    private final String AUTHORIZATION = "authorization";

    @Context
    private HttpServletRequest httpRequest;

    @Override
    public ContainerRequest filter(ContainerRequest request) {
        log.debug("ContainerRequest.filter()");

        // Cross-Origin threat vector check
        if (checkCORSOptionRequest(request)) {
            return request;
        }
        return request;
    }

    /**
     * CORS access control : when browser sends cross-origin request, it first
     * sends the OPTIONS method with a list of access control request headers,
     * which has a list of custom headers and access control method such as GET.
     * POST etc. You custom header "Authorization will not be present in request
     * header, instead it will be present as a value inside
     * Access-Control-Request-Headers. We should not do any authorization
     * against such request. for more details :
     * https://developer.mozilla.org/en-US/docs/Web/HTTP/Access_control_CORS
     * taken from aaa-authn-idmlight
     */

    private boolean checkCORSOptionRequest(ContainerRequest request) {
        if (OPTIONS.equals(request.getMethod())) {
            List<String> headerList = request.getRequestHeader(ACCESS_CONTROL_REQUEST_HEADERS);
            if (headerList != null && !headerList.isEmpty()) {
                String header = headerList.get(0);
                if (header != null && header.toLowerCase().contains(AUTHORIZATION)) {
                    return true;
                }
            }
        }
        return false;
    }

    // // Validate an ODL token...
    // private Authentication validate(final String token) {
    // Authentication auth = ServiceLocator.INSTANCE.ts.get(token);
    // if (auth == null) {
    // throw unauthorized();
    // } else {
    // ServiceLocator.INSTANCE.as.set(auth);
    // }
    // return auth;
    // }
    //
    // // Houston, we got a problem!
    // private static final WebApplicationException unauthorized() {
    // ServiceLocator.INSTANCE.as.clear();
    // return new UnauthorizedException();
    // }

    // A custom 401 web exception that handles http basic response as well
    static final class UnauthorizedException extends WebApplicationException {
        private static final long serialVersionUID = -1732363804773027793L;
        static final String WWW_AUTHENTICATE = "WWW-Authenticate";
        static final Object OPENDAYLIGHT = "Basic realm=\"opendaylight\"";
        private static final Response response = Response.status(Status.UNAUTHORIZED)
                .header(WWW_AUTHENTICATE, OPENDAYLIGHT).build();

        public UnauthorizedException() {
            super(response);
        }
    }
}
