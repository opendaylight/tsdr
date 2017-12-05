/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.spi.command.completer;

import java.util.List;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;

/**
 * This command is provided to get a list of metrics based on arguments passed.
 *
 * @author <a href="mailto:syedbahm@cisco.com">Basheeruddin Ahmed</a>
 *
 */
public class ListMetricsCommandCompleter implements Completer {

    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {
        StringsCompleter completer = new StringsCompleter();
        for (DataCategory c : DataCategory.values()) {
            completer.getStrings().add(c.name());
        }
        return completer.complete(buffer, cursor, candidates);
    }
}
