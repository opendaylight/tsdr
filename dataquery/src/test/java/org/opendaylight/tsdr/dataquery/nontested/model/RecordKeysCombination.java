/*
 * Copyright (c) 2015 Scott Melton All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.dataquery.nontested.model;

/**
 * This class represents a combination key represented in the metric record.
 * The naming smells but as close to the "what" this data is as I know.  Maybe
 * come up with a better name?
 *
 * @author <a href="mailto:smelton2@uccs.edu">Scott Melton</a>
 */

public class RecordKeysCombination {
    private final String keyName;
    private final String keyValue;

    public RecordKeysCombination(String keyName, String keyValue) {
        this.keyName = keyName;
        this.keyValue = keyValue;
    }

    public String getKeyName() {
        return keyName;
    }

    public String getKeyValue() {
        return keyValue;
    }
}
