package org.apache.servicecomb.saga.alpha.server.kafka;

import com.google.gson.GsonBuilder;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.RetriableException;
import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.apache.servicecomb.saga.alpha.core.kafka.IKafkaMessageProducer;
import org.apache.servicecomb.saga.alpha.core.kafka.IKafkaMessageRepository;
import org.apache.servicecomb.saga.alpha.core.kafka.KafkaMessage;
import org.apache.servicecomb.saga.alpha.core.kafka.KafkaMessageStatus;
import org.apache.servicecomb.saga.common.EventType;
import org.apache.servicecomb.saga.common.rmi.accidentplatform.AccidentType;
import org.apache.servicecomb.saga.common.rmi.accidentplatform.IAccidentPlatformService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

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
    private KafkaProducer kafkaProducer;

    private IKafkaMessageRepository kafkaMessageRepository;

    @Autowired
    IAccidentPlatformService accidentPlatformService;

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

                    // send msg
                    sendMessage(event, messageList, idList);
                }
            }
        } catch (Exception e) {
            LOG.error("Fail to send Kafka message - localTxId = " + event.localTxId(), e);
        }
    }

    private void sendMessage(TxEvent event, List<KafkaMessage> messageList, List<Long> idList) {
        try {
            ProducerRecord<String, String> record = new ProducerRecord<>("default_topic", new GsonBuilder().create().toJson(messageList));
            kafkaProducer.send(record, (metadata, exception) -> {
                if (exception == null) {
                    LOG.info("Successfully to send Kafka message - globalTxId = [{}].", event.globalTxId());
                    // To update message's status to 'successful'.
                    kafkaMessageRepository.updateMessageStatusByIdList(idList, KafkaMessageStatus.SUCCESSFUL);
                } else {
                    if (exception instanceof RetriableException) {
                        // Kafka will retry automatically for some exceptions which can possible be successful by retrying.
                    } else {
                        LOG.error("Unsuccessfully to send Kafka message - globalTxId = [{}].", event.globalTxId(), exception);

                        // To report message to Accident Platform.
                        accidentPlatformService.reportMsgToAccidentPlatform(AccidentType.SEND_MESSAGE_ERROR, event.globalTxId(), event.localTxId());

                        // To update message's status to 'failed'.
                        kafkaMessageRepository.updateMessageStatusByIdList(idList, KafkaMessageStatus.FAILED);
                    }
                }
            });
        } catch (Exception e) {
            LOG.error("To send message to Kafka exception - globalTxId = [{}].", event.globalTxId(), e);
        }
    }

}
