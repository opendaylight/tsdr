/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.cassandra;

import java.security.MessageDigest;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class MD5Identifier {
    private long a = 0;
    private long b = 0;
    private static MessageDigest md = null;
    private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static WriteLock writeLock = lock.writeLock();

    public MD5Identifier(long _a, long _b) {
        this.a = _a;
        this.b = _b;
    }

    public MD5Identifier(byte encodedRecordKey[]) {
        if (md == null) {
            try {
                writeLock.lock();
                if (md == null) {
                    try {
                        md = MessageDigest.getInstance("MD5");
                    } catch (Exception err) {
                        err.printStackTrace();
                    }
                }
            } finally {
                writeLock.unlock();
            }
        }

        byte by[] = null;

        try {
            writeLock.lock();
            md.update(encodedRecordKey);
            by = md.digest();
        } finally {
            writeLock.unlock();
        }

        a = (a << 8) + (by[0] & 0xff);
        a = (a << 8) + (by[1] & 0xff);
        a = (a << 8) + (by[2] & 0xff);
        a = (a << 8) + (by[3] & 0xff);
        a = (a << 8) + (by[4] & 0xff);
        a = (a << 8) + (by[5] & 0xff);
        a = (a << 8) + (by[6] & 0xff);
        a = (a << 8) + (by[7] & 0xff);

        b = (b << 8) + (by[8] & 0xff);
        b = (b << 8) + (by[9] & 0xff);
        b = (b << 8) + (by[10] & 0xff);
        b = (b << 8) + (by[11] & 0xff);
        b = (b << 8) + (by[12] & 0xff);
        b = (b << 8) + (by[13] & 0xff);
        b = (b << 8) + (by[14] & 0xff);
        b = (b << 8) + (by[15] & 0xff);
    }

    public long getA(){
        return this.a;
    }

    public long getB(){
        return this.b;
    }
}
