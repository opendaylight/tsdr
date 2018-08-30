/*
 * Copyright © 2018 Kontron Canada Inc and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.log.utils;

import org.ops4j.pax.logging.spi.PaxLevel;

public class LogLevel implements PaxLevel {
    @Override
    public boolean isGreaterOrEqual(PaxLevel paxLevel) {
        return false;
    }

    @Override
    public int toInt() {
        return 0;
    }

    @Override
    public int getSyslogEquivalent() {
        return 0;
    }

    public String toString() {
        return "TEST";
    }
}
