/*
 * Copyright (c) 2016 Saugo360 and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.restconf.collector;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * This class is responsible for testing the TSDRRestconfCollectorModule class.
 *
 * @author <a href="mailto:a.alhamali93@gmail.com">AbdulRahman AlHamali</a>
 *
 *         Created: Dec 16th, 2016
 *
 */
public class TSDRRestconfCollectorConfiguratorTest {
    @Mock
    private Configuration filterChainConfiguration;

    @Mock
    private ConfigurationAdmin configurationAdmin;

    private final Dictionary<String, String> configProperties = new Hashtable<>();

    @SuppressWarnings("unchecked")
    @Before
    public void setup() throws IOException {
        MockitoAnnotations.initMocks(this);

        Mockito.when(filterChainConfiguration.getProperties()).thenReturn(configProperties);

        Mockito.when(configurationAdmin.getConfiguration("org.opendaylight.aaa.filterchain"))
            .thenReturn(filterChainConfiguration);
    }

    /**
     * tests filter registration when other filters are already registered but our filter isn't.
     * the list should remain the same with our filter appended to it.
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testRegisterFilterWhenOtherFiltersExistButTSDRFilterDoesNot() throws IOException {
        try (TSDRRestconfCollectorConfigurator configurator =
                new TSDRRestconfCollectorConfigurator(configurationAdmin)) {
            configProperties.put("customFilterList", "randomFilter");

            configurator.init();

            ArgumentCaptor<Dictionary> argumentCaptor = ArgumentCaptor.forClass(Dictionary.class);
            Mockito.verify(filterChainConfiguration).update(argumentCaptor.capture());

            Dictionary argument = argumentCaptor.getValue();
            assertEquals("randomFilter," + TSDRRestconfCollectorFilter.class.getName(),
                    argument.get("customFilterList"));
        }
    }

    /**
     * tests filter registration when our filter already exists.
     * the list should remain the same
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testRegisterFilterWhenTSDRFilterExists() throws IOException {
        try (TSDRRestconfCollectorConfigurator configurator =
                new TSDRRestconfCollectorConfigurator(configurationAdmin)) {
            configProperties.put("customFilterList", "randomFilter1," + TSDRRestconfCollectorFilter.class.getName()
                    + ",randomFilter2");

            configurator.init();

            ArgumentCaptor<Dictionary> argumentCaptor = ArgumentCaptor.forClass(Dictionary.class);
            Mockito.verify(filterChainConfiguration).update(argumentCaptor.capture());

            Dictionary argument = argumentCaptor.getValue();
            assertEquals("randomFilter1," + TSDRRestconfCollectorFilter.class.getName() + ",randomFilter2",
                    argument.get("customFilterList"));
        }
    }

    /**
     * tests filter registration when the list is empty.
     * the list should contain only our filter
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testRegisterFilterWhenNoFiltersExist() throws IOException {
        try (TSDRRestconfCollectorConfigurator configurator =
                new TSDRRestconfCollectorConfigurator(configurationAdmin)) {
            configProperties.put("customFilterList", "");

            configurator.init();

            ArgumentCaptor<Dictionary> argumentCaptor = ArgumentCaptor.forClass(Dictionary.class);
            Mockito.verify(filterChainConfiguration).update(argumentCaptor.capture());

            Dictionary argument = argumentCaptor.getValue();
            assertEquals(TSDRRestconfCollectorFilter.class.getName(), argument.get("customFilterList"));
        }
    }

    /**
     * tests filter registration when the customFilterList property doesn't exist (perhaps it was removed by mistake).
     * the property should be created and our filter should be added to it
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testRegisterFilterWhenCustomFilterListDoesntExist() throws IOException {
        try (TSDRRestconfCollectorConfigurator configurator =
                new TSDRRestconfCollectorConfigurator(configurationAdmin)) {
            configurator.init();

            ArgumentCaptor<Dictionary> argumentCaptor = ArgumentCaptor.forClass(Dictionary.class);
            Mockito.verify(filterChainConfiguration).update(argumentCaptor.capture());

            Dictionary argument = argumentCaptor.getValue();
            assertEquals(TSDRRestconfCollectorFilter.class.getName(), argument.get("customFilterList"));
        }
    }

    /**
     * tests filter unregistration when our filter exists along with other filters.
     * the list should remain the same with our filter removed from it
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testUnregisterFilterWhenFilterExistsWithOtherFilters() throws Exception {
        try (TSDRRestconfCollectorConfigurator configurator =
                new TSDRRestconfCollectorConfigurator(configurationAdmin)) {
            configProperties.put("customFilterList", "randomFilter1," + TSDRRestconfCollectorFilter.class.getName()
                    + ",randomFilter2");

            configurator.init();

            ArgumentCaptor<Dictionary> argumentCaptor = ArgumentCaptor.forClass(Dictionary.class);
            Mockito.verify(filterChainConfiguration).update(argumentCaptor.capture());

            Dictionary argument = argumentCaptor.getValue();
            assertEquals("randomFilter1," + TSDRRestconfCollectorFilter.class.getName() + ",randomFilter2",
                    argument.get("customFilterList"));

            // To remove the old invocation
            Mockito.reset(filterChainConfiguration);
            Mockito.when(filterChainConfiguration.getProperties()).thenReturn(configProperties);
        }

        ArgumentCaptor<Dictionary> argumentCaptor = ArgumentCaptor.forClass(Dictionary.class);
        Mockito.verify(filterChainConfiguration).update(argumentCaptor.capture());

        Dictionary argument = argumentCaptor.getValue();
        assertEquals("randomFilter1,randomFilter2", argument.get("customFilterList"));
    }

    /**
     * tests filter unregistration when only our filter exists.
     * the list should become empty
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testUnregisterFilterWhenFilterExistsAlone() throws Exception {
        try (TSDRRestconfCollectorConfigurator configurator =
                new TSDRRestconfCollectorConfigurator(configurationAdmin)) {
            configProperties.put("customFilterList", TSDRRestconfCollectorFilter.class.getName());

            configurator.init();

            ArgumentCaptor<Dictionary> argumentCaptor = ArgumentCaptor.forClass(Dictionary.class);
            Mockito.verify(filterChainConfiguration).update(argumentCaptor.capture());

            Dictionary argument = argumentCaptor.getValue();
            assertEquals(TSDRRestconfCollectorFilter.class.getName(), argument.get("customFilterList"));

            // To remove the old invocation
            Mockito.reset(filterChainConfiguration);
            Mockito.when(filterChainConfiguration.getProperties()).thenReturn(configProperties);
        }

        ArgumentCaptor<Dictionary> argumentCaptor = ArgumentCaptor.forClass(Dictionary.class);
        Mockito.verify(filterChainConfiguration).update(argumentCaptor.capture());

        Dictionary argument = argumentCaptor.getValue();
        assertEquals("", argument.get("customFilterList"));
    }

    /**
     * tests filter unregistration when the customFilterList property doesn't even exist (perhaps it was removed).
     * no changes should be done
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testUnregisterFilterWhenCustomFilterListDoesntExist() throws Exception {
        try (TSDRRestconfCollectorConfigurator configurator =
                new TSDRRestconfCollectorConfigurator(configurationAdmin)) {
            configurator.init();

            ArgumentCaptor<Dictionary> argumentCaptor = ArgumentCaptor.forClass(Dictionary.class);
            Mockito.verify(filterChainConfiguration).update(argumentCaptor.capture());

            Dictionary argument = argumentCaptor.getValue();
            assertEquals(TSDRRestconfCollectorFilter.class.getName(), argument.get("customFilterList"));

            // To remove the old invocation
            Mockito.reset(filterChainConfiguration);
            Mockito.when(filterChainConfiguration.getProperties()).thenReturn(configProperties);
            configProperties.remove("customFilterList");
        }

        Mockito.verify(filterChainConfiguration, Mockito.never()).update(Mockito.any());
    }

    /**
     * tests filter unregistration when our filter doesn't exist.
     * the list should remain the same, no changes should be done
     */
    @SuppressWarnings("rawtypes")
    @Test
    public void testUnregisterFilterWhenFilterDoesntExist() throws Exception {
        try (TSDRRestconfCollectorConfigurator configurator =
                new TSDRRestconfCollectorConfigurator(configurationAdmin)) {
            configurator.init();

            ArgumentCaptor<Dictionary> argumentCaptor = ArgumentCaptor.forClass(Dictionary.class);
            Mockito.verify(filterChainConfiguration).update(argumentCaptor.capture());

            Dictionary argument = argumentCaptor.getValue();
            assertEquals(TSDRRestconfCollectorFilter.class.getName(), argument.get("customFilterList"));

            // To remove the old invocation
            Mockito.reset(filterChainConfiguration);
            Mockito.when(filterChainConfiguration.getProperties()).thenReturn(configProperties);
            configProperties.put("customFilterList", "randomFilter1,randomFilter2");
        }

        Mockito.verify(filterChainConfiguration, Mockito.never()).update(Mockito.any());
    }
}
