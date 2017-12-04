/*
 * Copyright (c) 2015 xFlow Research Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.netflow;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit tests for Netflow data parser.
 *
 * @author Muhammad Umair(muhammad.umair@xflowresearch.com)
**/

public class NetflowPacketParserTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final byte[] buff = new byte[1000];
    public final NetflowPacketParser parserService = new NetflowPacketParser(buff);

    @Test
    public void testConvertIPAddress() {
        //Buff string value can be more than int, so using long to handle.
        String buffStr = "167772162";
        long value = new Long(buffStr).longValue();
        String result = NetflowPacketParser.convertIPAddress(value);
        Assert.assertEquals("10.0.0.2", result);
        //Buff value more than int range
        buffStr = "2646458435";
        value = new Long(buffStr).longValue();
        result = NetflowPacketParser.convertIPAddress(value);
        Assert.assertEquals("157.189.192.67", result);
    }

    @Test
    public void testConvertIPAddress_Exception() {
        //Buff string value can be more than int, so to test exception
        thrown.expect(NumberFormatException.class);
        String buffStr = "2646458435";
        int intValue = new Integer(buffStr).intValue();
        NetflowPacketParser.convertIPAddress(intValue);
    }

    @Test
    public void convertBytetest() {
        byte value = 100;
        long result = NetflowPacketParser.convert(value);
        Assert.assertEquals(36, result);
    }

    @Test
    public void addValueTest() {
        String name = "Name";
        String value = "value";
        parserService.addValue(name, value);
    }

    @Test
    public void fillFlowSetTemplateMapTest() {
        NetflowPacketParser.fillFlowSetTemplateMap(buff, 100, 10);
        Assert.assertNotEquals(NetflowPacketParser.getTemplate(), null);
    }

    @Test
    public void convertByteArrayTest() {
        String result = NetflowPacketParser.convert("[B@19dbc5c".getBytes(), 5, 4);
        Assert.assertEquals("1684169525", result);
    }

    @Test
    public void toStringTest() {
        String result = parserService.toString();
        /*
         * The packet sent for test has nothing, 0 PDU. Thus only verifying the
         * Netflow header.
         */
        String expectedResult = "version=0,sysUpTime=0,unix_secs=0,unix_nsecs=0,flow_sequence=0,"
                + "engine_type=0,engine_id=0,samplingInterval=0";
        Assert.assertEquals(expectedResult, result);
    }

    @Test
    public void getRecordAttribTest() {
        Object obj = parserService.getRecordAttributes();
        Assert.assertNotNull(obj);
    }
}
