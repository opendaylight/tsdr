/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.nbi.rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/*
 * @Author = Sharon Aicler (saichler@cisco.com )
*/
public class TSDRRestRequest implements InvocationHandler{
    private List<String> request = new ArrayList<String>(10);
    private Map<String,String> requestAttributes = new HashMap<String,String>();

    public TSDRRestRequest(InputStream _input) throws IOException{
        BufferedReader in = new BufferedReader(new InputStreamReader(_input));
        String line = in.readLine();
        while(line!=null){
            if(line.trim().equals("")){
                break;
            }else{
                addLine(line);
            }
            line = in.readLine();
        }
        parseRequest();
    }

    private void parseRequest(){
        //GET /render?target=randomWalk(%27random%20walk%27)&from=-5min&until=now&format=json&maxDataPoints=1582 HTTP/1.1
        String line = request.get(0);
        int index1 = line.indexOf("//");
        int index2 = line.indexOf("/",index1+2);
        String method = line.substring(index1+1,index2).trim();
        requestAttributes.put("method", method);
        index1 = index2;
        index2 = line.indexOf("?");
        String command = line.substring(index1+1,index2);
        requestAttributes.put("command", command);
        int index3 = line.indexOf("HTTP");

        index1 = index2;
        index2 = line.indexOf("&",index1+1);
        while(index2!=-1){
            String arg = line.substring(index1+1,index2);
            int eqIndex = arg.indexOf("=");
            String name = arg.substring(0,eqIndex).trim();
            String value = arg.substring(eqIndex+1).trim();
            requestAttributes.put(name.toLowerCase(), value);
            index1 = index2;
            index2 = line.indexOf("&",index1+1);
        }
        String arg = line.substring(index1+1,index3);
        int eqIndex = arg.indexOf("=");
        String name = arg.substring(0,eqIndex).trim();
        String value = arg.substring(eqIndex+1).trim();
        requestAttributes.put(name.toLowerCase(), value);
    }

    public IRequest getRequest(Class<? extends IRequest> requestType){
        return (IRequest)Proxy.newProxyInstance(this.getClass().getClassLoader(),new Class[]{requestType}, this);
    }

    public static String extract(String str,String line){
        int index1 = line.indexOf(str)+str.length();
        int index2 = line.indexOf("&",index1);
        return line.substring(index1,index2);
    }

    private void addLine(String line){
        this.request.add(line);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return this.requestAttributes.get(method.getName().substring(3).toLowerCase());
    }
}
