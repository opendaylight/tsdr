/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.collectors.cmc.mdsal;
import java.math.BigInteger;

import org.datasand.codec.EncodeDataContainer;
import org.datasand.codec.ISerializer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.Counter64;
/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class Counter64Serializer implements ISerializer{
    @Override
    public void encode(Object value, byte[] byteArray, int location) {
    }

    @Override
    public void encode(Object value, EncodeDataContainer ba) {
        if(value==null){
            ba.getEncoder().encodeNULL(ba);
        }else
            ba.getEncoder().encodeString(value.toString(), ba);
    }
    @Override
    public Object decode(byte[] byteArray, int location, int length) {
        return null;
    }
    @Override
    public Object decode(EncodeDataContainer ba, int length) {
        String value = ba.getEncoder().decodeString(ba);
        if(value==null)
            return null;
        return new Counter64(new BigInteger(value));
    }
    public String getShardName(Object obj) {
        return "Default";
    }
    public String getRecordKey(Object obj) {
        return null;
    }
}
