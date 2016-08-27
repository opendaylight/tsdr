/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.collectors.cmc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Optional;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SigarCpuDataCollector extends CpuDataCollector {

    private static final Logger LOG = LoggerFactory.getLogger(SigarCpuDataCollector.class);

    private Object sigar;
    private URLClassLoader sigarLoader;

    public SigarCpuDataCollector() throws SigarNotPresentException {
        final Optional<Object> sigarOp = getSigar();

        if (sigarOp.isPresent()) {
            sigar = sigarOp.get();
        }
    }

    /*
     * There is a problem with the Karaf/OSGI/Config Subsystem class loader and Sigar.
     * Sigar cannot be instantiated due to "java.lang.NoClassDefFoundError: Could not initialize class org.hyperic.sigar.Sigar"
     * Hence i had to revert to using a dedicated class loader and reflection to use the
     * functionality in the module.
     */
    private Optional<Object> getSigar() throws SigarNotPresentException {
        try {
            final File sigarFile = new File("./system/org/fusesource/sigar/1.6.4/sigar-1.6.4.jar");

            final URL sigarUrl = sigarFile.toURI().toURL();

            return instantiateSigar(sigarUrl);
        } catch (final MalformedURLException e) {
            // this should never happen as the URL is hard coded and cannot be malformed
            throw new SigarNotPresentException(e);
        }
    }

    private Optional<Object> instantiateSigar(final URL sigarUrl) throws SigarNotPresentException {
        LOG.debug("Instantiating Sigar object");
        sigarLoader = new URLClassLoader(new URL[] {sigarUrl});
        try {
            return Optional.of(sigarLoader.loadClass("org.hyperic.sigar.Sigar").newInstance());
        } catch (final ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new SigarNotPresentException(e);
        }
    }

    @Override
    public Optional<Double> getControllerCpu() {
        LOG.debug("Getting controller CPU data");
        try{
            Method pidM = sigar.getClass().getMethod("getPid", (Class<?>[])null);
            long pid = (Long)pidM.invoke(sigar, (Object[])null);
            Method cpuM = sigar.getClass().getMethod("getProcCpu", long.class);
            Object procCPU = cpuM.invoke(sigar, pid);
            Method procM = procCPU.getClass().getMethod("getPercent",(Class<?>[])null);
            return Optional.of((double) procM.invoke(procCPU, (Object[])null));
        }catch(final NoSuchMethodException | InvocationTargetException | IllegalAccessException err){
            LOG.error("Failed to get Controller CPU, Sigar library is probably not installed...", err);
        }

        return Optional.empty();
    }

    @Override
    public Optional<Double> getMachineCpu() {
        LOG.debug("Getting machine CPU data");
        try {
            Method cpuM = sigar.getClass().getMethod("getCpuPerc", (Class<?>[])null);
            Object cpu = cpuM.invoke(sigar, (Object[])null);
            Method combineM = cpu.getClass().getMethod("getCombined",(Class<?>[])null);
            return Optional.of((double) combineM.invoke(cpu, (Object[])null));
        } catch (final NoSuchMethodException | InvocationTargetException | IllegalAccessException err) {
            LOG.error("Failed to get Machine CPU, Sigar library is probably not installed...", err);
        }

        return Optional.empty();
    }

    @Override
    public void close() throws Exception {
        sigarLoader.close();
    }
}
