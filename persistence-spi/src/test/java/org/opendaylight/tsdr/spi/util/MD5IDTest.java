/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.spi.util;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author saichler@gmail.com
 **/
public class MD5IDTest {
    @Test
    public void testCreateStringMD5(){
        MD5ID id1 = MD5ID.createTSDRID("Hello World");
        MD5ID id2 = MD5ID.createTSDRID("Hello World");
        Assert.assertEquals(id1,id2);
        Assert.assertTrue(id1.equals(id2));
        Assert.assertEquals(id1.hashCode(),id2.hashCode());
    }

    @Test
    public void testCreate2LongMD5(){
        MD5ID id1 = MD5ID.createTSDRID(12345L,67890L);
        Assert.assertEquals(12345L,id1.getMd5Long1());
        Assert.assertEquals(67890L,id1.getMd5Long2());
        MD5ID id2 = MD5ID.createTSDRID(12345L,67890L);
        Assert.assertEquals(id1,id2);
        MD5ID id3 = MD5ID.createTSDRID(12345L,67891L);
        Assert.assertNotEquals(id2,id3);
    }

    @Test
    public void testCreateByteMD5(){
        MD5ID id1 = MD5ID.createTSDRID("Hello World".getBytes());
        MD5ID id2 = MD5ID.createTSDRID("Hello World".getBytes());
        Assert.assertEquals(id1,id2);
    }

    @Test
    public void testCreateAlreadyHash(){
        MD5ID id1 = MD5ID.createTSDRID("Hello World");
        MD5ID id2 = MD5ID.createTSDRIDAlreadyHash(id1.toByteArray());
        Assert.assertEquals(id1,id2);
    }
}
