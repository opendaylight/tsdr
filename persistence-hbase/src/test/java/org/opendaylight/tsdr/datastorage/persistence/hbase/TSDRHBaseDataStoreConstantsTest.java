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
import org.opendaylight.tsdr.persistence.hbase.TSDRHBaseDataStoreConstants;

/**
 * @author <a href="mailto:chaudhry.usama@xflowresearch.com">Chaudhry Muhammad Usama</a>
 * Created by Chaudhry Usama on 1/6/16.
 */

public class TSDRHBaseDataStoreConstantsTest {

    @Test
    public void testConstants() {
        TSDRHBaseDataStoreConstants tsdrhbaseDataStoreConstants = new TSDRHBaseDataStoreConstants();
        Assert.assertTrue(tsdrhbaseDataStoreConstants.COLUMN_FAMILY_NAME == "c1");
        Assert.assertTrue(tsdrhbaseDataStoreConstants.ROWKEY_SPLIT == "_");
        Assert.assertTrue(tsdrhbaseDataStoreConstants.COLUMN_QUALIFIER_NAME == "raw");
        Assert.assertTrue(tsdrhbaseDataStoreConstants.LOGRECORD_FULL_TEXT == "RecordFullText");
        Assert.assertTrue(tsdrhbaseDataStoreConstants.MAX_QUERY_RECORDS == 1000);
    }

}
