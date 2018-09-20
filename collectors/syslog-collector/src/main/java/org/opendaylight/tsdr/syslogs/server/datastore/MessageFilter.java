/*
 * Copyright (c) 2016 TethrNet Technology Co.Ltd and others.  All rights reserved.
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.syslogs.server.datastore;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.opendaylight.tsdr.syslogs.server.decoder.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.Facility;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.Severity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.syslog.filter.Filter;

/**
 * This Class is for filter building and
 * define the judgement of whether a message
 * matches a filter.
 *
 * @author Kun Chen(kunch@tethrnet.com)
 */
public final class MessageFilter {
    //When the field is null, it means a wildcard
    //String type should support regex match
    private final Facility facility;
    private final Severity severity;
    private final Pattern hostname;
    private final Pattern applicationName;
    private final Pattern processId;
    private final Pattern sequenceId;
    private final Pattern content;

    private MessageFilter(Facility facility, Severity severity, String hostname,
            String applicationName, String processId, String messageId, String content) throws PatternSyntaxException {
        this.facility = facility;
        this.severity = severity;
        this.hostname = Pattern.compile(requireNonNull(hostname));
        this.applicationName = Pattern.compile(requireNonNull(applicationName));
        this.processId = Pattern.compile(requireNonNull(processId));
        this.sequenceId = Pattern.compile(requireNonNull(messageId));
        this.content = Pattern.compile(requireNonNull(content));
    }

    public static MessageFilter from(Filter filter) throws PatternSyntaxException {
        return new MessageFilter(filter.getFacility(), filter.getSeverity(), filter.getHost(), filter.getApplication(),
                filter.getPid(), filter.getSid(), ".*" + filter.getContent() + ".*\n?");
    }

    @Override
    public int hashCode() {
        return Objects.hash(content, facility, hostname, severity);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        MessageFilter other = (MessageFilter) obj;
        return Objects.equals(content, other.content) && Objects.equals(hostname, other.hostname)
                && facility == other.facility && severity == other.severity;
    }

    public boolean matches(Message msg) {
        if (facility != null && !facility.equals(msg.getFacility())) {
            return false;
        }

        if (severity != null && !severity.equals(msg.getSeverity())) {
            return false;
        }

        if (!hostname.matcher(msg.getHostname()).matches()
                && !applicationName.matcher(msg.getApplicationName()).matches()
                && !processId.matcher(msg.getProcessId()).matches()
                && !sequenceId.matcher(msg.getSequenceId()).matches()) {
            return false;
        }

        if (!content.matcher(msg.getContent()).matches()) {
            return false;
        }

        //All fields are matched!
        return true;
    }
}
