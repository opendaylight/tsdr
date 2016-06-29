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
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.storetsdrmetricrecord.input.TSDRMetricRecordBuilder;
/**
 * @author Sharon Aicler(saichler@gmail.com)
 **/
public class TSDRMetricRecordSerializer implements ISerializer{
    @Override
    public void encode(Object value, byte[] byteArray, int location) {
    }

    @Override
    public void encode(Object value, EncodeDataContainer ba) {
        TSDRMetricRecord element = (TSDRMetricRecord) value;
        ba.setCurrentAttributeName("MetricName");
        ba.getEncoder().encodeString(element.getMetricName(), ba);
        ba.setCurrentAttributeName("MetricValue");
        ba.getEncoder().encodeObject(element.getMetricValue(), ba, org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Counter64.class);
        ba.setCurrentAttributeName("TimeStamp");
        ba.getEncoder().encodeInt64(element.getTimeStamp(), ba);
        ba.setCurrentAttributeName("NodeID");
        ba.getEncoder().encodeString(element.getNodeID(), ba);
        ba.setCurrentAttributeName("TSDRDataCategory");
        ba.getEncoder().encodeObject(element.getTSDRDataCategory(), ba, org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory.class);
        ba.setCurrentAttributeName("Augmentations");
        ba.getEncoder().encodeAugmentations(value, ba);
        ba.setCurrentAttributeName("RecordKeys");
        ba.getEncoder().encodeAndAddList(element.getRecordKeys(), ba,org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys.class);
    }
    @Override
    public Object decode(byte[] byteArray, int location, int length) {
        return null;
    }
    @Override
    public Object decode(EncodeDataContainer ba, int length) {
        TSDRMetricRecordBuilder builder = new TSDRMetricRecordBuilder();
        ba.setCurrentAttributeName("MetricName");
        builder.setMetricName(ba.getEncoder().decodeString(ba));
        ba.setCurrentAttributeName("MetricValue");
        builder.setMetricValue((org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Counter64)ba.getEncoder().decodeObject(ba));
        ba.setCurrentAttributeName("TimeStamp");
        builder.setTimeStamp(ba.getEncoder().decodeInt64(ba));
        ba.setCurrentAttributeName("NodeID");
        builder.setNodeID(ba.getEncoder().decodeString(ba));
        ba.setCurrentAttributeName("TSDRDataCategory");
        builder.setTSDRDataCategory((org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory)ba.getEncoder().decodeObject(ba));
        ba.setCurrentAttributeName("Augmentations");
        ba.getEncoder().decodeAugmentations(builder, ba,TSDRMetricRecord.class);
        ba.setCurrentAttributeName("RecordKeys");
        builder.setRecordKeys(ba.getEncoder().decodeAndList(ba,org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys.class));
        return builder.build();
    }
    public String getShardName(Object obj) {
        return "Default";
    }
    public String getRecordKey(Object obj) {
        return null;
    }
}
