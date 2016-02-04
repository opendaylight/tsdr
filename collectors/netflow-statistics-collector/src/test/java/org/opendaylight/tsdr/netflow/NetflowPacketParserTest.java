/*
 * Copyright (c) 2015 xFlow Research Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.netflow;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for Netflow data parser
 * @author Muhammad Umair(muhammad.umair@xflowresearch.com)
 * Created: Dec 30, 2015
 * Modified: Feb 08, 2016
**/

public class NetflowPacketParserTest {
    public NetflowPacketParser parserService;
    @Before
    public void setup(){
        byte[] buff = new byte[100];
        parserService = new NetflowPacketParser(buff);
        Assert.assertNotNull(parserService);
    }
    @After
    public void teardown(){
        parserService = null;
        Assert.assertNull(parserService);
    }
    @Test
    public void testConvertIPAddress() {
        long value = 167772162;
        String result = parserService.convertIPAddress(value);
        Assert.assertEquals("10.0.0.2", result);
    }
    @Test
    public void convertBytetest() {
        byte value = 100;
        long result = parserService.convert(value);
        Assert.assertEquals(36, result);
    }
    @Test
    public void addValueTest(){
        String name = "Name";
        String value = "value";
        parserService.addValue(name, value);
    }
    @Test
    public void convertByteArrayTest(){
        byte[] b = "[B@19dbc5c".getBytes();
        String result = parserService.convert(b, 5, 4);
        Assert.assertEquals("1684169525", result);
    }
    @Test
    public void toStringTest(){
        String result = parserService.toString();
        /*The packet sent for test has nothing, 0 PDU. Thus only verifying the Netflow header.*/
        String expectedResult = "version=0,sysUpTime=0,unix_secs=0,unix_nsecs=0,flow_sequence=0,engine_type=0,engine_id=0,samplingInterval=0";
        Assert.assertEquals(expectedResult, result);
    }
    @Test
    public void getRecordAttribTest(){
        Object obj = parserService.getRecordAttributes();
        Assert.assertNotNull(obj);
    }
    @After
    public void tearDown(){
        parserService = null;
    }
}