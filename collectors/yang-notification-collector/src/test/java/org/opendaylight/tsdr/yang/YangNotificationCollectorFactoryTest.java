/*
 * Copyright © 2018 Kontron Canada Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.yang;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import com.google.common.util.concurrent.ListenableScheduledFuture;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventListener;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.mdsal.dom.api.DOMNotificationService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.tsdr.spi.scheduler.SchedulerService;
import org.opendaylight.tsdr.yang.notification.YangNotificationCollectorFactory;
import org.opendaylight.tsdr.yang.utils.MockSchemaService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRLogRecordOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRMetricRecordOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.tsdr.yang.collector.config.yang.notification.rev181005.NotificationSubscription;
import org.opendaylight.yang.gen.v1.urn.opendaylight.tsdr.yang.collector.config.yang.notification.rev181005.NotificationSubscriptionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.tsdr.yang.collector.config.yang.notification.rev181005.notification.subscription.Notifications;
import org.opendaylight.yang.gen.v1.urn.opendaylight.tsdr.yang.collector.config.yang.notification.rev181005.notification.subscription.NotificationsBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;

@Ignore
public class YangNotificationCollectorFactoryTest {
    private static class TestListener implements EventListener {
        /**
         * Logger instance.
         */
        private Logger logger;

        protected Logger getLogger() {
            Logger log = logger;
            if (log == null) {
                log = mock(Logger.class);
                logger = log;
            }
            return log;
        }
    }

    private static final InstanceIdentifier<NotificationSubscription> NOTIFICATION_CONFIG_IID =
            InstanceIdentifier.builder(NotificationSubscription.class).build();
    private static final DataTreeIdentifier<NotificationSubscription> NOTIFICATION_CONFIG_PATH =
            DataTreeIdentifier.create(LogicalDatastoreType.CONFIGURATION, NOTIFICATION_CONFIG_IID);

    private YangNotificationCollectorFactory factoryObject;

    @Mock
    private TsdrCollectorSpiService tsdrCollectorSpiService;

    @Mock
    private SchedulerService schedulerService;

    @Mock
    private DataBroker dataBroker;

    @Mock
    private ListenerRegistration<YangNotificationCollectorFactory> yangNotificationCollectorConfigReg;
    @Mock
    private ListenableScheduledFuture<?> mockFuture;

    @Mock
    private DOMNotificationService domNotificationService;

    @Mock
    private ListenerRegistration<TestListener> reg;

    @Captor
    private ArgumentCaptor<Runnable> runnableCaptor;

    private DOMSchemaService domSchemaService;

    @Before
    public void setup() {
        domSchemaService = new MockSchemaService();
        MockitoAnnotations.initMocks(this);
        when(tsdrCollectorSpiService.insertTSDRLogRecord(any()))
                .thenReturn(RpcResultBuilder.success(new InsertTSDRLogRecordOutputBuilder().build()).buildFuture());
        when(tsdrCollectorSpiService.insertTSDRMetricRecord(any()))
                .thenReturn(RpcResultBuilder.success(new InsertTSDRMetricRecordOutputBuilder().build()).buildFuture());
        doReturn(mockFuture).when(schedulerService)
                .scheduleTaskAtFixedRate(runnableCaptor.capture(), anyLong(), anyLong());
        //when(domNotificationService.registerNotificationListener(any(), anyCollection())).thenReturn(reg);
        when(dataBroker.registerDataTreeChangeListener(eq(NOTIFICATION_CONFIG_PATH),
                any(YangNotificationCollectorFactory.class))).thenReturn(yangNotificationCollectorConfigReg);
        final Notifications notification = new NotificationsBuilder()
                .setNotification("(http://netconfcentral.org/ns/toaster?revision=2009-11-20)toasterRestocked")
                .build();
        factoryObject = new YangNotificationCollectorFactory(
                dataBroker, tsdrCollectorSpiService, schedulerService, domNotificationService, domSchemaService);

    }

    private DataObjectModification<NotificationSubscription> getObjectModification(
            DataObjectModification.ModificationType modificationType,
            NotificationSubscription before, NotificationSubscription after) {
        DataObjectModification<NotificationSubscription> modification = mock(DataObjectModification.class);
        when(modification.getModificationType()).thenReturn(modificationType);
        when(modification.getDataAfter()).thenReturn(after);
        when(modification.getDataBefore()).thenReturn(before);
        when(modification.getDataType()).thenReturn(NotificationSubscription.class);
        return modification;
    }

    private NotificationSubscription createIdentity() {
        final Notifications notification = new NotificationsBuilder()
                .setNotification("(http://netconfcentral.org/ns/toaster?revision=2009-11-20)toasterRestocked")
                .build();
        NotificationSubscriptionBuilder builder = new NotificationSubscriptionBuilder();
        builder.setNotifications(Arrays.asList(notification));
        return builder.build();
    }

    private DataTreeModification<NotificationSubscription> getTreeModification(
            DataObjectModification.ModificationType modificationType, LogicalDatastoreType datastoreType,
            NotificationSubscription before, NotificationSubscription after) {
        DataTreeModification<NotificationSubscription> modification = mock(DataTreeModification.class);
        DataObjectModification<NotificationSubscription>
                objectModification =
                getObjectModification(modificationType, before, after);
        when(modification.getRootNode()).thenReturn(objectModification);
        return modification;
    }


    @Test
    public void testOnDataTreeChanged() {
        List<DataTreeModification<NotificationSubscription>> modificationList = new ArrayList<>();
        modificationList.add(
                getTreeModification(DataObjectModification.ModificationType.WRITE, LogicalDatastoreType.CONFIGURATION,
                        null, createIdentity())
        );
        factoryObject.onDataTreeChanged(modificationList);
        verify(mockFuture, times(0)).cancel(false);
        verify(reg, times(0)).close();

        factoryObject.onDataTreeChanged(modificationList);
        verify(mockFuture, times(1)).cancel(false);
        verify(reg, times(1)).close();
    }


    @After
    public void teardown() {
        factoryObject.close();
    }

    @Test
    public void shutDownTest() {
        List<DataTreeModification<NotificationSubscription>> modificationList = new ArrayList<>();
        modificationList.add(
                getTreeModification(DataObjectModification.ModificationType.WRITE, LogicalDatastoreType.CONFIGURATION,
                        null, createIdentity())
        );
        factoryObject.onDataTreeChanged(modificationList);
        factoryObject.close();
        verify(mockFuture, times(1)).cancel(false);
        verify(reg, times(1)).close();
        verify(yangNotificationCollectorConfigReg, times(1)).close();
    }
}
