/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.server.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.servicecomb.saga.alpha.core.kafka.IKafkaMessageProducer;
import org.apache.servicecomb.saga.alpha.core.kafka.IKafkaMessageRepository;
import org.apache.servicecomb.saga.alpha.server.ConfigLoading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.Map;

/**
 * Kafka producer configuration.
 *
 * @author Gannalyo
 * @since 2018/12/3
 */
@Configuration
//@ConfigurationProperties(prefix="spring.kafka")
@PropertySource({"classpath:kafka.properties"})
public class KafkaProducerConfig {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaProducerConfig.class);

    @Value("${topic:default_topic}")
    private String topic;

    @Bean
    public KafkaProducer<String, String> kafkaProducer() {
        Map<String, Object> kafkaProperties = ConfigLoading.loadKafkaProperties();
        try {
            KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(kafkaProperties);
            LOG.info("Successfully to construct kafka producer, bootstrap.servers = {}.", kafkaProperties.get("bootstrap.servers"));
            return kafkaProducer;
        } catch (Exception e) {
            LOG.error("Failed to construct kafka producer, bootstrap.servers = {}.", kafkaProperties.get("bootstrap.servers"), e);
        }
        return null;
    }

    @Bean
    IKafkaMessageProducer kafkaMessageProducer(IKafkaMessageRepository kafkaMessageRepository) {
        return new KafkaMessageProducer(kafkaMessageRepository, topic);
    }

    @Bean
    IKafkaMessageRepository kafkaMessageRepository(KafkaMessageEntityRepository kafkaMessageEntityRepository) {
        return new KafkaMessageRepositoryImpl(kafkaMessageEntityRepository);
    }
}