/*
 * Copyright (c) 2019 Bell, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.tsdr.persistence.kafka;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.opendaylight.tsdr.spi.util.ConfigFileUtil;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.binary.data.rev160325.storetsdrbinaryrecord.input.TSDRBinaryRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.log.data.rev160325.storetsdrlogrecord.input.TSDRLogRecord;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.metric.data.rev160325.storetsdrmetricrecord.input.TSDRMetricRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The back-end Kafka store.
 */
@Singleton
public class KafkaStore implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaStore.class);
    private static final String CONF_FILE = "./etc/tsdr-persistence-kafka.properties";
    private static final String DEFAULT_TOPIC = "tsdr";
    private static final String KAFKA_CLIENT_ID = "tsdr";
    private static final String METRIC_TOPIC_KEY = "metric-persistency-topic";
    private static final String LOG_TOPIC_KEY = "log-persistency-topic";
    private static final String BINARY_TOPIC_KEY = "binary-persistency-topic";

    private final String metricTopic;
    private final String logTopic;
    private final String binaryTopic;

    private final KafkaProducer kafkaProducer;

    @Inject
    public KafkaStore() {
        // Set topic name
        final Map<String, String> configFile = new HashMap<>();
        try {
            configFile.putAll(ConfigFileUtil.loadConfig(CONF_FILE));
        } catch (IOException e) {
            LOG.error("Error while loading config file", e);
        }
        metricTopic = configFile.getOrDefault(METRIC_TOPIC_KEY, DEFAULT_TOPIC);
        logTopic = configFile.getOrDefault(LOG_TOPIC_KEY, DEFAULT_TOPIC);
        binaryTopic = configFile.getOrDefault(BINARY_TOPIC_KEY, DEFAULT_TOPIC);

        LOG.info("Connecting to kafka server...");
        kafkaProducer = createKafkaProducer();
        LOG.info("kafka producer started");
    }

    private KafkaProducer<String, String> createKafkaProducer() {
        final Properties props = new Properties();
        try {
            props.putAll(ConfigFileUtil.loadConfig(CONF_FILE));
        } catch (IOException e) {
            LOG.error("Error while loading config file", e);
        }

        props.put(ProducerConfig.CLIENT_ID_CONFIG, KAFKA_CLIENT_ID);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());

        return new KafkaProducer<>(props);
    }

    public void store(TSDRMetricRecord mr) {
        kafkaProducer.send(
                new ProducerRecord<>(metricTopic, JsonSerializer.serialize(mr)));
        kafkaProducer.flush();
    }

    public void store(TSDRLogRecord lr) {
        kafkaProducer.send(
                new ProducerRecord<>(logTopic, JsonSerializer.serialize(lr)));
        kafkaProducer.flush();
    }

    public void store(TSDRBinaryRecord lr) {
        kafkaProducer.send(
                new ProducerRecord<>(binaryTopic, JsonSerializer.serialize(lr)));
        kafkaProducer.flush();
    }

    @Override
    @PreDestroy
    public void close() {
        kafkaProducer.flush();
        kafkaProducer.close(10_000, TimeUnit.MILLISECONDS);
    }

}
