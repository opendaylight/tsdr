/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.spi.util;

import java.io.File;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author saichler@gmail.com
 **/
public class GenerateKeyTest {
    @Test
    public void testGenerateKey(){
        File f = new File(GenerateKey.KEY_FILE_NAME);
        if(f.exists()){
            f.delete();
        }
        GenerateKey.main(null);
        f = new File(GenerateKey.KEY_FILE_NAME);
        Assert.assertTrue(f.exists());
    }
}
