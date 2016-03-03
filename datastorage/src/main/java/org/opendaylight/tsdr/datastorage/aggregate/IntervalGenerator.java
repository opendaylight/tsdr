/*
 * Copyright (c) 2016 The OpenNMS Group Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.datastorage.aggregate;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Splits a range into intervals of a fixed size.
 *
 * @author <a href="mailto:jesse@opennms.org">Jesse White</a>
 */
public class IntervalGenerator implements Iterable<Long>, Iterator<Long> {

    private final Long end;
    private final Long interval;
    private Long current;

    public IntervalGenerator(Long start, Long end, Long interval) {
        this.end = Objects.requireNonNull(end);
        this.interval = Objects.requireNonNull(interval);
        current = start;
    }

    @Override
    public Iterator<Long> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        return current <= end;
    }

    @Override
    public Long next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        try {
            return current;
        } finally {
            current += interval;
        }
    }
}
