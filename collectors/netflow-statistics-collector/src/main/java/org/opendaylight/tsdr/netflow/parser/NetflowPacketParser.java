/*
 * Copyright (c) 2018 Inocybe Technologies and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.netflow.parser;

import java.util.List;
import java.util.function.BiConsumer;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.tsdrlog.RecordAttributes;

/**
 * Interface that parses netflow packets into log records.
 *
 * @author Thomas Pantelis
 */
public interface NetflowPacketParser {
    void parseRecords(BiConsumer<List<RecordAttributes>, String> callback);
}
