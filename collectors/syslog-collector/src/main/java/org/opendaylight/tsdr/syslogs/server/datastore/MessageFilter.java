/*
 * Copyright (c) 2016 TethrNet Technology Co.Ltd and others.  All rights reserved.
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.syslogs.server.datastore;

import org.opendaylight.tsdr.syslogs.server.decoder.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.syslog.filter.FilterEntity;

import java.util.regex.Pattern;

/**
 * This Class is for filter building and
 * define the judgement of whether a message
 * matches a filter.
 *
 * @author Kun Chen(kunch@tethrnet.com)
 */
public class MessageFilter {
    //When the filed is null, it means a wildcard
    //String type should support regex match
    private final Message.Facility facility;
    private final Message.Severity severity;
    private final String hostname;
    private final String applicationName;
    private final String processId;
    private final String sequenceId;
    private final String content;

    private MessageFilter(Message.Facility facility, Message.Severity severity, String hostname, String applicationName, String processId, String messageId, String content) {
        this.facility = facility;
        this.severity = severity;
        this.hostname = hostname;
        this.applicationName = applicationName;
        this.processId = processId;
        this.sequenceId = messageId;
        this.content = content;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Message) {
            Message msg = (Message) obj;
            if (facility != null && !facility.equals(msg.getFacility()))
                return false;
            if (severity != null && !severity.equals(msg.getSeverity()))
                return false;
            if (!Pattern.matches(hostname, msg.getHostname()) &&
                    !Pattern.matches(applicationName, msg.getApplicationName()) &&
                    !Pattern.matches(processId, msg.getProcessId()) &&
                    !Pattern.matches(sequenceId, msg.getSequenceId()))
                return false;
            if (!Pattern.matches(content, msg.getContent())) {
                return false;
            }
            //All fields are matched!
            return true;
        } else {
            return false;
        }
    }

    /**
     * This the builder of filter.
     */
    public static class FilterBuilder {
        private Message.Facility facility = null;
        private Message.Severity severity = null;
        private String hostname = ".*";
        private String applicationName = ".*";
        private String processId = ".*";
        private String sequenceId = ".*";
        private String content = ".*";

        public static MessageFilter create(FilterEntity filterEntity) {
            FilterBuilder builder = new FilterBuilder();
            if (filterEntity.getSeverity() != null) {
                builder.severity = Message.Severity.values()[filterEntity.getSeverity().getValue()];
            }
            if (filterEntity.getFacility() != null) {
                builder.facility = Message.Facility.values()[filterEntity.getFacility().getValue()];
            }
            builder.hostname = filterEntity.getHost();
            builder.applicationName = filterEntity.getApplication();
            builder.processId = filterEntity.getPid();
            builder.sequenceId = filterEntity.getSid();
            builder.content = ".*" + filterEntity.getContent() + ".*\n?";
            return builder.build();
        }

        private MessageFilter build() {
            return new MessageFilter(facility, severity, hostname, applicationName, processId, sequenceId, content);
        }
    }
}
