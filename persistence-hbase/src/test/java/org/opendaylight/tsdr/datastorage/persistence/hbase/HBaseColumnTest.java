/*
 * Copyright (c) 2015 xFlow Research Inc. and others.  All rights reserved
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.datastorage.persistence.hbase;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.tsdr.persistence.hbase.HBaseColumn;

/**
 * @author <a href="mailto:chaudhry.usama@xflowresearch.com">Chaudhry Muhammad Usama</a>
 * Created by Chaudhry Usama on 1/6/16.
 */

public class HBaseColumnTest {
    public HBaseColumn hBaseColumn = null;

    @Before
    public void setUp(){
        hBaseColumn = new HBaseColumn("columnFamily","columnQualifier","value");
    }

    @Test
    public void testGetColumnFamily() {
        boolean result = false;
        System.out.print(hBaseColumn.getColumnFamily());
        result = hBaseColumn.getColumnFamily() == "columnFamily";
        assertTrue(result);
    }

    @Test
    public void testGetColumnQualifier() {
        boolean result = false;
        System.out.print(hBaseColumn.getColumnQualifier());
        result = hBaseColumn.getColumnQualifier() == "columnQualifier";
        assertTrue(result);
    }

    @Test
    public void testGetValue() {
        boolean result = false;
        System.out.print(hBaseColumn.getValue());
        result = hBaseColumn.getValue() == "value";
        assertTrue(result);
    }

    @Test
    public void testSetColumnFamily() {
        boolean result = false;
        hBaseColumn.setColumnFamily("newColumnFamily");
        result = hBaseColumn.getColumnFamily() == "newColumnFamily";
        assertTrue(result);
    }

    @Test
    public void testSetColumnQualifier() {
        boolean result = false;
        hBaseColumn.setColumnQualifier("newColumnQualifier");
        result = hBaseColumn.getColumnQualifier() == "newColumnQualifier";
        assertTrue(result);
    }

    @Test
    public void testSetValue() {
        boolean result = false;
        hBaseColumn.setValue("newvalue");
        result = hBaseColumn.getValue() == "newvalue";
        assertTrue(result);
    }

    @Test
    public void testSetTimeStamp() {
        boolean result = false;
        hBaseColumn.setTimeStamp(1000);
        result = hBaseColumn.getTimeStamp() == 1000;
        assertTrue(result);
    }

    @After
    public void tearDown() {
        hBaseColumn = null;
    }

}
