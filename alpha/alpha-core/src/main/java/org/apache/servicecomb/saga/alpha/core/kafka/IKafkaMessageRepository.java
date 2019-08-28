/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.core.kafka;

import java.util.List;

public interface IKafkaMessageRepository {

    boolean save(KafkaMessage message);

    List<KafkaMessage> findMessageListByGlobalTxId(String globalTxId, int status);

    boolean updateMessageStatusByIdList(List<Long> idList, KafkaMessageStatus messageStatus);

    boolean updateMessageStatusByIdListAndStatus(List<Long> idList, KafkaMessageStatus messageStatus, KafkaMessageStatus originalStatus);

}
