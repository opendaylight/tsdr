/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.syslogs;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.tsdr.syslogs.server.datastore.SyslogDatastoreManager;

public class TSDRSyslogCollectorImplNoPortAvailableTest {
    static final int UDP_PORT = 514;

    @Test
    public void testFailToBindToPorts() throws IOException, InterruptedException {
        DatagramSocket socket1 = null;
        DatagramSocket socket2 = null;

        // Just make sure the ports are occupied
        try {
            socket1 = new DatagramSocket(UDP_PORT);
        } catch (SocketException e) {
            /* Don't care */
        }
        try {
            socket2 = new DatagramSocket(UDP_PORT + 1000);
        } catch (SocketException e) {
            /* Don't care */
        }

        SyslogDatastoreManager manager = Mockito.mock(SyslogDatastoreManager.class);
        TSDRSyslogCollectorImpl impl = new TSDRSyslogCollectorImpl(null, manager, UDP_PORT, 6514);
        impl.init();
        Assert.assertTrue(!impl.isRunning());
        if (socket1 != null) {
            socket1.close();
        }
        if (socket2 != null) {
            socket2.close();
        }
    }
}
