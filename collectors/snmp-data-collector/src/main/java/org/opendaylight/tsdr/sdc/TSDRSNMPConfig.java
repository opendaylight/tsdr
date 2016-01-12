/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 * Copyright (c) 2015 Tata Consultancy Services, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.sdc;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Created by saichler@gmail.com on 12/2/15.
 * @author Trapti Khandelwal(trapti.khandelwal@tcs.com)
 * @author Razi Ahmed(ahmed.razi@tcs.com)
 */
public class TSDRSNMPConfig implements ManagedService {

    private static TSDRSNMPConfig instance = new TSDRSNMPConfig();
    /*There is multiple values against single key i.e  host and community_string.
     * So array of String is to hold multiple values against single key.
     * eg credentials=[127.0.0.1,public],[127.0.0.1,public]
    */
    private Dictionary<String, String[]> configurations = new Hashtable<>();
    public static final String P_CREDENTIALS = "credentials";
    private static final Logger log = LoggerFactory
            .getLogger(TSDRSNMPConfig.class);

    private TSDRSNMPConfig() {
        configurations.put(P_CREDENTIALS, new String[]{"127.0.0.1","public"});
    }

    public static TSDRSNMPConfig getInstance() {
        return instance;
    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        if (properties != null && !properties.isEmpty()) {
            Enumeration<String> k = properties.keys();
            while (k.hasMoreElements()) {
                String key = k.nextElement();
                String[] list = ((String) properties.get(key)).replace("[", "").replace("]", "").split(",");
                configurations.put(key, list);
            }
        }
    }

    public Dictionary<String, String[]> getConfiguration() {
        return this.configurations;
    }

    public Object getConfig(String name){
        return this.configurations.get(name);
    }
}
