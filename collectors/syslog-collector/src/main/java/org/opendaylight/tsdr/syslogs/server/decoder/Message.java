/*
 * Copyright (c) 2016 TethrNet Technology Co.Ltd and others.  All rights reserved.
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.syslogs.server.decoder;

/**
 * Format of Syslog
 * &lt;PRI&gt;SEQ NO:HostName:Timestamp:Application[ProcessID]:%Facility-Severity-MNEMONIC:description
 * Represents a Syslog content as defined by RFC 5424.
 * See http://tools.ietf.org/html/rfc5424#section-6.
 *
 * @author Wenbo Hu(wenbhu@tethrnet.com)
 * @author Kun Chen(kunch@tethrnet.com)
 */
public final class Message {
    // <PRI>SEQ NO:HostName:Timestamp:Application[ProcessID]:%Facility-Severity-MNEMONIC:description
    public enum Facility {
        KERNEL,
        USER_LEVEL,
        MAIL,
        SYSTEM_DAEMON,
        SECURITY,
        SYSLOGD,
        LINE_PRINTER,
        NETWORK_NEWS,
        UUCP,
        CLOCK,
        SECURITY2,
        FTP,
        NTP,
        LOG_AUDIT,
        LOG_ALERTY,
        CLOCK2,
        LOCAL0,
        LOCAL1,
        LOCAL2,
        LOCAL3,
        LOCAL4,
        LOCAL5,
        LOCAL6,
        LOCAL7
    }

    public enum Severity {
        EMERGENCY,
        ALERT,
        CRITICAL,
        ERROR,
        WARNING,
        NOTICE,
        INFORMATION,
        DEBUG
    }

    private final Facility facility;
    private final Severity severity;
    private final String sequenceId;
    private final String timestamp;
    private final String hostname;
    private final String applicationName;
    private final String processId;
    private final String content;

    private Message(Facility facility, Severity severity, String sequenceId, String timestamp, String hostname,
            String applicationName, String processId, String content) {
        this.facility = facility;
        this.severity = severity;
        this.sequenceId = sequenceId;
        this.timestamp = timestamp;
        this.hostname = hostname;
        this.applicationName = applicationName;
        this.processId = processId;
        this.content = content;
    }


    public Facility getFacility() {
        return facility;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getSequenceId() {
        return sequenceId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getHostname() {
        return hostname;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getProcessId() {
        return processId;
    }

    public String getContent() {
        return content;
    }


    @Override
    public String toString() {
        return "Message [facility=" + facility + ", severity=" + severity + ", sequenceId=" + sequenceId
                + ", timestamp=" + timestamp + ", hostname=" + hostname + ", applicationName=" + applicationName
                + ", processId=" + processId + ", content=" + content + "]";
    }

    public static class MessageBuilder {
        private Facility facility;
        private Severity severity;
        private String sequenceId;
        private String timestamp;
        private String hostname;
        private String applicationName;
        private String processId;
        private String content;

        public static MessageBuilder create() {
            return new MessageBuilder();
        }

        public MessageBuilder facility(Facility newFacility) {
            this.facility = newFacility;
            return this;
        }

        public MessageBuilder severity(Severity newSeverity) {
            this.severity = newSeverity;
            return this;
        }

        public MessageBuilder timestamp(String newTimestamp) {
            this.timestamp = newTimestamp;
            return this;
        }

        public MessageBuilder hostname(String newHostname) {
            this.hostname = newHostname;
            return this;
        }

        public MessageBuilder applicationName(String newApplicationName) {
            this.applicationName = newApplicationName;
            return this;
        }

        public MessageBuilder processId(String newProcessId) {
            this.processId = newProcessId;
            return this;
        }

        public MessageBuilder sequenceId(String newSequenceId) {
            this.sequenceId = newSequenceId;
            return this;
        }

        public MessageBuilder content(String newContent) {
            this.content = newContent;
            return this;
        }

        public Message build() {
            return new Message(facility, severity, sequenceId, timestamp, hostname, applicationName, processId,
                    content);
        }
    }
}
