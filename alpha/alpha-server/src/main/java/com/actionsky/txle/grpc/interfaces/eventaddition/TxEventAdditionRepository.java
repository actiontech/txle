/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.grpc.interfaces.eventaddition;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Gannalyo
 * @since 2019/3/29
 */
public interface TxEventAdditionRepository extends CrudRepository<TxEventAddition, Long> {

    @Query("FROM TxEventAddition T WHERE T.globalTxId = ?1 AND T.compensateStatus = 0 ORDER BY T.executeOrder DESC")
    List<TxEventAddition> selectDescEventByGlobalTxId(String globalTxId);

    @Query("FROM TxEventAddition T WHERE T.instanceId = ?1 AND T.globalTxId = ?2 AND T.compensateStatus = 0 ORDER BY T.executeOrder DESC")
    List<TxEventAddition> selectDescEventByGlobalTxId(String instanceId, String globalTxId);

    @Transactional
    @Modifying
    @Query("UPDATE TxEventAddition T SET T.compensateStatus = 1 WHERE T.instanceId = ?1 AND T.globalTxId = ?2 AND T.localTxId = ?3 AND T.compensateStatus = 0")
    int updateCompensateStatus(String instanceId, String globalTxId, String localTxId);
}
