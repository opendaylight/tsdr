/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.configuration;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Created by saichler on 11/30/15
 * This file serves as the global configuration source for TSDR project
 * The configutration is stored in tsdr.cfg under the ./etc directory
 * and it is loaded when the feature is loaded and the "update" method is invoked.
 * The update method is also invoked whenever there is a change to the file to
 * reload the configuration @ runtime.
 */
public class TSDRConfiguration implements ManagedService {

    private static TSDRConfiguration instance = new TSDRConfiguration();
    private Dictionary<String, Object> configurations = new Hashtable<>();

    public static final String P_HOST = "host";

    private TSDRConfiguration() {
        this.configurations.put(P_HOST, "127.0.0.1");
    }

    public static TSDRConfiguration getInstance() {
        return instance;
    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        if (properties != null && !properties.isEmpty()) {
            Enumeration<String> k = properties.keys();
            while (k.hasMoreElements()) {
                String key = k.nextElement();
                String value = (String) properties.get(key);
                configurations.put(key, value);
            }
        }
    }

    public Dictionary<String, Object> getConfiguration() {
        return this.configurations;
    }

    public Object getConfig(String name){
        return this.configurations.get(name);
    }
}
