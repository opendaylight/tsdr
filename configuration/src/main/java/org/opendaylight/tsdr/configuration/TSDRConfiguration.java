/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.configuration;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.configuration.rev151130.GetAllPropertiesOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.configuration.rev151130.GetAllPropertiesOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.configuration.rev151130.GetPropertyInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.configuration.rev151130.GetPropertyOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.configuration.rev151130.GetPropertyOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.configuration.rev151130.TsdrConfigurationService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.configuration.rev151130.getallproperties.output.Properties;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.configuration.rev151130.getallproperties.output.PropertiesBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Created by saichler on 11/30/15
 * This file serves as the global configuration source for TSDR project
 * The configutration is stored in tsdr.cfg under the ./etc directory
 * and it is loaded when the feature is loaded and the "update" method is invoked.
 * The update method is also invoked whenever there is a change to the file to
 * reload the configuration @ runtime.
 */
public class TSDRConfiguration implements ManagedService,TsdrConfigurationService {

    private Dictionary<String, Object> configurations = new Hashtable<>();

    public static final String P_HOST = "host";

    public TSDRConfiguration() {
        this.configurations.put(P_HOST, "127.0.0.1");
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

    private String getProperty(String name){
        return (String)this.configurations.get(name);
    }

    @Override
    public Future<RpcResult<GetPropertyOutput>> getProperty(GetPropertyInput input) {
        GetPropertyOutputBuilder b = new GetPropertyOutputBuilder();
        String value = (String)configurations.get(input.getName());
        b.setValue(value);
        return RpcResultBuilder.success(b).buildFuture();
    }

    @Override
    public Future<RpcResult<GetAllPropertiesOutput>> getAllProperties() {
        GetAllPropertiesOutputBuilder output = new GetAllPropertiesOutputBuilder();
        List<Properties> result = new ArrayList<>(configurations.size());
        Enumeration<String> keys = configurations.keys();
        while(keys.hasMoreElements()){
            String key = keys.nextElement();
            PropertiesBuilder b = new PropertiesBuilder();
            b.setKey(key);
            b.setValue((String)configurations.get(key));
            result.add(b.build());
        }
        output.setProperties(result);
        return RpcResultBuilder.success(output).buildFuture();
    }
}
