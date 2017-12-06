/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.spi.util;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import javax.xml.transform.stream.StreamSource;
import jline.internal.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data Encrypter is a simple API to encrypt and decrypt strings based on a key generated by the GenerateKey class.
 * If the key does not exist, it will generate it once and the use the same key to encrypt and decrypt the Strings.
 * The usage is very simple:
 *  Use the method "encrypt" with a String to encrypt, the result is an encrypted String.
 *  Use the method "decrypt" with an encrypted String to decrypt it.
 *  Please note, if a string is encrypted with a key and a new key is generated afterwards, you will be unable to
 *  decrypt the strings.
 *
 * @author saichler@gmail.com
 **/
public class DataEncrypter {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataEncrypter.class);
    public static final String ENCRYPTED_TAG = "Encrypted:";
    public static final String TAG_PASSWORD = "password";

    private static final Set<Tag> TAGS_TO_ENCRYPT = ConcurrentHashMap.newKeySet();
    private static final String CIPHER_PADDING = "AES/CFB8/NoPadding";

    static {
        init();
    }

    /**
     * Read the key and install the different tags to seek in the xml config files
     * Currently it is only seeking "password".
     **/
    private static void init() {
        TAGS_TO_ENCRYPT.add(new Tag(TAG_PASSWORD));

        if (GenerateKey.getKey() == null) {
            File keyFile = new File(GenerateKey.PATH_TO_KEY);
            if (keyFile.exists()) {
                int length = (int) keyFile.length();
                byte[] keyData = new byte[length];
                FileInputStream in = null;
                try {
                    in = new FileInputStream(keyFile);
                    int actual = in.read(keyData);
                    if (actual < length) {
                        Log.warn("Expected {} bytes read, actual {}", length, actual);
                    }

                } catch (FileNotFoundException e) {
                    LOGGER.error("Key file was not found", e);
                } catch (IOException e) {
                    LOGGER.error("Could not read key file", e);
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {
                            LOGGER.error("Could not close key file", e);
                        }
                    }
                }
                GenerateKey.setKey(new SecretKeySpec(keyData, GenerateKey.KEY_METHOD));
            } else {
                GenerateKey.generateKey();
            }
        }
    }

    public static void addTag(final String tag) {
        TAGS_TO_ENCRYPT.add(new Tag(tag));
    }

    /**
     * Encrypt a string and return its encrypted string representation with a prefix encrypted tag.
     * The method with encrypt the string only if there is a valid key in the GenerateKey.key member.
     * If the string is already encrypted, it will not encrypt it twice.
     *
     * @param  str  The String to be encrypted
     * @return      The Encrypted String representation with a prefix tag
     */
    public static String encrypt(final String str) {
        // No Key, hence disabled
        if (GenerateKey.getKey() == null) {
            return str;
        }

        if (str == null) {
            return str;
        }
        //already encrypted
        if (str.startsWith(ENCRYPTED_TAG)) {
            return str;
        }

        Cipher cr = null;
        try {
            cr = Cipher.getInstance(CIPHER_PADDING);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Failed to get Cipher",e);
            return str;
        } catch (NoSuchPaddingException e) {
            LOGGER.error("Failed to set Padding",e);
            return str;
        }

        try {
            cr.init(Cipher.ENCRYPT_MODE, GenerateKey.getKey(), GenerateKey.getIvSpec());
        } catch (InvalidKeyException e) {
            LOGGER.error("Invalide key",e);
            return str;
        } catch (InvalidAlgorithmParameterException e) {
            LOGGER.error("Invalide algorithm",e);
            return str;
        }

        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        try (CipherOutputStream out = new CipherOutputStream(bout, cr)) {
            final byte[] data = str.getBytes(StandardCharsets.UTF_8);
            out.write(data);
            final byte[] encData = bout.toByteArray();
            return ENCRYPTED_TAG + DatatypeConverter.printBase64Binary(encData);
        } catch (IOException e) {
            LOGGER.error("Could not encrypt String", e);
        }

        return str;
    }

    /**
     * Decrypt a tagged encrypted by the "encrypt" method. Will not decrypt if the string is not encrypted.
     * @param  encStr  The Tagged Encrypted String
     * @return         The unencrypted string.
     */
    @SuppressFBWarnings("RR_NOT_CHECKED")
    public static String decrypt(final String encStr) {
        // No Key, hence disabled
        if (GenerateKey.getKey() == null) {
            return encStr;
        }

        if (encStr == null) {
            return null;
        }

        //is not encrypted with this util
        if (!encStr.startsWith(ENCRYPTED_TAG)) {
            return encStr;
        }

        Cipher cr = null;
        try {
            cr = Cipher.getInstance(CIPHER_PADDING);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Can't find Cipher",e);
            return encStr;
        } catch (NoSuchPaddingException e) {
            LOGGER.error("Failed to set Padding",e);
            return encStr;
        }

        try {
            cr.init(Cipher.DECRYPT_MODE, GenerateKey.getKey(), GenerateKey.getIvSpec());
        } catch (InvalidKeyException e) {
            LOGGER.error("Invalide Key",e);
            return encStr;
        } catch (InvalidAlgorithmParameterException e) {
            LOGGER.error("Invalide algorithm",e);
            return encStr;
        }

        final byte[] encData = DatatypeConverter.parseBase64Binary(encStr.substring(ENCRYPTED_TAG.length()));
        final ByteArrayInputStream bin = new ByteArrayInputStream(encData);
        try (CipherInputStream in = new CipherInputStream(bin, cr)) {
            final byte[] data = new byte[encStr.length() * 2];
            in.read(data);
            return new String(data, StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            LOGGER.error("Could not decrypt string", e);
        }

        return encStr;
    }

    /**
     * Loads the a text file content.
     */
    @Nonnull
    private static String loadFileContent(final String fileName) {
        final File file = new File(fileName);
        final byte[] data = new byte[(int) file.length()];
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            int actual = in.read(data);
            if (actual < data.length) {
                Log.warn("Expected {} bytes read, actual {}", data.length, actual);
            }
        } catch (IOException e) {
            LOGGER.error("Could not read file", e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                LOGGER.error("could not close stream", e);
            }
        }
        return new String(data, StandardCharsets.UTF_8);
    }

    /**
     * Receive a filename and seeks all the defined tags in it and encrypt them.
     * @param  filename  The filename to seek the tags and encrypt
     */
    public static final void encryptCredentialAttributes(final String filename) {
        // No Key, hence disabled
        if (GenerateKey.getKey() == null) {
            return;
        }

        String fileContent = loadFileContent(filename);
        String fileContentInLowerCase = fileContent.toLowerCase(Locale.getDefault());
        boolean encryptedAValue = false;

        for (Tag t : TAGS_TO_ENCRYPT) {
            t.reset();
            String data = t.next(fileContentInLowerCase, fileContent);
            while (data != null) {
                if (data.startsWith(ENCRYPTED_TAG)) {
                    data = t.next(fileContentInLowerCase, fileContent);
                } else {
                    encryptedAValue = true;
                    final String eData = encrypt(data);
                    fileContent = fileContent.substring(0, t.tagLabelEnd + 1) + eData
                            + fileContent.substring(t.tagDataEnd);
                    fileContentInLowerCase = fileContent.toLowerCase(Locale.getDefault());
                    data = t.next(fileContentInLowerCase, fileContent);
                }
            }
        }
        if (encryptedAValue) {
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(filename);
                out.write(fileContent.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                LOGGER.error("", e);
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        LOGGER.error("", e);
                    }
                }
            }
        }
    }

    /**
     * Receive a filename and seeks all the defined tags in it and decrypt them.
     * @param  filename - The filename to seek the tags and decrypt
     * @return - StreamSource object
     */
    public static StreamSource decryptCredentialAttributes(final String filename) {
        if (filename == null) {
            return null;
        }

        // No Key, hence disabled
        if (GenerateKey.getKey() == null) {
            return new StreamSource(new File(filename));
        }

        String fileContent = loadFileContent(filename);

        int index = fileContent.indexOf(ENCRYPTED_TAG);
        while (index != -1) {
            int index1 = fileContent.indexOf("<", index);
            String encyptedData = fileContent.substring(index, index1);
            String data = decrypt(encyptedData);
            fileContent = fileContent.substring(0, index) + data + fileContent.substring(index1);
            index = fileContent.indexOf(ENCRYPTED_TAG);
        }

        return new StreamSource(new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * The Tag class represents a tag in an xml file that its value should be encrypted,
     * For example, if the tag is "&lt;password&gt;" and value is "admin",
     * e.g. "&lt;password&gt;admin&lt;/password&gt;", the "admin" should be encrypted in the XML file.
     */
    private static final class Tag {
        private final String startTag;
        private int tagLabelStart = -1;
        private int tagLabelEnd = -1;
        private int tagDataEnd = -1;

        Tag(String tag) {
            this.startTag = "<" + tag;
        }

        /*
            Reset the locations of tag last position.
         */
        private void reset() {
            this.tagLabelStart = -1;
            this.tagLabelEnd = -1;
            this.tagDataEnd = -1;
        }

        /*
            Gets the next tag value to encrypt.
         */
        private String next(String lowerCase, String originalTXT) {
            tagLabelStart = lowerCase.indexOf(startTag, tagLabelStart + 1);
            if (tagLabelStart == -1) {
                return null;
            }

            tagLabelEnd = lowerCase.indexOf(">", tagLabelStart);
            if (tagLabelEnd == -1) {
                return null;
            }

            tagDataEnd = lowerCase.indexOf("<", tagLabelStart + 1);
            if (tagDataEnd == -1 || tagDataEnd < tagLabelEnd) {
                return null;
            }

            return originalTXT.substring(tagLabelEnd + 1, tagDataEnd);
        }
    }
}
