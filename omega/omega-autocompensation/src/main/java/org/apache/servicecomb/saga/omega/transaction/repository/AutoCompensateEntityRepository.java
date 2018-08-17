package org.apache.servicecomb.saga.omega.transaction.repository;

import java.util.List;

import javax.transaction.Transactional;

import org.apache.servicecomb.saga.omega.transaction.repository.entity.SagaUndoLogEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * The data interface for auto-compensation.
 * Aim to execute standard SQL.
 * 
 * @author Gannalyo
 * @since 201807-30
 */
public interface AutoCompensateEntityRepository extends CrudRepository<SagaUndoLogEntity, Long> {

	@Query("from SagaUndoLogEntity where globalTxId = ?1")
	List<SagaUndoLogEntity> findSagaUndoLogEntityByGlobalTxId(@Param("globalTxId") String globalTxId);
	
	@Query("from SagaUndoLogEntity where globalTxId = ?1 and localTxId = ?2")
	List<SagaUndoLogEntity> findSagaUndoLogEntityByGlobalTxId(@Param("globalTxId") String globalTxId, @Param("localTxId") String localTxId);

	@Transactional
	@Modifying(clearAutomatically = true)
	@Query("from SagaUndoLogEntity where globalTxId = ?1")
	int executeDynamicSql(@Param("globalTxId") String globalTxId);

}
