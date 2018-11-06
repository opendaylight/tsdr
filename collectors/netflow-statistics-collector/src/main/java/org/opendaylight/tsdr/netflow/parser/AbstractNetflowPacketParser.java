/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.netflow.parser;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
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
    private int position;

    protected AbstractNetflowPacketParser(final byte[] data, final int version, final int initialPosition,
            TSDRLogRecordBuilder recordBuilder, Consumer<TSDRLogRecordBuilder> callback) {
        this.data = data;
        this.position = initialPosition;
        this.recordBuilder = recordBuilder;
        this.callback = callback;

        addHeaderAttribute("version", Integer.toString(version));
    }

    protected AbstractNetflowPacketParser(AbstractNetflowPacketParser other, int fromPosition, int bytesToCopy) {
        data = new byte[bytesToCopy];
        System.arraycopy(other.data, fromPosition, data, 0, bytesToCopy);

        this.position = 0;
        this.recordBuilder = other.recordBuilder;
        this.callback = other.callback;

        headerAttributes.addAll(other.headerAttributes);
    }

    protected TSDRLogRecordBuilder recordBuilder() {
        return recordBuilder;
    }

    protected Consumer<TSDRLogRecordBuilder> callback() {
        return callback;
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

    public long parseInt() {
        return parseLong(4);
    }

    protected String parseIntString() {
        return Long.toString(parseInt());
    }

    public int parseShort() {
        return (int) parseLong(2);
    }

    protected String parseShortString() {
        return Integer.toString(parseShort());
    }

    protected String parseByteString() {
        return Integer.toString(parseByte());
    }

    public int parseByte() {
        return (int) parseLong(1);
    }

    public String parseIPv4Address() {
        return parseOctetString(4);
    }

    public String parseOctetString(int length) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++, position++) {
            if (i > 0) {
                builder.append('.');
            }

            builder.append(data[position] & 0xff);
        }

        return builder.toString();
    }

    public String parseIPv6Address() {
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

    public String parseMACAddress() {
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

    public String parseString(int length) {
        return new String(parseBytes(length), Charset.defaultCharset());
    }

    public byte[] parseBytes(int len) {
        byte[] ret = new byte[len];
        System.arraycopy(data, position, ret, 0, len);
        position += len;
        return ret;
    }

    public long parseLong(int len) {
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

    public long parseSignedLong(int len) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(parseBytes(len)))) {
            switch (len) {
                case 1:
                    return in.readByte();
                case 2:
                    return in.readShort();
                case 4:
                    return in.readInt();
                case 8:
                    return in.readLong();
                default:
                    return 0;
            }
        } catch (IOException e) {
            LOG.error("Error parsing signed long", e);
            return 0;
        }
    }

    protected void skipPadding(int start, int totalLength) {
        int padding = totalLength - (position() - start);
        skip(padding);

        LOG.debug("Skip padding: {}", padding);
    }
}
