package org.apache.servicecomb.saga.alpha.server.kafka;

import org.apache.servicecomb.saga.alpha.core.kafka.KafkaMessage;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface KafkaMessageEntityRepository extends CrudRepository<KafkaMessage, Long> {

    @Query("SELECT T FROM KafkaMessage T WHERE T.status = ?2 AND T.globaltxid = ?1")
    List<KafkaMessage> findMessageListByGlobalTxId(String globalTxId, int status);

//	@Query("from TxleUndoLogEntity where globalTxId = ?1")
//	List<TxEvent> findTxleUndoLogEntityByGlobalTxId(@Param("globalTxId") String globalTxId);
//
//	@Query("from TxleUndoLogEntity where globalTxId = ?1 and localTxId = ?2")
//	List<TxEvent> findTxleUndoLogEntityByGlobalTxId(@Param("globalTxId") String globalTxId, @Param("localTxId") String localTxId);

    @Transactional
    @Modifying
    @Query("UPDATE KafkaMessage T SET T.status = ?2 WHERE T.id IN ?1")
    int updateMessageStatusByIdList(List<Long> idList, int status);

    @Transactional
    @Modifying
    @Query("UPDATE KafkaMessage T SET T.status = ?2 WHERE T.status = ?3 AND T.id IN ?1")
    int updateMessageStatusByIdListAndStatus(List<Long> idList, int status, int initStatus);

}
