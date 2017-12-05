/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.spi.util;

import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.tsdr.spi.command.completer.ListMetricsCommandCompleter;
import org.opendaylight.tsdr.spi.model.TSDRConstants;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;

public class GeneralTest {
    @Test
    public void testTSDRConstants() {
        Assert.assertEquals("FlowID", TSDRConstants.FLOW_KEY_NAME);
    }

    @Test
    public void testCompleter() {
        ListMetricsCommandCompleter completer = new ListMetricsCommandCompleter();
        String str = new String("PORT");
        List<String> vals = new ArrayList<>();
        for (DataCategory dc : DataCategory.values()) {
            vals.add(dc.name());
        }
        DataCategory.values();
        completer.complete(str,4,vals);
    }
}
