/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2015 Tata Consultancy Services, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.sdc;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maintains SNMP configuration.
 *
 * @author Trapti Khandelwal(trapti.khandelwal@tcs.com)
 * @author Razi Ahmed(ahmed.razi@tcs.com)
 */
public class SNMPConfig implements ManagedService {
    private static final Logger LOG = LoggerFactory.getLogger(SNMPConfig.class);
    public static final String P_CREDENTIALS = "credentials";

    /*
     * There is multiple values against single key i.e host and
     * community_string. So array of String is to hold multiple values against
     * single key. eg credentials=[127.0.0.1,public],[127.0.0.1,public]
     */
    private final Dictionary<String, String[]> configurations = new Hashtable<>();

    public SNMPConfig() {
        configurations.put(P_CREDENTIALS, new String[]{"127.0.0.1","public"});
    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        if (properties != null && !properties.isEmpty()) {
            StringBuilder builder = new StringBuilder("{");
            Enumeration<String> keys = properties.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                String[] list = ((String) properties.get(key)).replace("[", "").replace("]", "").split(",");
                builder.append(key).append('=').append(Arrays.toString(list)).append(',');
                configurations.put(key, list);
            }

            builder.append('}');
            LOG.info("TSDRSNMPConfig updated to {}", builder);
        }
    }

    public Dictionary<String, String[]> getConfiguration() {
        return this.configurations;
    }

    public Object getConfig(String name) {
        return this.configurations.get(name);
    }
}
