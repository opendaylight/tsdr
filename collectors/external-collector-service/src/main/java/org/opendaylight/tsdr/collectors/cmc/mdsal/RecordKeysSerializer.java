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
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;
/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class RecordKeysSerializer implements ISerializer{
    @Override
    public void encode(Object value, byte[] byteArray, int location) {
    }

    @Override
    public void encode(Object value, EncodeDataContainer ba) {
        RecordKeys element = (RecordKeys) value;
        ba.setCurrentAttributeName("KeyName");
        ba.getEncoder().encodeString(element.getKeyName(), ba);
        ba.setCurrentAttributeName("KeyValue");
        ba.getEncoder().encodeString(element.getKeyValue(), ba);
        ba.setCurrentAttributeName("Augmentations");
        ba.getEncoder().encodeAugmentations(value, ba);
    }
    @Override
    public Object decode(byte[] byteArray, int location, int length) {
        return null;
    }
    @Override
    public Object decode(EncodeDataContainer ba, int length) {
        RecordKeysBuilder builder = new RecordKeysBuilder();
        ba.setCurrentAttributeName("KeyName");
        builder.setKeyName(ba.getEncoder().decodeString(ba));
        ba.setCurrentAttributeName("KeyValue");
        builder.setKeyValue(ba.getEncoder().decodeString(ba));
        ba.setCurrentAttributeName("Augmentations");
        ba.getEncoder().decodeAugmentations(builder, ba,RecordKeys.class);
        return builder.build();
    }
    public String getShardName(Object obj) {
        return "Default";
    }
    public String getRecordKey(Object obj) {
        return null;
    }
}
