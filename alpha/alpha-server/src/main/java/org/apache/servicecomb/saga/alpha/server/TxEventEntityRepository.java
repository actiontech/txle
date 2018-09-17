package org.apache.servicecomb.saga.alpha.server;

import java.util.List;

import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface TxEventEntityRepository extends CrudRepository<TxEvent, Long> {

//	@Query("from SagaUndoLogEntity where globalTxId = ?1")
//	List<TxEvent> findSagaUndoLogEntityByGlobalTxId(@Param("globalTxId") String globalTxId);
//
//	@Query("from SagaUndoLogEntity where globalTxId = ?1 and localTxId = ?2")
//	List<TxEvent> findSagaUndoLogEntityByGlobalTxId(@Param("globalTxId") String globalTxId, @Param("localTxId") String localTxId);

}
