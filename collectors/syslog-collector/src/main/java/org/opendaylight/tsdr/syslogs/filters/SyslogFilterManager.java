/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.syslogs.filters;


import java.util.HashMap;
import java.util.Map;

import org.opendaylight.tsdr.syslogs.server.decoder.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class SyslogFilterManager {
    private final static Logger LOGGER = LoggerFactory.getLogger(SyslogFilterManager.class);
    private Map<String,SyslogFilter[]> filters = new HashMap<String,SyslogFilter[]>();
    public SyslogFilterManager(){
        filters.put("*",new SyslogFilter[]{new PersistAllSyslogFilter()});
    }

    public TSDRLogRecord applyFilters(Message message){
        if(message!=null){
            String syslogString = message.getContent().trim();
            String packetHostAddress = message.getHostname();
            String syslogOriginator = null;
            int index1 = syslogString.indexOf("Original Address");
            int index2 = syslogString.indexOf("=",index1);
            int index3 = syslogString.indexOf(" ",index2+2);
            if(index1!=-1 && index2!=-1 && index3!=-1 && index2>index1 && index3>index2){
                syslogOriginator = syslogString.substring(index2+1,index3).trim();
            }else{
                syslogOriginator = packetHostAddress;
            }
            SyslogFilter[] flt = filters.get(syslogOriginator);
            if(flt==null){
                flt = filters.get("*");
            }
            if(flt.length==1 && flt[0].match(syslogString)){
                return flt[0].filterAndParseSyslog(syslogString, packetHostAddress, syslogOriginator);
            }
        }
        return null;
    }
}
