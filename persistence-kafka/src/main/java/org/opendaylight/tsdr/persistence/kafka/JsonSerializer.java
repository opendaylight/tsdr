/*
 * Copyright (c) 2019 Bell. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Json serializer for kafka producer usage.
 */
public final class JsonSerializer {
    private static final Logger LOG = LoggerFactory.getLogger(JsonSerializer.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static byte[] serialize(Object data) {
        if (data == null) {
            return new byte[0];
        }
        try {
            return OBJECT_MAPPER.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            LOG.error("Error serializing JSON message", e);
        }
        return new byte[0];
    }

    private JsonSerializer() {

    }
}
