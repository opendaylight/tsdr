/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.spi.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class encode the tsdr key into MD5 hash code to serve as a row key identifier for a single metric
 * so when pushing a sample we do not need to persist the whole tsdr string key but only two longs.
 * @author - Sharon Aicler (saichler@gmail.com)
 */
public class MD5ID {

    private static final Logger LOGGER = LoggerFactory.getLogger(MD5ID.class);
    private static MessageDigest md = null;
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static final WriteLock writeLock = lock.writeLock();

    private final long md5Long1;
    private final long md5Long2;
    private byte[] by = null;

    static {
        if (md == null) {
            if (md == null) {
                try {
                    md = MessageDigest.getInstance("MD5");
                } catch (NoSuchAlgorithmException e) {
                    throw new ExceptionInInitializerError("Could not initialize MD5 Algorithm");
                }
            }
        }
    }

    private MD5ID(long[] l, int offset) {
        this.md5Long1 = l[offset];
        this.md5Long2 = l[offset + 1];
    }

    private MD5ID(long md5long1, long md5long2) {
        this.md5Long1 = md5long1;
        this.md5Long2 = md5long2;
    }

    /**
      This constructor is calculating two longs that represent the MD5 hashing of the input byte array.
      @param byteArray - A byte array to be hashed or an already hashed byte array.
      @param alreadyHashed - If the byte array is already hashed, don't hash it again.
     **/
    private MD5ID(byte byteArray[],boolean alreadyHashed) {

        if(!alreadyHashed) {
            try {
                writeLock.lock();
                md.update(byteArray);
                by = md.digest();
            } finally {
                writeLock.unlock();
            }
        }

        //Calculate the two longs
        long md5Long1 = (0 << 8) + (by[0] & 0xff);
        md5Long1 = (md5Long1 << 8) + (by[1] & 0xff);
        md5Long1 = (md5Long1 << 8) + (by[2] & 0xff);
        md5Long1 = (md5Long1 << 8) + (by[3] & 0xff);
        md5Long1 = (md5Long1 << 8) + (by[4] & 0xff);
        md5Long1 = (md5Long1 << 8) + (by[5] & 0xff);
        md5Long1 = (md5Long1 << 8) + (by[6] & 0xff);
        md5Long1 = (md5Long1 << 8) + (by[7] & 0xff);
        this.md5Long1 = md5Long1;

        long md5Long2 = (0 << 8) + (by[8] & 0xff);
        md5Long2 = (md5Long2 << 8) + (by[9] & 0xff);
        md5Long2 = (md5Long2 << 8) + (by[10] & 0xff);
        md5Long2 = (md5Long2 << 8) + (by[11] & 0xff);
        md5Long2 = (md5Long2 << 8) + (by[12] & 0xff);
        md5Long2 = (md5Long2 << 8) + (by[13] & 0xff);
        md5Long2 = (md5Long2 << 8) + (by[14] & 0xff);
        md5Long2 = (md5Long2 << 8) + (by[15] & 0xff);
        this.md5Long2 = md5Long2;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(md5Long1, md5Long2);
    }

    @Override
    public boolean equals(Object obj) {
        MD5ID other = (MD5ID) obj;
        if (other.md5Long1 == md5Long1 && other.md5Long2 == md5Long2)
            return true;
        return false;
    }

    public long getMd5Long1() {
        return this.md5Long1;
    }

    public long getMd5Long2() {
        return this.md5Long2;
    }

    public long[] toLongArray() {
        return new long[] {md5Long1, md5Long2};
    }

    public static final MD5ID createTSDRID(final String tsdrKey) {
        return new MD5ID(tsdrKey.getBytes(),false);
    }

    public static final MD5ID createTSDRID(byte data[]) {
        return new MD5ID(data,false);
    }

    public static final MD5ID createTSDRIDAlreadyHash(byte data[]) {
        return new MD5ID(data,true);
    }

    public static MD5ID createTSDRID(long a, long b) {
        return new MD5ID(a, b);
    }
}