/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.server.kafka;

import org.apache.servicecomb.saga.alpha.core.kafka.IKafkaMessageRepository;
import org.apache.servicecomb.saga.alpha.core.kafka.KafkaMessage;
import org.apache.servicecomb.saga.alpha.core.kafka.KafkaMessageStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;

public class KafkaMessageRepositoryImpl implements IKafkaMessageRepository {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private KafkaMessageEntityRepository kafkaMessageEntityRepository;

    public KafkaMessageRepositoryImpl(KafkaMessageEntityRepository kafkaMessageEntityRepository) {
        this.kafkaMessageEntityRepository = kafkaMessageEntityRepository;
    }

    @Override
    public boolean save(KafkaMessage message) {
        try {
            kafkaMessageEntityRepository.save(message);
            return true;
        } catch (Exception e) {
            LOG.error("Failed to save kafka message.", e);
        }
        return false;
    }

    @Override
    public List<KafkaMessage> findMessageListByGlobalTxId(String globalTxId, int status) {
        return kafkaMessageEntityRepository.findMessageListByGlobalTxId(globalTxId, status);
    }

    @Override
    public boolean updateMessageStatusByIdList(List<Long> idList, KafkaMessageStatus messageStatus) {
        return kafkaMessageEntityRepository.updateMessageStatusByIdList(idList, messageStatus.toInteger()) > 0;
    }

    @Override
    public boolean updateMessageStatusByIdListAndStatus(List<Long> idList, KafkaMessageStatus messageStatus, KafkaMessageStatus originalStatus) {
        return kafkaMessageEntityRepository.updateMessageStatusByIdListAndStatus(idList, messageStatus.toInteger(), originalStatus.toInteger()) > 0;
    }

}
