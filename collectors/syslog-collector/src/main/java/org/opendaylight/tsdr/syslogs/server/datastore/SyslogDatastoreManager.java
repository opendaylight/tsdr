/*
 * Copyright (c) 2016 TethrNet Technology Co.Ltd and others.  All rights reserved.
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.syslogs.server.datastore;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.tsdr.syslogs.server.decoder.Message;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.ConfigThreadpoolInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.ConfigThreadpoolOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.ConfigThreadpoolOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.DeleteRegisteredFilterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.DeleteRegisteredFilterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.DeleteRegisteredFilterOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.RegisterFilterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.RegisterFilterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.RegisterFilterOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.ShowRegisterFilterOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.ShowRegisterFilterOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.ShowThreadpoolConfigurationOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.ShowThreadpoolConfigurationOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.SyslogDispatcher;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.TsdrSyslogCollectorService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.show.register.filter.output.RegisteredSyslogFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.show.register.filter.output.RegisteredSyslogFilterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.show.register.filter.output.registered.syslog.filter.RegisteredFilterEntity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.show.register.filter.output.registered.syslog.filter.RegisteredFilterEntityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.SyslogFilter;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.SyslogFilterBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.SyslogFilterKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.SyslogListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.SyslogListenerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.SyslogListenerKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.syslog.filter.FilterEntity;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.syslog.filter.FilterEntityBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.syslog.filter.Listener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.syslog.filter.ListenerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.syslog.collector.rev151007.syslog.dispatcher.syslog.filter.ListenerKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This SyslogDatastoreManager handles the initialization of the data
 * structure of Syslog collector. it also implements all its RPC and
 * provides multi-threads to filter the incoming Syslog messages.
 *
 * @author Wei Lai(weilai@tethrnet.com)
 * @author Wenbo Hu(wenbhu@tethrnet.com)
 */
