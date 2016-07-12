/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2016 TethrNet Technology Co.Ltd and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.syslogs;

import java.io.IOException;
import java.net.*;

/**
 * This Class is for Syslog Message generating.
 *
 * @author Sharon Aicler(saichler@gmail.com)
 **/
/*
    This is a Syslog Generator for testing purposes
 */
public class SyslogGenerator {
    private final DatagramSocket socket;
    private final InetAddress destHost;
    private final int destPort;
    public static final int DEFAULT_SYSLOG_PORT = 514;

    public SyslogGenerator(String destHost, int destinationPort) throws SocketException, UnknownHostException {
        this.destPort = destinationPort;
        //Arbitrary port to send the packets from
        this.socket = new DatagramSocket(54321);
        this.destHost = InetAddress.getByName(destHost);
    }

    public final void sendSyslog(String syslogMessageText, int numberOfSyslogsToSend, long delayBetweenSyslogs, int numberOfIteration, long delayBetweenIterations) throws InterruptedException, IOException {
        for (int i = 0; i < numberOfIteration; i++) {
            for (int j = 0; j < numberOfSyslogsToSend; j++) {
                byte data[] = syslogMessageText.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, destHost, destPort);
                socket.send(packet);
                System.out.println(syslogMessageText + " has been sent.");
                Thread.sleep(delayBetweenSyslogs);
            }
            Thread.sleep(delayBetweenIterations);
        }
    }

    public final void sendSyslog(String syslogMessageText) throws IOException, InterruptedException {
        sendSyslog(syslogMessageText, 1, 0, 1, 0);
    }

    public final void sendSyslog(String syslogMessageText, int numberOfSyslogsToSend) throws IOException, InterruptedException {
        sendSyslog(syslogMessageText, numberOfSyslogsToSend, 50, 1, 0);
    }

    public static void main(String args[]) {
        if (args == null) {
            //Will add more usages in the future
            System.out.println("Usage: sendSyslog <message> - Send syslog to local host on port 514");
            System.out.println("       sendSyslog <message> <count> - Send <count> syslog to local host on port 514");
            System.out.println("       sendSyslog <message> <host> <port> - Send syslog to <host> on <port>");
        }
        try {
            if (args.length == 1) {
                SyslogGenerator generator = new SyslogGenerator("127.0.0.1", DEFAULT_SYSLOG_PORT);
                generator.sendSyslog(args[0]);
            } else if (args.length == 2) {
                SyslogGenerator generator = new SyslogGenerator("127.0.0.1", DEFAULT_SYSLOG_PORT);
                for (int i = 0; i < Integer.parseInt(args[1]); i++) {
                    generator.sendSyslog("" + i + ":" + args[0]);
                    Thread.sleep(100);
                }
            } else if (args.length == 3) {
                SyslogGenerator generator = new SyslogGenerator(args[1], Integer.parseInt(args[2]));
                generator.sendSyslog(args[0]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}