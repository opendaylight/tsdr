/*
 * Copyright (c) 2016 TethrNet Technology Co.Ltd and others.  All rights reserved.
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.syslogs.server.decoder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opendaylight.tsdr.syslogs.server.decoder.Message.MessageBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.Facility;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.Severity;

/**
 * This Class is for message format decoding. Messages meet RFC5424 can be parsed:
 * Format of Syslog
 * &lt;PRI&gt;SEQ NO:HostName:Timestamp:Application[ProcessID]:%Facility-Severity-MNEMONIC:description
 *
 * @author Kun Chen(kunch@tethrnet.com)
 * @author Wenbo Hu(wenbhu@tethrnet.com)
 */
public final class MessageDecoder {
    /*
     <PRI>SEQ NO:HostName:Timestamp:Application[ProcessID]:%Facility-Severity-MNEMONIC:description
     regex for syslog:
           (<[0-9]+>)([0-9]+):[\s]*([^:]*):[\s]*(.*)[\s]*:[\s]*(.+)\[(.+)\][\s]*:[\s]*%(.*)-([0-7])-(.*)[\s]*:[\s]*(.*)
     Group 1: Pri (Facility * 8 + Severity)
     Group 2: SequenceID
     Group 3: Hostname
     Group 4: Timestamp
     Group 5: Application
     Group 6: ProcessID
     Group 7: MNEMONIC
     Group 8: Description
     */
    private static final String REGEX = "<([0-9]+)>([0-9]+):([^:]*):(.+):(.+)\\[([0-9]*)]:[\\s]*%.+-[0-7]-(.*):(.*)";
    private static final Pattern PATTERN = Pattern.compile(REGEX);

    private MessageDecoder() {
    }

    public static Message decode(String msg) {
        Matcher matcher = PATTERN.matcher(msg);
        matcher.find();
        MessageBuilder builder = new MessageBuilder();
        int pri = Integer.parseInt(matcher.group(1));
        int facility = pri / 8;
        int severity = pri % 8;
        builder.facility(Facility.values()[facility])
                .severity(Severity.values()[severity])
                .sequenceId(matcher.group(2).trim())
                .hostname(matcher.group(3).trim())
                .timestamp(matcher.group(4).trim())
                .applicationName(matcher.group(5).trim())
                .processId(matcher.group(6).trim())
                .content(matcher.group(7).trim() + " : " + matcher.group(8).trim());
        return builder.build();
    }

    public static boolean matches(String msg) {
        Matcher matcher = PATTERN.matcher(msg);
        return matcher.find();
    }
}
