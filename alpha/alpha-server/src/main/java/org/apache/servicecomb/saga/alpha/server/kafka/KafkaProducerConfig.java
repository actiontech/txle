package org.apache.servicecomb.saga.alpha.server.kafka;

import org.apache.servicecomb.saga.alpha.core.kafka.IKafkaMessageProducer;
import org.apache.servicecomb.saga.alpha.core.kafka.IKafkaMessageRepository;
import org.apache.servicecomb.saga.alpha.server.ConfigLoading;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Kafka producer configuration.
 *
 * @author Gannalyo
 * @date 2018/12/3
 */
@Configuration
@EnableKafka
//@ConfigurationProperties(prefix="spring.kafka")
//@PropertySource({"classpath:kafka.properties"})
public class KafkaProducerConfig {

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(ConfigLoading.loadKafkaProperties()));
    }

    @Bean
    IKafkaMessageProducer kafkaMessageProducer(IKafkaMessageRepository kafkaMessageRepository) {
        return new KafkaMessageProducer(kafkaMessageRepository);
    }

    @Bean
    IKafkaMessageRepository kafkaMessageRepository(KafkaMessageEntityRepository kafkaMessageEntityRepository) {
        return new KafkaMessageRepositoryImpl(kafkaMessageEntityRepository);
    }
}