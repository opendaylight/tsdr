/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.datapurge;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by saichler@gmail.com on 12/2/15.
 */
public class TSDRDataPurgeConfig implements ManagedService {

    private static TSDRDataPurgeConfig instance = new TSDRDataPurgeConfig();
    private Dictionary<String, Object> configurations = new Hashtable<>();
    private static final Logger log = LoggerFactory.getLogger(TSDRDataPurgeConfig.class);

    private TSDRDataPurgeConfig() {
    }

    public static TSDRDataPurgeConfig getInstance() {
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
            PurgingScheduler.getInstance().loadProperties();
            PurgingScheduler.getInstance().schedule();
        }
    }

    public Dictionary<String, Object> getConfiguration() {
        return this.configurations;
    }

    public String getProperty(String name){
        return (String)this.configurations.get(name);
    }

}
