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
import org.apache.karaf.shell.api.console.CommandLine;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
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
        String str = new String("PORT");
        CommandLine commandLine = Mockito.mock(CommandLine.class);
        Mockito.when(commandLine.getCursorArgument()).thenReturn("PORT");
        Mockito.when(commandLine.getArgumentPosition()).thenReturn(0);
        Mockito.when(commandLine.getBufferPosition()).thenReturn(4);
        List<String> vals = new ArrayList<>();
        for (DataCategory dc : DataCategory.values()) {
            vals.add(dc.name());
        }
        DataCategory.values();
        ListMetricsCommandCompleter completer = new ListMetricsCommandCompleter();
        completer.complete(null, commandLine, vals);
    }
}
