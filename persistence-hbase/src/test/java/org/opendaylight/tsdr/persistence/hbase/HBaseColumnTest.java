/*
 * Copyright (c) 2016 xFlow Research Inc. and others.  All rights reserved
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.hbase;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for HBaseColumn.
 *
 * @author <a href="mailto:chaudhry.usama@xflowresearch.com">Chaudhry Muhammad Usama</a>
 */
public class HBaseColumnTest {
    private HBaseColumn hbaseColumn;

    @Before
    public void setUp() {
        hbaseColumn = new HBaseColumn("columnFamily","columnQualifier","value");
    }

    @Test
    public void testGetColumnFamily() {
        assertTrue("columnFamily".equals(hbaseColumn.getColumnFamily()));
    }

    @Test
    public void testGetColumnQualifier() {
        assertTrue("columnQualifier".equals(hbaseColumn.getColumnQualifier()));
    }

    @Test
    public void testGetValue() {
        assertTrue("value".equals(hbaseColumn.getValue()));
    }

    @Test
    public void testSetColumnFamily() {
        hbaseColumn.setColumnFamily("newColumnFamily");
        assertTrue("newColumnFamily".equals(hbaseColumn.getColumnFamily()));
    }

    @Test
    public void testSetColumnQualifier() {
        hbaseColumn.setColumnQualifier("newColumnQualifier");
        assertTrue("newColumnQualifier".equals(hbaseColumn.getColumnQualifier()));
    }

    @Test
    public void testSetValue() {
        hbaseColumn.setValue("newvalue");
        assertTrue("newvalue".equals(hbaseColumn.getValue()));
    }

    @Test
    public void testSetTimeStamp() {
        hbaseColumn.setTimeStamp(1000);
        assertTrue(hbaseColumn.getTimeStamp() == 1000);
    }
}
