/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
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
import java.util.Properties;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Created by saichler@gmail.com on 12/2/15.
 */
public class TSDRSNMPConfig implements ManagedService {

    private static TSDRSNMPConfig instance = new TSDRSNMPConfig();
    private Dictionary<String, Object> configurations = new Hashtable<>();
    private String tsdrConfigFile = "tsdr-snmp.cfg";
    public static final String P_HOST = "host";
    public static final String P_COMMUNITY = "community_string";
    private static final Logger log = LoggerFactory
            .getLogger(TSDRSNMPConfig.class);

    private TSDRSNMPConfig() {
        loadConfigurationInfo();
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

    private void loadConfigurationInfo()
    {
        InputStream inputStream = null;
        Properties properties = new Properties();
        try {
           String  fileFullPath = System.getProperty("karaf.etc") + "/" + tsdrConfigFile;
           File f = new File(fileFullPath);
           if(f.exists()){
               inputStream = new FileInputStream(f);
               properties.load(inputStream);
               for (final String name: properties.stringPropertyNames())
               {
                    this.configurations.put(name, properties.getProperty(name));
               }
              } else {
                  log.error("Property file " + fileFullPath + " missing");
           }
       } catch(Exception e){
           log.error("Exception while loading the datapurge.properties stream",e);
       }
    }
}
