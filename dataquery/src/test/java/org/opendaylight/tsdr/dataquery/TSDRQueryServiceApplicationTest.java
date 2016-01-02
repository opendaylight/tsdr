/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.dataquery;

import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.tsdr.dataquery.rest.query.TSDRLogQueryAPI;

/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class TSDRQueryServiceApplicationTest {

    @Test
    public void testTSDRApplication(){
        TSDRQueryServiceApplication app = new TSDRQueryServiceApplication();
        Set<Class<?>> classes = app.getClasses();
        Assert.assertTrue(classes.contains(TSDRLogQueryAPI.class));
    }

}
