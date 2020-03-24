/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.grpc.interfaces.eventaddition;

import java.util.List;

public class TxEventAdditionService implements ITxEventAdditionService {
    private TxEventAdditionRepository eventAdditionRepository;

    public TxEventAdditionService(TxEventAdditionRepository eventAdditionRepository) {
        this.eventAdditionRepository = eventAdditionRepository;
    }

    @Override
    public void save(TxEventAddition eventAddition) {
        eventAdditionRepository.save(eventAddition);
    }

    @Override
    public List<TxEventAddition> selectDescEventByGlobalTxId(String globalTxId) {
        return eventAdditionRepository.selectDescEventByGlobalTxId(globalTxId);
    }

    @Override
    public List<TxEventAddition> selectDescEventByGlobalTxId(String instanceId, String globalTxId) {
        return eventAdditionRepository.selectDescEventByGlobalTxId(instanceId, globalTxId);
    }

    @Override
    public void updateCompensateStatus(String instanceId, String globalTxId, String localTxId) {
        eventAdditionRepository.updateCompensateStatus(instanceId, globalTxId, localTxId);
    }
}
