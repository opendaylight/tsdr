/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.netflow.parser;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.tsdrlog.RecordAttributes;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.tsdrlog.RecordAttributesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecordBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base for a NetflowPacketParser.
 *
 * @author Thomas Pantelis
 */
public abstract class AbstractNetflowPacketParser implements NetflowPacketParser {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractNetflowPacketParser.class);

    private final TSDRLogRecordBuilder recordBuilder;
    private final Consumer<TSDRLogRecordBuilder> callback;
    private final List<RecordAttributes> headerAttributes = new ArrayList<>();
    private final byte[] data;
    private final int totalRecordCount;
    private int position;

    protected AbstractNetflowPacketParser(final byte[] data, final int version, final int initialPosition,
            TSDRLogRecordBuilder recordBuilder, Consumer<TSDRLogRecordBuilder> callback) {
        this.data = data;
        this.position = initialPosition;
        this.recordBuilder = recordBuilder;
        this.callback = callback;

        this.totalRecordCount = parseShort();

        addHeaderAttribute("version", Integer.toString(version));

        LOG.debug("Packet version: {}, total record count: {}, headers: {}", version, totalRecordCount,
                headerAttributes);
    }

    protected AbstractNetflowPacketParser(AbstractNetflowPacketParser other, int fromPosition, int bytesToCopy) {
        data = new byte[bytesToCopy];
        System.arraycopy(other.data, fromPosition, data, 0, bytesToCopy);

        this.position = 0;
        this.recordBuilder = other.recordBuilder;
        this.callback = other.callback;
        this.totalRecordCount = Integer.MAX_VALUE;

        headerAttributes.addAll(other.headerAttributes);
    }

    protected TSDRLogRecordBuilder recordBuilder() {
        return recordBuilder;
    }

    protected Consumer<TSDRLogRecordBuilder> callback() {
        return callback;
    }

    protected int totalRecordCount() {
        return totalRecordCount;
    }

    protected List<RecordAttributes> headerAttributes() {
        return headerAttributes;
    }

    protected void addHeaderAttribute(String name, String value) {
        headerAttributes.add(newRecordAttributes(name, value));
    }

    protected static RecordAttributes newRecordAttributes(String name, String value) {
        return new RecordAttributesBuilder().setName(name).setValue(value).build();
    }

    protected boolean endOfData() {
        return position >= data.length;
    }

    protected void skip(int num) {
        position += num;
    }

    protected int position() {
        return position;
    }

    protected long parseInt() {
        return parseLong(4);
    }

    protected String parseIntString() {
        return Long.toString(parseInt());
    }

    protected int parseShort() {
        return (int) parseLong(2);
    }

    protected String parseShortString() {
        return Integer.toString(parseShort());
    }

    protected String parseByteString() {
        return Integer.toString((int) parseLong(1));
    }

    protected String parseIPv4Address() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 4; i++, position++) {
            if (i > 0) {
                builder.append('.');
            }

            builder.append(data[position] & 0xff);
        }

        return builder.toString();
    }

    protected String parseIPv6Address() {
        StringBuilder builder = new StringBuilder(39);
        for (int i = 0; i < 8; i++) {
            builder.append(Integer.toHexString(data[position + (i << 1)] << 8 & 0xff00
                    | data[position + (i << 1) + 1] & 0xff));

            if (i < 7) {
                builder.append(':');
            }
        }

        position += 16;
        return builder.toString();
    }

    protected String parseMACAddress() {
        StringBuilder builder = new StringBuilder(17);
        for (int i = 0; i < 6; i++, position++) {
            if (i > 0) {
                builder.append(':');
            }

            String term = Integer.toString(data[position] & 0xff, 16);
            if (term.length() == 1) {
                builder.append('0');
            }

            builder.append(term);
        }

        return builder.toString();
    }

    protected String parseString(int length) {
        return new String(parseBytes(length), Charset.defaultCharset());
    }

    protected byte[] parseBytes(int len) {
        byte[] ret = new byte[len];
        System.arraycopy(data, position, ret, 0, len);
        position += len;
        return ret;
    }

    protected long parseLong(int len) {
        long value = parseLong(data, position, len);
        position += len;
        return value;
    }

    static long parseLong(byte[] bytes, int off, int len) {
        long ret = 0;
        int done = off + len;
        for (int i = off; i < done; i++) {
            ret = (ret << 8 & 0xffffffff) + (bytes[i] & 0xff);
        }

        return ret;
    }
}
