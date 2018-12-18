package org.apache.servicecomb.saga.alpha.server.kafka;

import com.google.gson.GsonBuilder;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.apache.servicecomb.saga.alpha.core.kafka.IKafkaMessageProducer;
import org.apache.servicecomb.saga.alpha.core.kafka.IKafkaMessageRepository;
import org.apache.servicecomb.saga.alpha.core.kafka.KafkaMessage;
import org.apache.servicecomb.saga.alpha.core.kafka.KafkaMessageStatus;
import org.apache.servicecomb.saga.common.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.ProducerListener;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * Kafka message producer.
 *
 * @author Gannalyo
 * @date 2018/12/3
 */
public class KafkaMessageProducer implements IKafkaMessageProducer {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


    @Autowired
    private KafkaTemplate kafkaTemplate;

    private IKafkaMessageRepository kafkaMessageRepository;

    KafkaMessageProducer(IKafkaMessageRepository kafkaMessageRepository) {
        this.kafkaMessageRepository = kafkaMessageRepository;
    }

    @Override
    public void send(TxEvent event) {
        try {
            if (EventType.SagaEndedEvent.name().equals(event.type())) {
                List<KafkaMessage> messageList = kafkaMessageRepository.findMessageListByGlobalTxId(event.globalTxId(), KafkaMessageStatus.INIT.toInteger());
                if (messageList != null && !messageList.isEmpty()) {
                    // To update message's status to 'sending'.
                    List<Long> idList = new ArrayList<>();
                    messageList.forEach(msg -> {
                        idList.add(msg.getId());
                    });
                    kafkaMessageRepository.updateMessageStatusByIdList(idList, KafkaMessageStatus.SENDING);

                    // TODO That's better to support the topic's configuration.
                    kafkaTemplate.send("default_topic", new GsonBuilder().create().toJson(messageList));
                    this.setProducerListener(event, idList);
                }
            }
        } catch (Exception e) {
            LOG.error("Fail to send Kafka message - localTxId = " + event.localTxId(), e);
        }
    }

    private void setProducerListener(TxEvent event, List<Long> idList) {
        // listener for producer
        kafkaTemplate.setProducerListener(new ProducerListener<String, String>() {
            @Override
            public void onSuccess(String topic, Integer partition, String key, String value, RecordMetadata recordMetadata) {
                LOG.info("Successfully to send Kafka message - globalTxId = [{}].", event.globalTxId());
                // To update message's status to 'successful'.
                kafkaMessageRepository.updateMessageStatusByIdList(idList, KafkaMessageStatus.SUCCESSFUL);
            }

            @Override
            public void onError(String topic, Integer partition, String key, String value, Exception exception) {
                LOG.error("Unsuccessfully to send Kafka message - globalTxId = [{}].", event.globalTxId(), exception);
                // TODO retries ???
                // ....

                // To update message's status to 'failed'.
                kafkaMessageRepository.updateMessageStatusByIdList(idList, KafkaMessageStatus.FAILED);
            }

            @Override
            public boolean isInterestedInSuccess() {
                // true, will call the onSuccess function.
                return true;
            }
        });
    }
}
