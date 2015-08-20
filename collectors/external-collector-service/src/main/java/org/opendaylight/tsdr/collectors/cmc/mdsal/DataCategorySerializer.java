/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.collectors.cmc.mdsal;
import org.datasand.codec.EncodeDataContainer;
import org.datasand.codec.ISerializer;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class DataCategorySerializer implements ISerializer{
    @Override
    public void encode(Object value, byte[] byteArray, int location) {
    }

    @Override
    public void encode(Object value, EncodeDataContainer ba) {
        DataCategory element = (DataCategory) value;
        ba.setCurrentAttributeName("IntValue");
        ba.getEncoder().encodeInt32(element.getIntValue(), ba);
        ba.setCurrentAttributeName("DeclaringClass");
    }
    @Override
    public Object decode(byte[] byteArray, int location, int length) {
        return null;
    }
    @Override
    public Object decode(EncodeDataContainer ba, int length) {
        DataCategory instance = DataCategory.forValue(ba.getEncoder().decodeInt32(ba));
        return instance;
    }
    public String getShardName(Object obj) {
        return "Default";
    }
    public String getRecordKey(Object obj) {
        return null;
    }
}
