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
import org.junit.Assert;
import org.junit.Test;


/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class TSDRSyslogCollectorImplNoPortAvailableTest {

    @Test
    public void testFailToBindToPorts() throws IOException, InterruptedException {
        DatagramSocket socket1 = null;
        DatagramSocket socket2 = null;
        //Just make sure the ports are occupied
        try{
            socket1 = new DatagramSocket(TSDRSyslogCollectorImpl.SYSLOG_PORT);
        }catch(Exception e){
            /*Don't care */
        }
        try{
            socket2 = new DatagramSocket(TSDRSyslogCollectorImpl.SYSLOG_BACKUP_PORT);
        }catch(Exception e) {
            /*Don't care */
        }
        TSDRSyslogCollectorImpl impl = new  TSDRSyslogCollectorImpl(null);
        Assert.assertTrue(!impl.isRunning());
        if(socket1!=null)
            socket1.close();
        if(socket2!=null)
            socket2.close();
    }
}
