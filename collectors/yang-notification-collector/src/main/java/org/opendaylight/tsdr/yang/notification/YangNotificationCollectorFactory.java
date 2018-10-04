/*
 * Copyright © 2018 Kontron Canada Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.yang.notification;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMNotificationService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.tsdr.spi.scheduler.SchedulerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.tsdr.yang.collector.config.yang.notification.rev181005.NotificationSubscription;
import org.opendaylight.yang.gen.v1.urn.opendaylight.tsdr.yang.collector.config.yang.notification.rev181005.notification.subscription.Notifications;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this class is responsible to create a yangNotificationCollector instance and manage its life.
 */
@Singleton
public class YangNotificationCollectorFactory implements DataTreeChangeListener<NotificationSubscription>,
        AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(YangNotificationCollectorFactory.class);
    private static final InstanceIdentifier<NotificationSubscription> NOTIFICATION_CONFIG_IID =
            InstanceIdentifier.builder(NotificationSubscription.class).build();
    private static final DataTreeIdentifier<NotificationSubscription> NOTIFICATION_CONFIG_PATH =
            DataTreeIdentifier.create(LogicalDatastoreType.CONFIGURATION, NOTIFICATION_CONFIG_IID);

    private final DOMNotificationService notificationService;
    private final DOMSchemaService domSchemaService;
    private final ListenerRegistration<YangNotificationCollectorFactory> yangNotificationCollectorConfigReg;
    private final TsdrCollectorSpiService collectorSPIService;
    private final SchedulerService schedulerService;
    private YangNotificationCollectorLogger yangNotificationCollectorImpl;

    @Inject
    public YangNotificationCollectorFactory(@Nonnull final DataBroker broker,
                                            @Nonnull final TsdrCollectorSpiService collectorSPIService,
                                            @Nonnull final SchedulerService schedulerService,
                                            @Nonnull final DOMNotificationService notifyService,
                                            @Nonnull final DOMSchemaService domSchemaService) {

        this.domSchemaService = domSchemaService;
        this.collectorSPIService = collectorSPIService;
        this.schedulerService = schedulerService;
        this.notificationService = notifyService;
        yangNotificationCollectorConfigReg = broker.registerDataTreeChangeListener(NOTIFICATION_CONFIG_PATH, this);

        LOG.debug("YangNotificationCollectorFactory Initialized");
    }

    @Override
    @PreDestroy
    public void close() {
        if (yangNotificationCollectorImpl != null) {
            yangNotificationCollectorImpl.close();
        }
        yangNotificationCollectorConfigReg.close();

    }

    @Override
    public void onDataTreeChanged(final Collection<DataTreeModification<NotificationSubscription>> changed) {
        createOrReset(
                changed.stream()
                        .map(DataTreeModification::getRootNode)
                        .map(DataObjectModification::getDataAfter)
                        .map(NotificationSubscription::getNotifications)
                        .flatMap(List::stream)
                        .collect(Collectors.toList())
        );
    }

    private void createOrReset(List<Notifications> config) {
        if (yangNotificationCollectorImpl != null) {
            LOG.info("Closing pre-existing yang notification collector agent...");
            yangNotificationCollectorImpl.close();
        }
        LOG.info("Creating a new yang notification collector agent using configuration");
        yangNotificationCollectorImpl = new YangNotificationCollectorLogger(this.collectorSPIService,
                schedulerService,
                domSchemaService,
                notificationService,
                config);
    }
}
