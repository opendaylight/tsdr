/*
 * Copyright (c) 2016 Saugo360 and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.yang.notification;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.dom.adapter.BindingToNormalizedNodeCodec;
import org.opendaylight.mdsal.binding.dom.adapter.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
import org.opendaylight.mdsal.dom.api.DOMNotification;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.mdsal.dom.broker.DOMNotificationRouter;
import org.opendaylight.tsdr.spi.scheduler.impl.SchedulerServiceImpl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRLogRecordInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.InsertTSDRLogRecordOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.inserttsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.bi.ba.notification.rev150205.OutOfPixieDustNotification;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.md.sal.test.bi.ba.notification.rev150205.OutOfPixieDustNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.tsdr.yang.notification.collector.config.rev181005.NotificationSubscriptionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.tsdr.yang.notification.collector.config.rev181005.notification.subscription.NotificationsBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

/**
 * Unit tests for YangNotificationCollectorLogger.
 *
 * @author Mathieu Cauffiez
 * @author Thomas Pantelis
 */
public class YangNotificationCollectorLoggerTest {
    private static final SchemaPath NOTIFICATION_SCHEMA_PATH =
            SchemaPath.create(true, OutOfPixieDustNotification.QNAME);

    @Mock
    private TsdrCollectorSpiService mockSpiService;

    private final DOMNotificationRouter domNotificationRouter = DOMNotificationRouter.create(8);
    private final SchedulerServiceImpl schedulerService = new SchedulerServiceImpl();

    private DOMSchemaService domSchemaService;
    private BindingToNormalizedNodeCodec bindingToNormalized;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        AbstractConcurrentDataBrokerTest dataBrokerTest = new AbstractConcurrentDataBrokerTest(true) {
            @Override
            protected Set<YangModuleInfo> getModuleInfos() throws Exception {
                return ImmutableSet.of(BindingReflections.getModuleInfo(OutOfPixieDustNotification.class));
            }
        };

        dataBrokerTest.setup();

        domSchemaService = dataBrokerTest.getDataBrokerTestCustomizer().getSchemaService();
        bindingToNormalized = dataBrokerTest.getDataBrokerTestCustomizer().getBindingToNormalized();
    }

    @After
    public void teardown() {
        schedulerService.close();
        domNotificationRouter.close();
    }

    @Test
    public void testOnNotification() throws InterruptedException {
        List<TSDRLogRecord> storedRecords = Collections.synchronizedList(new ArrayList<>());
        doAnswer(invocation -> {
            storedRecords.addAll(((InsertTSDRLogRecordInput) invocation.getArguments()[0]).getTSDRLogRecord());
            return RpcResultBuilder.success(new InsertTSDRLogRecordOutputBuilder().build()).buildFuture();
        }).when(mockSpiService).insertTSDRLogRecord(any());

        YangNotificationCollectorLogger logger = new YangNotificationCollectorLogger(mockSpiService, schedulerService,
            domSchemaService, domNotificationRouter, new NotificationSubscriptionBuilder().setNotifications(
                Arrays.asList(new NotificationsBuilder().setNotification(
                    OutOfPixieDustNotification.QNAME.toString()).build())).build());

        String reason = "lost it";
        Integer daysTillNewDust = 5;
        final ContainerNode notificationBody = bindingToNormalized.toNormalizedNodeNotification(
                new OutOfPixieDustNotificationBuilder().setReason(reason).setDaysTillNewDust(daysTillNewDust).build());

        DOMNotification mockNotification = mock(DOMNotification.class);
        doReturn(NOTIFICATION_SCHEMA_PATH).when(mockNotification).getType();
        doReturn(notificationBody).when(mockNotification).getBody();

        domNotificationRouter.putNotification(mockNotification);

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
            return storedRecords.size() == 1;
        });

        String recordText = storedRecords.get(0).getRecordFullText();

        JsonParser parser = new JsonParser();
        JsonElement rootElement = parser.parse(new InputStreamReader(new ByteArrayInputStream(recordText.getBytes())));
        Set<Entry<String, JsonElement>> entries = rootElement.getAsJsonObject().entrySet();
        assertEquals(1, entries.size());
        Entry<String, JsonElement> entry = entries.iterator().next();

        assertEquals("opendaylight-test-notification:out-of-pixie-dust-notification", entry.getKey());
        Set<Entry<String, JsonElement>> childEntries = entry.getValue().getAsJsonObject().entrySet();
        assertEquals(reason, childEntries.stream().filter(e -> e.getKey().equals("reason")).findFirst()
                .orElseThrow(() -> new AssertionError("reason element not found")).getValue().getAsString());
        assertEquals(daysTillNewDust, Integer.valueOf(childEntries.stream()
            .filter(e -> e.getKey().equals("days-till-new-dust")).findFirst()
                .orElseThrow(() -> new AssertionError("days-till-new-dust element not found")).getValue().getAsInt()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testClose() {
        DOMNotificationRouter spiedNotificationRouter = spy(domNotificationRouter);
        ListenerRegistration<?> mocklListenerReg = mock(ListenerRegistration.class);
        doReturn(mocklListenerReg).when(spiedNotificationRouter).registerNotificationListener(
                any(), any(Collection.class));

        YangNotificationCollectorLogger logger = new YangNotificationCollectorLogger(mockSpiService, schedulerService,
                domSchemaService, spiedNotificationRouter, new NotificationSubscriptionBuilder().setNotifications(
                    Arrays.asList(new NotificationsBuilder().setNotification(
                        OutOfPixieDustNotification.QNAME.toString()).build())).build());

        logger.close();
        verify(mocklListenerReg).close();

        spiedNotificationRouter = spy(domNotificationRouter);
        logger = new YangNotificationCollectorLogger(mockSpiService, schedulerService,
                domSchemaService, spiedNotificationRouter, new NotificationSubscriptionBuilder().build());

        logger.close();
        verify(spiedNotificationRouter, never()).registerNotificationListener(any(), any(Collection.class));
    }
}
