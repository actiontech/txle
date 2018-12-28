package org.apache.servicecomb.saga.alpha.server.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.servicecomb.saga.alpha.core.kafka.IKafkaMessageProducer;
import org.apache.servicecomb.saga.alpha.core.kafka.IKafkaMessageRepository;
import org.apache.servicecomb.saga.alpha.server.ConfigLoading;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.Map;

/**
 * Kafka producer configuration.
 *
 * @author Gannalyo
 * @date 2018/12/3
 */
@Configuration
//@ConfigurationProperties(prefix="spring.kafka")
@PropertySource({"classpath:kafka.properties"})
public class KafkaProducerConfig {

    @Value("${utx.kafka.enable:false}")
    private boolean enabled;

    @Value("${topic:default_topic}")
    private String topic;

    @Bean
    public KafkaProducer<String, String> kafkaProducer() {
        if (enabled) {
            return new KafkaProducer<>(ConfigLoading.loadKafkaProperties(enabled));
        }
        return null;
    }

    @Bean
    IKafkaMessageProducer kafkaMessageProducer(IKafkaMessageRepository kafkaMessageRepository) {
        return new KafkaMessageProducer(kafkaMessageRepository, enabled, topic);
    }

    @Bean
    IKafkaMessageRepository kafkaMessageRepository(KafkaMessageEntityRepository kafkaMessageEntityRepository) {
        return new KafkaMessageRepositoryImpl(kafkaMessageEntityRepository);
    }
}