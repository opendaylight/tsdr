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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.tsdrlog.RecordAttributes;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.tsdrlog.RecordAttributesBuilder;

/**
 * @author <a href="mailto:saichler@gmail.com">Sharon Aicler</a>
 * @author <a href="mailto:muhammad.umair@xflowresearch.com">Umair Bhatti</a>
 *
 * Modified: Jul 18, 2016
 */
public class NetflowPacketParser {
    private final List<RecordAttributes> recordAttributes = new ArrayList<>();
    private static HashMap<Integer, Integer> template;
    private static enum netflowV9Attribs {
        IN_BYTES,
        IN_PKTS,
        FLOWS,
        PROTOCOL,
        SRC_TOS,
        TCP_FLAGS,
        L4_SRC_PORT,
        IPV4_SRC_ADDR,
        SRC_MASK,
        INPUT_SNMP,
        L4_DST_PORT,
        IPV4_DST_ADDR,
        DST_MASK,
        OUTPUT_SNMP,
        IPV4_NEXT_HOP,
        SRC_AS, DST_AS,
        BGP_IPV4_NEXT_HOP,
        MUL_DST_PKTS,
        MUL_DST_BYTES,
        LAST_SWITCHED,
        FIRST_SWITCHED,
        OUT_BYTES,
        OUT_PKTS,
        MIN_PKT_LNGTH,
        MAX_PKT_LNGTH,
        IPV6_SRC_ADDR,
        IPV6_DST_ADDR,
        IPV6_SRC_MASK,
        IPV6_DST_MASK,
        IPV6_FLOW_LABEL,
        ICMP_TYPE,
        MUL_IGMP_TYPE,
        SAMPLING_INTERVAL,
        SAMPLING_ALGORITHM,
        FLOW_ACTIVE_TIMEOUT,
        FLOW_INACTIVE_TIMEOUT,
        ENGINE_TYPE,
        ENGINE_ID,
        TOTAL_BYTES_EXP,
        TOTAL_PKTS_EXP,
        TOTAL_FLOWS_EXP,
        IPV4_SRC_PREFIX,
        IPV4_DST_PREFIX,
        MPLS_TOP_LABEL_TYPE,
        MPLS_TOP_LABEL_IP_ADDR,
        FLOW_SAMPLER_ID,
        FLOW_SAMPLER_MODE,
        FLOW_SAMPLER_RANDOM_INTERVAL,
        MIN_TTL,
        MAX_TTL,
        IPV4_IDENT,
        DST_TOS,
        IN_SRC_MAC,
        OUT_DST_MAC,
        SRC_VLAN,
        DST_VLAN,
        IP_PROTOCOL_VERSION,
        DIRECTION,
        IPV6_NEXT_HOP,
        BPG_IPV6_NEXT_HOP,
        IPV6_OPTION_HEADERS,
        MPLS_LABEL_1,
        MPLS_LABEL_2,
        MPLS_LABEL_3,
        MPLS_LABEL_4,
        MPLS_LABEL_5,
        MPLS_LABEL_6,
        MPLS_LABEL_7,
        MPLS_LABEL_8,
        MPLS_LABEL_9,
        MPLS_LABEL_10,
        IN_DST_MAC,
        OUT_SRC_MAC,
        IF_NAME,
        IF_DESC,
        SAMPLER_NAME,
        IN_PERMANENT_BYTES,
        IN_PERMANENT_PKTS
    }
    /*
     * Constructor just make the header for netflow packet.There could be multiple PDU's of which the header would be same.
     */
    public NetflowPacketParser(final byte[] buff){
        template = null;
        netflowV9Attribs.values();//initializing here for code coverage.
        int version = Integer.parseInt(convert(buff, 0, 2));
        if(version == 9){
            addValue("version", "" + version + "");
            addValue("sysUpTime",convert(buff, 4, 4));
            addValue("unix_secs",convert(buff, 8, 4));
            addValue("flow_sequence",convert(buff, 12, 4));
            addValue("source_id",convert(buff, 16, 4));
        }else{
            addValue("version", "" + version + "");
            addValue("sysUpTime",convert(buff, 4, 4));
            addValue("unix_secs",convert(buff, 8, 4));
            addValue("unix_nsecs",convert(buff, 12, 4));
            addValue("flow_sequence",convert(buff, 16, 4));
            addValue("engine_type",Byte.toString(buff[20]));
            addValue("engine_id",Byte.toString(buff[21]));
            long s_interval = convert(buff[23]);
            s_interval += Long.parseLong(convert(buff, 23, 1));
            addValue("samplingInterval","" + s_interval);
        }
    }
    public static HashMap<Integer, Integer> getTemplate() {
        return template;
    }
    /**
     * function to add netflow format to the packets. The netflow header would be same while the format would be different according to the PDU's.
     * @param buff - the byte array of data contained in netflow packet.
     * @param len - the offset in the byte array where the data starts from.
     */
    public void addFormat(byte[] buff, int len){
        int version = Integer.parseInt((convert(buff, 0, 2)));
        if(version == 9){
            addFormatV9(buff, len);
        }
        else{
            addFormatV5(buff, len);
        }
    }
    public static void fillFlowSetTemplateMap(byte[] buff, int len, int count){
        if(template == null){
            template = new HashMap<Integer, Integer>();
            if(buff == null){
                return;
            }
            while(count > 0){
                int attrib = Integer.parseInt(convert(buff, len, 2));
                int lenn = Integer.parseInt(convert(buff, len + 2, 2));
                template.put(attrib, lenn);
                len += 4;
                count -= 1;
            }
        }
    }
    public void addFormatV9(byte[] buff, int len){
        if(template != null){
            netflowV9Attribs[] attributes = netflowV9Attribs.values();
            int dataOffset = 0;
            for(Map.Entry<Integer, Integer> entry : template.entrySet()){
                int attribID = entry.getKey();
                int attribLen = entry.getValue();
                addValue(attributes[attribID].toString(),convert(buff, len + dataOffset, attribLen));
                dataOffset += attribLen;
            }
        }else{
            addValue("startTime",convert(buff, len, 4));
            addValue("endTime", convert(buff, len+4, 4));
            addValue("Octets",convert(buff, len+8, 4));
            addValue("Packets",convert(buff, len+12, 4));
            addValue("inputInt", convert(buff, len+16, 2));
            addValue("outputInt", convert(buff, len+18, 2));
            addValue("srcAddr", convertIPAddress(new Long(convert(buff, len + 20, 4)).longValue()));
            addValue("dstAddr",convertIPAddress(new Long(convert(buff, len + 24, 4)).longValue()));
            addValue("Protocol", convert(buff, len+28, 1));
            addValue("ipTOS", convert(buff, len+29, 1));
            addValue("srcPort", convert(buff, len+30, 2));
            addValue("dstPort", convert(buff, len+32, 2));
            addValue("samplerID", convert(buff, len+34, 1));
            addValue("flowClass", convert(buff, len+35, 1));
            addValue("nextHop", convertIPAddress(new Long(convert(buff, len + 36, 4)).longValue()));
            addValue("dstMask", convert(buff, len+40, 1));
            addValue("srcMask", convert(buff, len+41, 1));
            addValue("TCPFlags", convert(buff, len+42, 1));
            addValue("Direction", convert(buff, len+43, 1));
            addValue("dstAS", convert(buff, len+44, 2));
            addValue("srcAS", convert(buff, len+46, 2));
        }
    }
    public void addFormatV5(byte[] buff, int len){
        addValue("srcAddr",convertIPAddress(new Long(convert(buff, len+24, 4)).longValue()));
        addValue("dstAddr",convertIPAddress(new Long(convert(buff, len+28, 4)).longValue()));
        addValue("nextHop",convertIPAddress(new Long(convert(buff, len+32, 4)).longValue()));
        addValue("input",convert(buff, len+36, 2));
        addValue("output", convert(buff, len+38, 2));
        addValue("dPkts", convert(buff, len+40, 4));
        addValue("dOctets", convert(buff, len+44, 4));
        String first = convert(buff, len+48, 4);
        addValue("First", first);
        String last = convert(buff, len+52, 4);
        addValue("Last",last);
        addValue("srcPort",convert(buff, len+56, 2));
        addValue("dstPort",convert(buff, len + 58, 2));
        addValue("tcpFlags",Byte.toString(buff[len+61]));
        addValue("protocol",Byte.toString(buff[len+62]));
        addValue("tos", Byte.toString(buff[len+63]));
        addValue("srcAS",convert(buff, len+64, 2));
        addValue("dstAS",convert(buff, len+66, 2));
        addValue("srcMask",Byte.toString(buff[len+68]));
        addValue("dstMask",Byte.toString(buff[len+69]));
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