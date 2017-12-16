/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.spi.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class to generate an AES key for encrypting/decrypting  strings.
 * you just need to execute the main and copy the .bashrck file to the root of controller.
 * If you are using the DataEncrypter, it will do it for you.
 * to generate a key just execute the main directory with no arguments.
 * @author saichler@gmail.com
 **/
public final class GenerateKey {
    public static final int KEY_ENCRYPTION_SIZE = 128;
    public static final String KEY_METHOD = "AES";

    private static final Logger LOG = LoggerFactory.getLogger(GenerateKey.class);
    private static byte[] iv = { 0, 4, 0, 0, 6, 81, 0, 8, 0, 0, 0, 0, 0, 43, 0,1 };
    private static IvParameterSpec ivspec = new IvParameterSpec(iv);

    static final String KEY_FILE_NAME = ".bashrck";
    static final String PATH_TO_KEY = "." + File.separator + KEY_FILE_NAME;

    private static SecretKey key;

    private GenerateKey(){
    }

    public static void generateKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(KEY_METHOD);
            keyGen.init(KEY_ENCRYPTION_SIZE);
            key = keyGen.generateKey();
            byte[] keyData = key.getEncoded();
            FileOutputStream out = new FileOutputStream(PATH_TO_KEY);
            out.write(keyData);
            out.close();
        } catch (NoSuchAlgorithmException | IOException e) {
            LOG.error("Failed to generate a key", e);
        }
    }

    public static void setKey(SecretKey newKey) {
        key = newKey;
    }

    public static SecretKey getKey() {
        return key;
    }

    public static IvParameterSpec getIvSpec() {
        return ivspec;
    }

    public static void main(String[] args) {
        LOG.info("Generating Key... ");
        generateKey();
        if (key != null) {
            LOG.info("Done!");
        }
    }
}
