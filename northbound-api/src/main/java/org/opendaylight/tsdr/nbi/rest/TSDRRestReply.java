/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.nbi.rest;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
/*
 * @Author = Sharon Aicler (saichler@cisco.com )
*/
public class TSDRRestReply {
    private OutputStream output = null;
    private StringBuilder buff = new StringBuilder();
    public TSDRRestReply(OutputStream _output){
        this.output = new BufferedOutputStream(_output);
    }
    public void reply(String reply,int status) throws IOException{
        if(status==200){
            buff = new StringBuilder();

            write("HTTP/1.1 200 OK");
            write("Date: Fri, 31 Dec 1999 23:59:59 GMT");
            write("Server: Apache/0.8.4");
            write("Content-Type: application/json");
            write("Content-Length: "+reply.length());
            write("Expires: Sat, 01 Jan 2000 00:59:59 GMT");
            write("Cache-Control: max-age=60");
            write("Access-Control-Allow-Origin: *");
            write("Access-Control-Allow-Methods: GET, OPTIONS");
            write("Access-Control-Allow-Headers: origin, authorization, accept");
            write("Access-Control-Allow-Credentials: true");
            write("Connection: close");
            write("Last-modified: Fri, 09 Aug 1996 14:21:40 GMT");
            write("");
            write(reply);
            output.write(buff.toString().getBytes());
            output.flush();
        }
    }
    public void write(String str) throws IOException {
        buff.append(str);
        buff.append("\r\n");
    }
}
