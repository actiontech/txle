package org.apache.servicecomb.saga.alpha.core.kafka;

import java.util.List;

public interface IKafkaMessageRepository {

    boolean save(KafkaMessage message);

    List<KafkaMessage> findMessageListByGlobalTxId(String globalTxId, int status);

    boolean updateMessageStatusByIdList(List<Long> idList, KafkaMessageStatus messageStatus);

}
