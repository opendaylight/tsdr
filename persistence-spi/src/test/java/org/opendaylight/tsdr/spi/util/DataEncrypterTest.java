/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.spi.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.crypto.SecretKey;
import javax.xml.transform.stream.StreamSource;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class DataEncrypterTest {

    private final SecretKey key = null;

    @BeforeClass
    public static final void setup() {
        File file = new File(".bashrck");
        if (file.exists()) {
            file.delete();
        }
    }

    @Before
    public void init() {
        GenerateKey.generateKey();
    }

    @Test
    public void testEncryptDecryptString() {
        String encString = DataEncrypter
                .encrypt("Hello World, This is a long string to test long data encryption util with long string....");
        Assert.assertNotNull(encString);
        Assert.assertNotEquals(
                "Hello World, This is a long string to test long data encryption util with long string....", encString);
        String decString = DataEncrypter.decrypt(encString);
        Assert.assertEquals("Hello World, This is a long string to test long data encryption util with long string....",
                decString);
    }

    @Test
    public void testEncrypDecryptNull() {
        String encString = DataEncrypter.encrypt(null);
        Assert.assertNull(encString);
        String decString = DataEncrypter.decrypt(encString);
        Assert.assertNull(decString);
    }

    @Test
    public void testEncrypDecryptKeyNull() {
        GenerateKey.setKey(null);
        String encString = DataEncrypter.encrypt("Test");
        Assert.assertNotNull(encString);
        Assert.assertEquals(encString, "Test");
        String decString = DataEncrypter.decrypt(encString);
        Assert.assertEquals(encString, decString);
    }

    @Test
    public void testEncryptDecryptAttributes() throws IOException {
        File file = new File("test.xml");

        FileOutputStream out = new FileOutputStream(file);
        out.write("<password>hello world</password>".getBytes());
        out.close();
        DataEncrypter.encryptCredentialAttributes("test.xml");

        file = new File("test.xml");
        byte[] data = new byte[(int) file.length()];
        FileInputStream in = new FileInputStream(file);
        in.read(data);
        in.close();
        String encOutput = new String(data).trim();
        Assert.assertNotEquals(encOutput, "<password>hello world</password>");

        StreamSource source = DataEncrypter.decryptCredentialAttributes("test.xml");
        data = new byte[100];
        source.getInputStream().read(data);
        String output = new String(data).trim();
        Assert.assertEquals("<password>hello world</password>", output);
        file.delete();
    }

    @AfterClass
    public static final void end() {
        File file = new File(".bashrck");
        if (file.exists()) {
            file.delete();
        }
    }
}
