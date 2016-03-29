/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.spi.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author <a href="mailto:saichler@cisco.com">Sharon Aicler</a>
 *         <p>
 *         Created: March 29, 2016
 */
public class ConfigFileUtil {

    public static final String CASSANDRA_STORE_CONFIG_FILE = "./etc/tsdr-persistence-cassandra.properties";
    public static final String HBASE_STORE_CONFIG_FILE = "./etc/tsdr-persistence-hbase.properties";
    public static final String HSQLDB_STORE_CONFIG_FILE = "./etc/tsdr-persistence-hsqldb.properties";

    public static final String METRIC_PERSISTENCE_PROPERTY = "metric-persistency";
    public static final String LOG_PERSISTENCE_PROPERTY = "log-persistency";
    public static final String BINARY_PERSISTENCE_PROPERTY = "binary-persistency";

    public static final Map<String,String> loadConfig(String confFile) throws IOException {
        HashMap<String, String> result = new HashMap<String,String>();
        BufferedReader in = null;
        in = new BufferedReader(new InputStreamReader(new FileInputStream(confFile)));
        String line = in.readLine();
        while(line!=null ){
            if(!line.trim().equals("")) {
                int index = line.indexOf("=");
                if(index!=-1) {
                    String key = line.substring(0, index).trim();
                    String value = line.substring(index + 1).trim();
                    result.put(key, value);
                }
            }
            line = in.readLine();
        }
        in.close();
        return result;
    }

    public static final boolean isMetricPersistenceEnabled(Map<String,String> props){
        String enabled = props.get(METRIC_PERSISTENCE_PROPERTY);
        if(enabled!=null){
            return Boolean.parseBoolean(enabled);
        }
        return false;
    }

    public static final boolean isLogPersistenceEnabled(Map<String,String> props){
        String enabled = props.get(LOG_PERSISTENCE_PROPERTY);
        if(enabled!=null){
            return Boolean.parseBoolean(enabled);
        }
        return false;
    }

    public static final boolean isBinaryPersistenceEnabled(Map<String,String> props){
        String enabled = props.get(BINARY_PERSISTENCE_PROPERTY);
        if(enabled!=null){
            return Boolean.parseBoolean(enabled);
        }
        return false;
    }
}
