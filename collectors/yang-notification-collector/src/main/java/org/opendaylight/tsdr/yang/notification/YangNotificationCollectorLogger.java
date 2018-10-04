/*
 * Copyright © 2018 Kontron Canada Inc and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.tsdr.yang.notification;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import org.opendaylight.mdsal.dom.api.DOMNotification;
import org.opendaylight.mdsal.dom.api.DOMNotificationListener;
import org.opendaylight.mdsal.dom.api.DOMNotificationService;
import org.opendaylight.mdsal.dom.api.DOMSchemaService;
import org.opendaylight.tsdr.collector.spi.logger.BatchingLogCollector;
import org.opendaylight.tsdr.spi.scheduler.SchedulerService;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.controller.config.tsdr.collector.spi.rev150915.TsdrCollectorSpiService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.tsdr.yang.collector.config.yang.notification.rev181005.notification.subscription.Notifications;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactory;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonWriterFactory;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class is responsible to subscribe to provided yang notification.
 * When the subscribed yang notification is emit, this class will transform it in json and send it TsdrLogCollector
 */
public final class YangNotificationCollectorLogger extends BatchingLogCollector implements DOMNotificationListener {
    private static final Logger LOG = LoggerFactory.getLogger(YangNotificationCollectorLogger.class);
    private static final JSONCodecFactorySupplier CODEC = JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02;

    private final ListenerRegistration<YangNotificationCollectorLogger> notificationReg;
    private final JSONCodecFactory codecFactory;

    public YangNotificationCollectorLogger(
            final TsdrCollectorSpiService collectorSPIService,
                                         final SchedulerService schedulerService,
                                         final DOMSchemaService domSchemaService,
                                         final DOMNotificationService notificationService,
                                         final List<Notifications> configuration) {


        super(collectorSPIService, schedulerService, "TSDRYangCollector");
        SchemaContext plop = domSchemaService.getGlobalContext();
        this.codecFactory = CODEC.getShared(plop);

        List<SchemaPath> notificationsToListen = new ArrayList<>();
        configuration.stream().forEach(notification -> {
            final SchemaPath notificationToListen =
                    SchemaPath.create(true, QName.create(notification.getNotification()));
            notificationsToListen.add(notificationToListen);
            LOG.debug("Registered for notification {}", notificationToListen);
        });
        notificationReg = notificationService.registerNotificationListener(this, notificationsToListen);
    }

    @Override
    public void onNotification(DOMNotification notification) {
        final String jsonNotification = writeNotificationBodyToJson(notification);
        LOG.debug("onNotification - dom: {} converted to JSON: {}", notification, jsonNotification);
        super.insertLog(notification.getType().getLastComponent().getLocalName(),
                System.currentTimeMillis(),
                jsonNotification,
                DataCategory.YANGNOTIFICATION);
    }

    private String writeNotificationBodyToJson(DOMNotification notification) {
        final Writer writer = new StringWriter();
        final NormalizedNodeStreamWriter jsonStream =
                JSONNormalizedNodeStreamWriter.createExclusiveWriter(codecFactory,
                        notification.getType(), null, JsonWriterFactory.createJsonWriter(writer));
        final NormalizedNodeWriter nodeWriter = NormalizedNodeWriter.forStreamWriter(jsonStream);
        try {
            nodeWriter.write(notification.getBody());
            nodeWriter.close();
        } catch (final IOException e) {
            LOG.error("error while extracting yang notification data", e);
        }
        return writer.toString();
    }

    /**
     * Stop yang notification Collector.
     */
    @Override
    public void close() {
        super.close();
        LOG.debug("Close yang Notification Collector");
        notificationReg.close();
    }
}
