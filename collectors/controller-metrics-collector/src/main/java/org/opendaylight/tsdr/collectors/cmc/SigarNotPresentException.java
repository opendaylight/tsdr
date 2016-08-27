/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.collectors.cmc;

public class SigarNotPresentException extends Exception {
    // no additional functionality required beyond pass-through constructors

    public SigarNotPresentException() {
        super();
    }

    public SigarNotPresentException(String message) {
        super(message);
    }

    public SigarNotPresentException(String message, Throwable cause) {
        super(message, cause);
    }

    public SigarNotPresentException(Throwable cause) {
        super(cause);
    }

}
