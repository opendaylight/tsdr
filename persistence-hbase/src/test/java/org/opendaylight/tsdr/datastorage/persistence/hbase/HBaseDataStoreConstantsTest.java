/*
 * Copyright (c) 2016 xFlow Research Inc. and others.  All rights reserved
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.datastorage.persistence.hbase;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Test;
import org.opendaylight.tsdr.persistence.hbase.HBaseDataStoreConstants;

/**
 * @author <a href="mailto:chaudhry.usama@xflowresearch.com">Chaudhry Muhammad Usama</a>
 * Created by Chaudhry Usama on 1/6/16.
 */


public class HBaseDataStoreConstantsTest {

    @Test
    public void testConstants() {
        HBaseDataStoreConstants hbaseDataStoreConstants = new HBaseDataStoreConstants();
        Assert.assertTrue(hbaseDataStoreConstants.HBASE_CLIENT_PAUSE == "hbase.client.pause");
        Assert.assertTrue(hbaseDataStoreConstants.HBASE_CLIENT_RETRIES_NUMBER == "hbase.client.retries.number");
        Assert.assertTrue(hbaseDataStoreConstants.ZOOKEEPER_CLIENTPORT == "hbase.zookeeper.property.clientPort");
        Assert.assertTrue(hbaseDataStoreConstants.ZOOKEEPER_QUORUM == "hbase.zookeeper.quorum");
    }

}