public class SyslogDatastoreManager implements TsdrSyslogCollectorService, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(TsdrSyslogCollectorService.class);

    private final ThreadPoolExecutor threadPool;
    private final DataBroker dataBroker;
    private final Map<String, String> registerMap = new ConcurrentHashMap<>();
    private final Map<String, RegisteredListener> listenerMap = new ConcurrentHashMap<>();

    private SyslogDatastoreManager(DataBroker dataBroker, int coreThreadPoolSize, int maxThreadpoolSize,
            long keepAliveTime, int queueSize) {
        this.dataBroker = Objects.requireNonNull(dataBroker);
        this.threadPool = new ThreadPoolExecutor(coreThreadPoolSize, maxThreadpoolSize, keepAliveTime,
                TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(queueSize));
        this.threadPool.prestartAllCoreThreads();

        LOG.info("SyslogDatastoreManager created: coreThreadPoolSize: {}, maxThreadpoolSize: {}, keepAliveTime: {}, "
            + "queueSize: {}", coreThreadPoolSize, maxThreadpoolSize, keepAliveTime, queueSize);
    }

    public static SyslogDatastoreManager getInstance(DataBroker dataBroker, int coreThreadPoolSize,
            int maxThreadpoolSize, long keepAliveTime, int queueSize) {
        return new SyslogDatastoreManager(dataBroker, coreThreadPoolSize, maxThreadpoolSize, keepAliveTime, queueSize);
    }

    @Override
    public void close() {
        threadPool.shutdown();

        LOG.info("SyslogDatastoreManager closed");
    }

    public void execute(String ipaddress, Message message) {
        threadPool.execute(new WorkerThread(dataBroker, message));
    }

    @Override
    public Future<RpcResult<ShowThreadpoolConfigurationOutput>> showThreadpoolConfiguration() {

        int currentThreadpoolQueueSize = threadPool.getQueue().size();
        int currentThreadpoolQueueRemainingCapacity = threadPool.getQueue().remainingCapacity();
        long currentThreadpoolKeepAliveTime = threadPool.getKeepAliveTime(TimeUnit.SECONDS);

        LOG.info("currentThreadpoolKeepAliveTime" + currentThreadpoolKeepAliveTime);
        LOG.info("currentThreadpoolQueueSize" + currentThreadpoolQueueSize);
        LOG.info("currentThreadpoolQueueRemainingCapacity" + currentThreadpoolQueueRemainingCapacity);

        ShowThreadpoolConfigurationOutput output = new ShowThreadpoolConfigurationOutputBuilder()
                .setCoreThreadNumber(threadPool.getCorePoolSize())
                .setMaxThreadNumber(threadPool.getMaximumPoolSize())
                .setCurrentAliveThreadNumber(threadPool.getPoolSize())
                .setKeepAliveTime((int) currentThreadpoolKeepAliveTime)
                .setQueueRemainingCapacity(currentThreadpoolQueueRemainingCapacity)
                .setQueueUsedCapacity(currentThreadpoolQueueSize)
                .build();

        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public Future<RpcResult<DeleteRegisteredFilterOutput>> deleteRegisteredFilter(DeleteRegisteredFilterInput input) {

        String listenerID = registerMap.get(input.getFilterId());
        RegisteredListener registeredListener = listenerMap.get(listenerID);
        boolean closeResult = registeredListener.close();
        if (!closeResult) {
            LOG.error("listener registration close failed");
            DeleteRegisteredFilterOutput output = new DeleteRegisteredFilterOutputBuilder()
                    .setResult("listener registration close failed")
                    .build();
            return RpcResultBuilder.success(output).buildFuture();
        }

        InstanceIdentifier<SyslogListener> syslogListenerIID =
                InstanceIdentifier.create(SyslogDispatcher.class)
                        .child(SyslogListener.class, new SyslogListenerKey(listenerID));

        WriteTransaction deleteTransaction = dataBroker.newWriteOnlyTransaction();
        InstanceIdentifier<SyslogFilter> filterIID = InstanceIdentifier.create(SyslogDispatcher.class)
                .child(SyslogFilter.class, new SyslogFilterKey(input.getFilterId()));
        deleteTransaction.delete(LogicalDatastoreType.CONFIGURATION, filterIID);
        deleteTransaction.delete(LogicalDatastoreType.OPERATIONAL, syslogListenerIID);

        try {
            deleteTransaction.submit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("filter delete failed");
            DeleteRegisteredFilterOutput output = new DeleteRegisteredFilterOutputBuilder()
                    .setResult("filter delete failed")
                    .build();
            return RpcResultBuilder.success(output).buildFuture();
        }

        DeleteRegisteredFilterOutput output = new DeleteRegisteredFilterOutputBuilder()
                .setResult("filter delete successfully")
                .build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    @VisibleForTesting
    public Map<String, String> getRegisterMap() {
        return registerMap;
    }

    @VisibleForTesting
    public void setRegisterMap(Map<String, String> registerMap) {
        this.registerMap.putAll(registerMap);
    }

    @VisibleForTesting
    public Map<String, RegisteredListener> getListenerMap() {
        return listenerMap;
    }

    @VisibleForTesting
    public void setListenerMap(Map<String, RegisteredListener> listenerMap) {
        this.listenerMap.putAll(listenerMap);
    }

    @Override
    public Future<RpcResult<ShowRegisterFilterOutput>> showRegisterFilter() {

        ReadTransaction transaction = dataBroker.newReadOnlyTransaction();
        InstanceIdentifier<SyslogDispatcher> iid =
                InstanceIdentifier.create(SyslogDispatcher.class);
        ListenableFuture<Optional<SyslogDispatcher>> future = transaction.read(LogicalDatastoreType.CONFIGURATION, iid);
        Optional<SyslogDispatcher> optional;
        try {
            optional = future.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Reading Filter failed");
            ShowRegisterFilterOutput output = new ShowRegisterFilterOutputBuilder()
                    .setResult("Reading Filter failed")
                    .build();
            return RpcResultBuilder.success(output).buildFuture();
        }

        if (optional.isPresent() && !optional.get().getSyslogFilter().isEmpty()) {

            LOG.info("reading filter success");

            List<SyslogFilter> filters = optional.get().getSyslogFilter();
            LOG.info("currently registered filters are:     " + filters);
            List<RegisteredSyslogFilter> registeredSyslogFiltersList = new ArrayList<>();
            for (SyslogFilter filter : filters) {
                LOG.info("filter entity:  " + filter.getFilterEntity());
                LOG.info("filter ID:  " + filter.getFilterId());

                RegisteredFilterEntity registeredFilterEntity = new RegisteredFilterEntityBuilder()
                        .setApplication(filter.getFilterEntity().getApplication())
                        .setContent(filter.getFilterEntity().getContent())
                        .setFacility(filter.getFilterEntity().getFacility())
                        .setHost(filter.getFilterEntity().getHost())
                        .setPid(filter.getFilterEntity().getPid())
                        .setSid(filter.getFilterEntity().getSid())
                        .setSeverity(filter.getFilterEntity().getSeverity())
                        .build();

                RegisteredSyslogFilter filter1 = new RegisteredSyslogFilterBuilder()
                        .setFilterId(filter.getFilterId())
                        .setRegisteredFilterEntity(registeredFilterEntity)
                        .setCallbackUrl(filter.getCallbackUrl())
                        .build();
                registeredSyslogFiltersList.add(filter1);
            }
            ShowRegisterFilterOutput output = new ShowRegisterFilterOutputBuilder()
                    .setResult("registered filters are:")
                    .setRegisteredSyslogFilter(registeredSyslogFiltersList)
                    .build();

            return RpcResultBuilder.success(output).buildFuture();
        } else {
            ShowRegisterFilterOutput output = new ShowRegisterFilterOutputBuilder()
                    .setResult("no registered filter")
                    .build();
            return RpcResultBuilder.success(output).buildFuture();
        }
    }

    @Override
    public Future<RpcResult<ConfigThreadpoolOutput>> configThreadpool(ConfigThreadpoolInput input) {

        if (input.getCoreThreadNumber() != 0) {
            threadPool.setCorePoolSize(input.getCoreThreadNumber());
        }

        if (input.getMaxThreadNumber() != 0) {
            threadPool.setMaximumPoolSize(input.getMaxThreadNumber());
        }
        if (input.getKeepAliveTime() != 0) {

            threadPool.setKeepAliveTime(input.getKeepAliveTime(), TimeUnit.SECONDS);
        }
        ConfigThreadpoolOutput output = new ConfigThreadpoolOutputBuilder()
                .setResult("success")
                .build();

        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public Future<RpcResult<RegisterFilterOutput>> registerFilter(RegisterFilterInput input) {

        LOG.info("Received a new Register");
        String url = input.getCallbackUrl();
        WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
        String filterID = UUID.randomUUID().toString();
        String listenerUUID = UUID.randomUUID().toString();

        FilterEntity filterEntity = new FilterEntityBuilder()
                .setSeverity(input.getSeverity())
                .setFacility(input.getFacility())
                .setHost(input.getHost())
                .setApplication(input.getApplication())
                .setSid(input.getSid())
                .setPid(input.getPid())
                .setContent(input.getContent())
                .build();
        InstanceIdentifier<SyslogFilter> filterIID = InstanceIdentifier.create(SyslogDispatcher.class)
                .child(SyslogFilter.class, new SyslogFilterKey(filterID));
        SyslogFilter filter = new SyslogFilterBuilder()
                .setFilterId(filterID)
                .setFilterEntity(filterEntity)
                .setCallbackUrl(url)
                .build();
        transaction.merge(LogicalDatastoreType.CONFIGURATION, filterIID, filter);

        InstanceIdentifier<Listener> listenerIID =
                filterIID.child(Listener.class, new ListenerKey(listenerUUID));
        Listener listener = new ListenerBuilder().setListenerId(listenerUUID).build();
        transaction.merge(LogicalDatastoreType.CONFIGURATION, listenerIID, listener);
        //Create Listener on Operational Tree
        InstanceIdentifier<SyslogListener> syslogListenerIID =
                InstanceIdentifier.create(SyslogDispatcher.class)
                        .child(SyslogListener.class, new SyslogListenerKey(listenerUUID));

        SyslogListener syslogListener =
                new SyslogListenerBuilder().setListenerId(listenerUUID).setSyslogMessage("").build();
        transaction.merge(LogicalDatastoreType.OPERATIONAL, syslogListenerIID, syslogListener);

        try {
            transaction.submit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error(e.getMessage());
        }

        final RegisterFilterOutput output = new RegisterFilterOutputBuilder().setListenerId(listenerUUID).build();

        RegisteredListener newRrgisteredListener = new RegisteredListener(dataBroker, listenerUUID, url);

        registerMap.put(filterID, listenerUUID);
        listenerMap.put(listenerUUID, newRrgisteredListener);

        newRrgisteredListener.listen();

        LOG.info(newRrgisteredListener.toString());

        return RpcResultBuilder.success(output).buildFuture();
    }

    private static class WorkerThread implements Runnable {
        private final DataBroker dataBroker;
        private final Message message;

        WorkerThread(DataBroker dataBroker, Message message) {
            this.dataBroker = dataBroker;
            this.message = message;
        }

        public List<SyslogFilter> getFilters() {
            if (dataBroker == null) {
                return null;
            }
            ReadTransaction transaction = dataBroker.newReadOnlyTransaction();
            InstanceIdentifier<SyslogDispatcher> iid =
                    InstanceIdentifier.create(SyslogDispatcher.class);
            ListenableFuture<Optional<SyslogDispatcher>> future =
                    transaction.read(LogicalDatastoreType.CONFIGURATION, iid);
            Optional<SyslogDispatcher> optional = Optional.absent();
            try {
                optional = future.get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.warn("Reading Filter failed:", e);
                return null;
            }
            if (optional.isPresent()) {
                LOG.info("reading filter success");
                return optional.get().getSyslogFilter();
            } else {
                return null;
            }
        }

        private List<Listener> getListenerList(String filterID) {
            if (dataBroker == null) {
                return null;
            }
            ReadTransaction transaction = dataBroker.newReadOnlyTransaction();
            InstanceIdentifier<SyslogFilter> iid = InstanceIdentifier.create(SyslogDispatcher.class)
                    .child(SyslogFilter.class, new SyslogFilterKey(filterID));
            ListenableFuture<Optional<SyslogFilter>> future =
                    transaction.read(LogicalDatastoreType.CONFIGURATION, iid);
            Optional<SyslogFilter> optional = Optional.absent();
            try {
                optional = future.get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.warn("Reading Listener failed:", e);
            }
            if (optional.isPresent()) {
                return optional.get().getListener();

            } else {
                return null;
            }
        }

        private void update(List<Listener> nodes) {
            if (dataBroker == null) {
                return;
            }
            WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
            InstanceIdentifier<SyslogDispatcher> baseIID = InstanceIdentifier.create(SyslogDispatcher.class);
            for (Listener node : nodes) {
                String listenerUUID = node.getListenerId();
                InstanceIdentifier<SyslogListener> iid =
                        baseIID.child(SyslogListener.class, new SyslogListenerKey(listenerUUID));
                SyslogListener listener = new SyslogListenerBuilder()
                        .setListenerId(listenerUUID)
                        .setSyslogMessage(message.getContent())
                        .build();
                transaction.put(LogicalDatastoreType.OPERATIONAL, iid, listener);
            }
            transaction.submit();

        }

        @Override
        public void run() {
            Message msg = this.message;
            List<Listener> nodes = new ArrayList<>();
            if (msg != null && this.getFilters() != null) {
                List<SyslogFilter> filters = this.getFilters();
                for (SyslogFilter filter : filters) {
                    MessageFilter messageFilter = MessageFilter.FilterBuilder.create(filter.getFilterEntity());
                    if (messageFilter.equals(msg)) {
                        //Match
                        nodes.addAll(getListenerList(filter.getFilterId()));
                    }
                }
            }
            update(nodes);
        }
    }
}
