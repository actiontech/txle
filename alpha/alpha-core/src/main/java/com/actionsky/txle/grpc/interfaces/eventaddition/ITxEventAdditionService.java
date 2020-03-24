/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.grpc.interfaces.eventaddition;

import java.util.List;

public interface ITxEventAdditionService {

    void save(TxEventAddition eventAddition);

    List<TxEventAddition> selectDescEventByGlobalTxId(String globalTxId);

    List<TxEventAddition> selectDescEventByGlobalTxId(String instanceId, String globalTxId);

    void updateCompensateStatus(String instanceId, String globalTxId, String localTxId);
}
