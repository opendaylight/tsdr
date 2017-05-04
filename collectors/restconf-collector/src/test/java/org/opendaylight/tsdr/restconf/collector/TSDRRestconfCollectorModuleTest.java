/*
 * Copyright (c) 2016 Saugo360 and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.restconf.collector;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Set;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.DependencyResolverFactory;
import org.opendaylight.controller.config.api.DynamicMBeanWithInstance;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.config.api.annotations.AbstractServiceInterface;
import org.opendaylight.controller.config.spi.Module;
import org.opendaylight.controller.config.yang.config.tsdr.restconf.collector.AbstractTSDRRestconfCollectorModuleFactory;
import org.opendaylight.controller.config.yang.config.tsdr.restconf.collector.TSDRRestconfCollectorModule;
import org.opendaylight.controller.config.yang.config.tsdr.restconf.collector.TSDRRestconfCollectorModuleFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
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
public class TSDRRestconfCollectorModuleTest {

    /**
     * tests filter registration when other filters are already registered but our filter isn't.
     * the list should remain the same with our filter appended to it.
     */
    @Test
    public void testRegisterFilterWhenOtherFiltersExistButTSDRFilterDoesNot() throws IOException {
        TSDRRestconfCollectorModule module = new TSDRRestconfCollectorModule(null,null);
        setupModuleWithMocks(module,"X");
        Dictionary<String, String> properties = new Hashtable<>();
        properties.put("customFilterList", "randomFilter");
        Configuration filterChainConfiguration = prepareModuleForFilterRegistration(module, properties);
        module.createInstance();

        ArgumentCaptor<Dictionary> argumentCaptor = ArgumentCaptor.forClass(Dictionary.class);
        Mockito.verify(filterChainConfiguration).update(argumentCaptor.capture());

        Dictionary argument = argumentCaptor.getValue();
        Assert.assertEquals("randomFilter," + TSDRRestconfCollectorFilter.class.getName(),
            argument.get("customFilterList"));
    }

    /**
     * tests filter registration when our filter already exists.
     * the list should remain the same
     */
    @Test
    public void testRegisterFilterWhenTSDRFilterExists() throws IOException {
        TSDRRestconfCollectorModule module = new TSDRRestconfCollectorModule(null,null);
        setupModuleWithMocks(module,"X");
        Dictionary<String, String> properties = new Hashtable<>();
        properties.put("customFilterList", "randomFilter1," + TSDRRestconfCollectorFilter.class.getName()
            + ",randomFilter2");
        Configuration filterChainConfiguration = prepareModuleForFilterRegistration(module, properties);
        module.createInstance();

        ArgumentCaptor<Dictionary> argumentCaptor = ArgumentCaptor.forClass(Dictionary.class);
        Mockito.verify(filterChainConfiguration).update(argumentCaptor.capture());

        Dictionary argument = argumentCaptor.getValue();
        Assert.assertEquals("randomFilter1," + TSDRRestconfCollectorFilter.class.getName() + ",randomFilter2",
            argument.get("customFilterList"));
    }

    /**
     * tests filter registration when the list is empty.
     * the list should contain only our filter
     */
    @Test
    public void testRegisterFilterWhenNoFiltersExist() throws IOException {
        TSDRRestconfCollectorModule module = new TSDRRestconfCollectorModule(null,null);
        setupModuleWithMocks(module,"X");
        Dictionary<String, String> properties = new Hashtable<>();
        properties.put("customFilterList", "");
        Configuration filterChainConfiguration = prepareModuleForFilterRegistration(module, properties);
        module.createInstance();

        ArgumentCaptor<Dictionary> argumentCaptor = ArgumentCaptor.forClass(Dictionary.class);
        Mockito.verify(filterChainConfiguration).update(argumentCaptor.capture());

        Dictionary argument = argumentCaptor.getValue();
        Assert.assertEquals(TSDRRestconfCollectorFilter.class.getName(), argument.get("customFilterList"));
    }

    /**
     * tests filter registration when the customFilterList property doesn't exist (perhaps it was removed by mistake).
     * the property should be created and our filter should be added to it
     */
    @Test
    public void testRegisterFilterWhenCustomFilterListDoesntExist() throws IOException {
        TSDRRestconfCollectorModule module = new TSDRRestconfCollectorModule(null,null);
        setupModuleWithMocks(module,"X");
        Dictionary<String, String> properties = new Hashtable<>();

        Configuration filterChainConfiguration = prepareModuleForFilterRegistration(module, properties);
        module.createInstance();

        ArgumentCaptor<Dictionary> argumentCaptor = ArgumentCaptor.forClass(Dictionary.class);
        Mockito.verify(filterChainConfiguration).update(argumentCaptor.capture());

        Dictionary argument = argumentCaptor.getValue();
        Assert.assertEquals(TSDRRestconfCollectorFilter.class.getName(), argument.get("customFilterList"));
    }

    /**
     * tests filter registration when the configuration admin is down.
     * no attempts to modify the list should be done
     */
    @Test
    public void testRegisterFilterWhenConfigurationAdminDoesNotExist() throws IOException {
        TSDRRestconfCollectorModule module = new TSDRRestconfCollectorModule(null,null);
        setupModuleWithMocks(module, "X");
        Dictionary<String, String> properties = new Hashtable<>();
        Configuration filterChainConfiguration = prepareModuleForFilterRegistration(module, properties, false);
        module.createInstance();

        Mockito.verify(filterChainConfiguration, Mockito.never()).update(Mockito.any());
    }

    /**
     * tests filter unregistration when our filter exists along with other filters.
     * the list should remain the same with our filter removed from it
     */
    @Test
    public void testUnregisterFilterWhenFilterExistsWithOtherFilters() throws Exception {
        TSDRRestconfCollectorModule module = new TSDRRestconfCollectorModule(null,null);
        setupModuleWithMocks(module,"X");
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("customFilterList", "randomFilter1," + TSDRRestconfCollectorFilter.class.getName()
            + ",randomFilter2");

        Configuration filterChainConfiguration = prepareModuleForFilterRegistration(module, properties);
        module.createInstance();

        // To remove the old invocation
        Mockito.reset(filterChainConfiguration);
        Mockito.when(filterChainConfiguration.getProperties()).thenReturn(properties);

        module.close();

        ArgumentCaptor<Dictionary> argumentCaptor = ArgumentCaptor.forClass(Dictionary.class);
        Mockito.verify(filterChainConfiguration).update(argumentCaptor.capture());

        Dictionary argument = argumentCaptor.getValue();
        Assert.assertEquals("randomFilter1,randomFilter2", argument.get("customFilterList"));
    }

    /**
     * tests filter unregistration when only our filter exists.
     * the list should become empty
     */
    @Test
    public void testUnregisterFilterWhenFilterExistsAlone() throws Exception {
        TSDRRestconfCollectorModule module = new TSDRRestconfCollectorModule(null,null);
        setupModuleWithMocks(module,"X");
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("customFilterList", TSDRRestconfCollectorFilter.class.getName());

        Configuration filterChainConfiguration = prepareModuleForFilterRegistration(module, properties);
        module.createInstance();

        // To remove the old invocation
        Mockito.reset(filterChainConfiguration);
        Mockito.when(filterChainConfiguration.getProperties()).thenReturn(properties);

        module.close();

        ArgumentCaptor<Dictionary> argumentCaptor = ArgumentCaptor.forClass(Dictionary.class);
        Mockito.verify(filterChainConfiguration).update(argumentCaptor.capture());

        Dictionary argument = argumentCaptor.getValue();
        Assert.assertEquals("", argument.get("customFilterList"));
    }

    /**
     * tests filter unregistration when the customFilterList property doesn't even exist (perhaps it was removed).
     * no changes should be done
     */
    @Test
    public void testUnregisterFilterWhenCustomFilterListDoesntExist() throws Exception {
        TSDRRestconfCollectorModule module = new TSDRRestconfCollectorModule(null,null);
        setupModuleWithMocks(module,"X");
        Dictionary<String, Object> properties = new Hashtable<>();

        Configuration filterChainConfiguration = prepareModuleForFilterRegistration(module, properties);
        module.createInstance();

        // To remove the old invocation
        Mockito.reset(filterChainConfiguration);

        properties.remove("customFilterList");
        Mockito.when(filterChainConfiguration.getProperties()).thenReturn(properties);

        module.close();

        Mockito.verify(filterChainConfiguration, Mockito.never()).update(Mockito.any());
    }

    /**
     * tests filter unregistration when our filter doesn't exist.
     * the list should remain the same, no changes should be done
     */
    @Test
    public void testUnregisterFilterWhenFilterDoesntExist() throws Exception {
        TSDRRestconfCollectorModule module = new TSDRRestconfCollectorModule(null,null);
        setupModuleWithMocks(module,"X");
        Dictionary<String, Object> properties = new Hashtable<>();

        Configuration filterChainConfiguration = prepareModuleForFilterRegistration(module, properties);
        module.createInstance();

        // To remove the old invocation
        Mockito.reset(filterChainConfiguration);

        properties.put("customFilterList", "randomFilter1,randomFilter2");
        Mockito.when(filterChainConfiguration.getProperties()).thenReturn(properties);

        module.close();

        Mockito.verify(filterChainConfiguration, Mockito.never()).update(Mockito.any());
    }

    /**
     * Used to increase code coverage because of a bug in odlparent.
     */
    @Test
    public void codeCoverageModule() throws Exception {
        TSDRRestconfCollectorModuleFactory mf = new TSDRRestconfCollectorModuleFactory();
        mf.createModule("X", null, null);

        DynamicMBeanWithInstance moduleInstance = Mockito.mock(DynamicMBeanWithInstance.class);
        Mockito.when(moduleInstance.getModule()).thenReturn(new TSDRRestconfCollectorModule(null, null));
        mf.createModule("X", null, moduleInstance, null);

        new TSDRRestconfCollectorModule(null,null,null,null);
        TSDRRestconfCollectorModule module1 = new TSDRRestconfCollectorModule(null,null);
        TSDRRestconfCollectorModule module2 = new TSDRRestconfCollectorModule(null,null);
        TSDRRestconfCollectorModule module3 = new TSDRRestconfCollectorModule(null,null);
        setupModuleWithMocks(module1,"X");
        setupModuleWithMocks(module2,"X");
        setupModuleWithMocks(module3,"Y");
        module1.equals(new TSDRRestconfCollectorModule(null,null));
        module1.canReuseInstance(module2);
        module1.getRpcRegistry();
        module1.getLogger();
        module1.getIdentifier();
        module1.reuseInstance(module1);
        module1.hashCode();
        module1.isSame(module1);
        module1.isSame(module2);
        module1.isSame(module3);
        try {
            module1.isSame(null);
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e != null);
        }
        module1.validate();
        try {
            executeResolveDependencies(module1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        module1.setRpcRegistry(null);



        AbstractTSDRRestconfCollectorModuleFactory factory = new AbstractTSDRRestconfCollectorModuleFactory() {
            @Override
            public Set<Class<? extends AbstractServiceInterface>> getImplementedServiceIntefaces() {
                return super.getImplementedServiceIntefaces();
            }

            @Override
            public Module createModule(final String instanceName, final DependencyResolver dependencyResolver,
                final BundleContext bundleContext) {

                return super.createModule(instanceName, dependencyResolver, bundleContext);
            }

            @Override
            public Module createModule(final String instanceName, final DependencyResolver dependencyResolver,
                final DynamicMBeanWithInstance old, final BundleContext bundleContext) throws Exception {

                return super.createModule(instanceName, dependencyResolver, old, bundleContext);
            }

            @Override
            public TSDRRestconfCollectorModule instantiateModule(final String instanceName,
                final DependencyResolver dependencyResolver, final TSDRRestconfCollectorModule oldModule, final AutoCloseable oldInstance,
                final BundleContext bundleContext) {

                return super.instantiateModule(instanceName, dependencyResolver, oldModule, oldInstance, bundleContext);
            }

            @Override
            public TSDRRestconfCollectorModule instantiateModule(final String instanceName,
                final DependencyResolver dependencyResolver, final BundleContext bundleContext) {

                return super.instantiateModule(instanceName, dependencyResolver, bundleContext);
            }

            @Override
            public TSDRRestconfCollectorModule handleChangedClass(final DynamicMBeanWithInstance old) throws Exception {
                return super.handleChangedClass(old);
            }

            @Override
            public Set<TSDRRestconfCollectorModule> getDefaultModules(
                final DependencyResolverFactory dependencyResolverFactory, final BundleContext bundleContext) {

                return super.getDefaultModules(dependencyResolverFactory, bundleContext);
            }
        };
        factory.getImplementationName();
        factory.getImplementedServiceIntefaces();
        factory.getDefaultModules(null,null);
        factory.isModuleImplementingServiceInterface(AbstractServiceInterface.class);
        try {
            factory.handleChangedClass(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            DependencyResolver dpr = Mockito.mock(DependencyResolver.class);
            BundleContext b = Mockito.mock(BundleContext.class);
            factory.createModule("tsdr-syslog-collector",dpr , b);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            DependencyResolver dpr = Mockito.mock(DependencyResolver.class);
            BundleContext b = Mockito.mock(BundleContext.class);
            DynamicMBeanWithInstance old = Mockito.mock(DynamicMBeanWithInstance.class);
            factory.createModule("tsdr-syslog-collector",dpr , old,b);
        } catch (Exception e){
            e.printStackTrace();
        }
        try {
            factory.instantiateModule(null, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void executeResolveDependencies(final Object module) throws NoSuchMethodException, InvocationTargetException,
        IllegalAccessException {

        Method methods[] = module.getClass().getSuperclass().getDeclaredMethods();
        for(Method m:methods){
            if(m.getName().equals("resolveDependencies")){
                m.setAccessible(true);
                m.invoke(module,null);
                return;
            }
        }
    }

    /**
     * sets up the module and provides it with the necessary mocks.
     * @param obj the module to be set up
     * @param factoryName the name of the module
     */
    public static final void setupModuleWithMocks(final Object obj,final String factoryName) {
        try {
            org.opendaylight.controller.sal.binding.api.RpcProviderRegistry rpcProviderRegistry
                = Mockito.mock(org.opendaylight.controller.sal.binding.api.RpcProviderRegistry.class);
            DependencyResolver dpr = Mockito.mock(DependencyResolver.class);
            javax.management.ObjectName rpcRegistry = Mockito.mock(javax.management.ObjectName.class);
            ModuleIdentifier id = Mockito.mock(ModuleIdentifier.class);
            Mockito.when(id.getFactoryName()).thenReturn(factoryName);
            Mockito.when(id.getInstanceName()).thenReturn(factoryName);
            Mockito.doNothing().when(dpr).validateDependency(Mockito.any(Class.class),
                Mockito.any(javax.management.ObjectName.class),
                Mockito.any(org.opendaylight.controller.config.api.JmxAttribute.class));

            Field field = findField("rpcRegistryDependency",obj.getClass());
            if (field != null) {
                field.set(obj, rpcProviderRegistry);
            }

            field = findField("identifier",obj.getClass());
            if (field != null) {
                field.set(obj, id);
            }

            field = findField("dependencyResolver",obj.getClass());
            if (field != null) {
                field.set(obj, dpr);
            }

            field = findField("rpcRegistry",obj.getClass());
            if (field != null) {
                field.set(obj, rpcRegistry);
            }

        } catch (IllegalAccessException e) {
        }
    }

    public static final Field findField(final String name, final Class clz) {
        Field[] fields = clz.getDeclaredFields();
        for (Field f: fields) {
            if (f.getName().equals(name)) {
                f.setAccessible(true);
                return f;
            }
        }
        if (clz.getSuperclass() == null) {
            return null;
        }
        return findField(name, clz.getSuperclass());
    }

    /**
     * This function takes the module, and the properties that need to be initially found in the filter chain
     * configuration. It then creates a bundle context that would eventually give the module these properties.
     * Finally, the function returns the filterChainConfiguration, so that the caller of the function can then check
     * out whether filterChainConfiguration.update function was called with the correct parameters.
     * @param module the module to be configured
     * @param properties the properties that we want the module to find when querying the filterchain configuration
     * @param isConfigurationAdminInstalled when set to false, the configuration admin will be null
     * @return the configuration object of the filter chain, could be used to check if its update method is called
     */
    private Configuration prepareModuleForFilterRegistration(final TSDRRestconfCollectorModule module, final Dictionary properties,
        final boolean isConfigurationAdminInstalled) throws IOException {

        ServiceReference<ConfigurationAdmin> configurationAdminServiceReference = null;
        if (isConfigurationAdminInstalled) {
            configurationAdminServiceReference = Mockito.mock(ServiceReference.class);
        }

        Configuration filterChainConfiguration = Mockito.mock(Configuration.class);
        Mockito.when(filterChainConfiguration.getProperties()).thenReturn(properties);

        ConfigurationAdmin configurationAdmin = Mockito.mock(ConfigurationAdmin.class);
        Mockito.when(configurationAdmin.getConfiguration("org.opendaylight.aaa.filterchain"))
            .thenReturn(filterChainConfiguration);

        BundleContext bundleContext = Mockito.mock(BundleContext.class);
        Mockito.when(bundleContext.getServiceReference(ConfigurationAdmin.class))
            .thenReturn(configurationAdminServiceReference);
        Mockito.when(bundleContext.getService(configurationAdminServiceReference)).thenReturn(configurationAdmin);

        module.setBundleContext(bundleContext);

        return filterChainConfiguration;
    }

    /**
     * This function takes the module, and the properties that need to be initially found in the filter chain
     * configuration. It then creates a bundle context that would eventually give the module these properties.
     * Finally, the function returns the filterChainConfiguration, so that the caller of the function can then check
     * out whether filterChainConfiguration.update function was called with the correct parameters.
     * @param module the module to be configured
     * @param properties the properties that we want the module to find when querying the filterchain configuration
     * @return the configuration object of the filter chain, could be used to check if its update method is called
     */
    private Configuration prepareModuleForFilterRegistration(final TSDRRestconfCollectorModule module, final Dictionary properties)
        throws IOException {

        return prepareModuleForFilterRegistration(module, properties, true);
    }

    /**
     * called after each test to make sure that the TSDRRestconfCollectorLogger instance is cleaned.
     */
    @After
    public void teardown() {
        TSDRRestconfCollectorLogger.setInstance(null);
    }
}
