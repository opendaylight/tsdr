/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.nbi;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.sun.jersey.api.core.DefaultResourceConfig;
/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class TSDRApplication extends DefaultResourceConfig {
    @Override
    public Set<Class<?>> getClasses() {
        return new HashSet<Class<?>>(Arrays.asList(TSDRRestAPI.class));
    }
}