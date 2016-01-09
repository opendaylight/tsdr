/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2015 xFlow Research Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.netflow;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrlog.RecordAttributes;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrlog.RecordAttributesBuilder;

/**
 * @author <a href="mailto:saichler@gmail.com">Sharon Aicler</a>
 * @author <a href="mailto:muhammad.umair@xflowresearch.com">Umair Bhatti</a>
 */
public class NetflowPacketParser {

    private final List<RecordAttributes> recordAttributes = new ArrayList<>(27);

    public NetflowPacketParser(final byte[] buff){
        addValue("version",convert(buff, 0, 2));
        addValue("sysUpTime",convert(buff, 4, 4));
        addValue("unix_secs",convert(buff, 8, 4));
        addValue("unix_nsecs",convert(buff, 12, 4));
        addValue("flow_sequence",convert(buff, 16, 4));
        addValue("engine_type",Byte.toString(buff[20]));
        addValue("engine_id",Byte.toString(buff[21]));
        long s_interval = convert(buff[23]);
        s_interval += Long.parseLong(convert(buff, 23, 1));
        addValue("samplingInterval","" + s_interval);
        addValue("srcAddr",convertIPAddress(new Long(convert(buff, 24, 4)).longValue()));
        addValue("dstAddr",convertIPAddress(new Long(convert(buff, 28, 4)).longValue()));
        addValue("nextHop", convert(buff, 32, 4));
        addValue("input",convert(buff, 36, 2));
        addValue("output", convert(buff, 38, 2));
        addValue("dPkts", convert(buff, 40, 4));
        addValue("dOctets", convert(buff, 44, 4));
        String first = convert(buff, 48, 4);
        addValue("First", first);
        String last = convert(buff, 52, 4);
        addValue("Last",last);
        addValue("srcPort",convert(buff, 56, 2));
        addValue("dstPort",convert(buff, 58, 2));
        addValue("tcpFlags",Byte.toString(buff[61]));
        addValue("protocol",Byte.toString(buff[62]));
        addValue("tos", Byte.toString(buff[63]));
        addValue("srcAS",convert(buff, 64, 2));
        addValue("dstAS",convert(buff, 66, 2));
        addValue("srcMask",Byte.toString(buff[68]));
        addValue("dstMask",Byte.toString(buff[69]));
        addValue("flowDuration",new Long(Long.parseLong(last) - Long.parseLong(first)).toString());
    }

    public List<RecordAttributes> getRecordAttributes(){
        return this.recordAttributes;
    }

    public void addValue(String name,String value){
        RecordAttributesBuilder builder = new RecordAttributesBuilder();
        builder.setName(name);
        builder.setValue(value);
        this.recordAttributes.add(builder.build());
    }

    /**
     * function to convert the IP address from byte to decimal (quad dotted) notation
     * @param addr1 - long representing the ip address. if this is ipv4 it should be int and not long, ipv6 is two longs not one.
     * @return - String of the ip address
     */
    public static final String convertIPAddress(long addr1){
        int addr = (int) (addr1 & 0xffffffff);
        StringBuffer buf = new StringBuffer();
        buf.append(((addr >>> 24) & 0xff)).append('.').append(((addr >>> 16) & 0xff)).append('.').append(((addr >>> 8) & 0xff)).append('.').append(addr & 0xff);
        return buf.toString();
    }

    /**
     * function to convert attributes from byte data to long data type accordingly.
     * @param p - the byte array containing the long
     * @param off - The offet place where the long starts
     * @param len - The length, actually we should remove this parameter as a long is always 8 bytes.
     * @return - A string representation of the long
     */
    public static final String convert(byte[] p, int off, int len){
        long ret = 0;
        int done = off + len;
        for (int i = off; i < done; i++){
            ret = ((ret << 8) & 0xffffffff) + (p[i] & 0xff);
        }
        return (new Long(ret)).toString();
    }

    /**
     * function to convert the sampling interval (6 bits of 23rd byte)
     * @param p - byte out of 6 bits representing the interval.
     * @return - long interval
     */
    public static final long convert(byte p){
        long ret = 0;
        ret = ((ret << 8) & 0xffffffff) + (p & 0x3f);
        return ret;
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(RecordAttributes ra:this.recordAttributes){
            if(!first){
                sb.append(",");
            }
            sb.append(ra.getName());
            sb.append("=");
            sb.append(ra.getValue());
            first = false;
        }
        return sb.toString();
    }
}