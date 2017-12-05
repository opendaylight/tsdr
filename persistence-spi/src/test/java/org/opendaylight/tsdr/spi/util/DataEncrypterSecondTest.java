/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.spi.util;

import java.io.File;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class DataEncrypterSecondTest {
    @BeforeClass
    public static void beforeClass() {
        GenerateKey.generateKey();
        GenerateKey.setKey(null);
    }

    @Test
    public void testDataEncrypterLoadKey() {
        DataEncrypter.encrypt("Test");
    }

    @AfterClass
    public static final void end() {
        File file = new File(".bashrck");
        if (file.exists()) {
            file.delete();
        }
    }
}
