/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.server;

import org.apache.servicecomb.saga.alpha.core.EventScanner;
import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

interface TxEventEnvelopeRepository extends CrudRepository<TxEvent, Long> {
  List<TxEvent> findByGlobalTxId(String globalTxId);

  @Query(value = "SELECT * FROM TxEvent t WHERE t.surrogateId > ?1 AND t.type IN ('TxStartedEvent', 'SagaStartedEvent') AND t.expiryTime < ?2" +
          " AND NOT EXISTS (SELECT 1 FROM TxEvent t1 WHERE t1.globalTxId = t.globalTxId AND t1.localTxId = t.localTxId AND t1.type != t.type)" +
          " AND NOT EXISTS (SELECT 1 FROM TxEvent t2 WHERE t2.globalTxId = t.globalTxId AND t2.type = 'TxAbortedEvent')" + EventScanner.SCANNER_SQL, nativeQuery = true)
  List<TxEvent> findTimeoutEvents(long unendedMinEventId, Date currentDateTime);

  @Query(value = "SELECT * FROM TxEvent t WHERE t.globalTxId = ?1 AND t.type IN ('TxStartedEvent', 'SagaStartedEvent') AND t.expiryTime < ?2" +
          " AND NOT EXISTS (SELECT 1 FROM TxEvent WHERE globalTxId = ?1 AND type = 'TxAbortedEvent')" +
          " AND NOT EXISTS (SELECT 1 FROM TxEvent WHERE globalTxId = ?1 AND type = 'SagaEndedEvent')" +
          " ORDER BY surrogateId DESC LIMIT 1", nativeQuery = true)
  TxEvent findTimeoutEventsBeforeEnding(String globalTxId, Date currentDateTime);

  @Query("SELECT t FROM TxEvent t "
      + "WHERE t.globalTxId = ?1 "
      + "  AND t.localTxId = ?2 "
      + "  AND t.type = 'TxStartedEvent'")
  Optional<TxEvent> findFirstStartedEventByGlobalTxIdAndLocalTxId(String globalTxId, String localTxId);

  @Query("FROM TxEvent t WHERE t.globalTxId = ?1 AND t.localTxId = ?2 AND t.type = 'TxStartedEvent'")
  List<TxEvent> selectTxStartedEventByLocalTxId(String globalTxId, String localTxId);

  @Query("SELECT t FROM TxEvent t "
      + "WHERE t.globalTxId = ?1 AND t.type = 'TxStartedEvent' AND EXISTS ( "
      + "  SELECT t1.globalTxId"
      + "  FROM TxEvent t1 "
      + "  WHERE t1.globalTxId = ?1 "
      + "  AND t1.localTxId = t.localTxId "
      + "  AND t1.type = 'TxEndedEvent'"
      + ") AND NOT EXISTS ( "
      + "  SELECT t2.globalTxId"
      + "  FROM TxEvent t2 "
      + "  WHERE t2.globalTxId = ?1 "
      + "  AND t2.localTxId = t.localTxId "
      + "  AND t2.type = 'TxCompensatedEvent') "
      + "ORDER BY t.surrogateId ASC")
  List<TxEvent> findDoneAndUncompensatedSubTx(String globalTxId);

  // Find sub-transactions which need to be compensated in case of global transaction is aborted.
  // In the timeout case, just compensate the done sub-transactions. Because it could not be sure if the undone sub-transactions would complete finally.
  // If the undone sub-transaction will complete successfully, then it will be compensated immediately after saving 'TxEndedEvent', opposite, it'll be rolled back in a local transaction.
  // ps: The condition 't.globalTxId = t1.globalTxId' will not produce influence to the result. But it can improve the SQL performance due to it can trigger the index 'saga_globalid_localid_type'.
  @Query(value = "FROM TxEvent t WHERE t.globalTxId = ?1 AND t.type = 'TxStartedEvent'" +
          " AND EXISTS (SELECT 1 FROM TxEvent t1 WHERE t.globalTxId = t1.globalTxId AND t1.localTxId = t.localTxId AND t1.type = 'TxEndedEvent')" +
          " AND NOT EXISTS (SELECT 1 FROM TxEvent t2 WHERE t.globalTxId = t2.globalTxId AND t2.localTxId = t.localTxId AND t2.type = 'TxCompensatedEvent')")
  List<TxEvent> findNeedCompensateEventForGlobalTxAborted(String globalTxId);

  @Query(value = "FROM TxEvent t WHERE t.globalTxId = ?1 AND t.localTxId != ?2 AND t.type = 'TxStartedEvent'" +
          " AND NOT EXISTS (SELECT 1 FROM TxEvent t2 WHERE t.globalTxId = t2.globalTxId AND t2.localTxId = t.localTxId AND t2.type = 'TxCompensatedEvent')")
  List<TxEvent> findNeedCompensateEventForException(String globalTxId, String localTxId);

  @Query(value = "SELECT * FROM TxEvent t WHERE t.surrogateId > ?1 AND t.globalTxId NOT IN (SELECT t1.globalTxId FROM TxEvent t1 WHERE t1.type = 'SagaEndedEvent') AND t.type = 'TxCompensatedEvent' ORDER BY surrogateId" + EventScanner.SCANNER_SQL, nativeQuery = true)
  List<TxEvent> findSequentialCompensableEventOfUnended(long unendedMinEventId);

  @Query("SELECT T.type FROM TxEvent T WHERE T.globalTxId = ?1")
  List<String> selectAllTypeByGlobalTxId(String globalTxId);

  @Query(value = "SELECT T FROM TxEvent T WHERE T.type IN ('SagaPausedEvent', 'SagaContinuedEvent', 'SagaAutoContinuedEvent') AND T.globalTxId = ?1 ORDER BY T.surrogateId DESC")
  List<TxEvent> selectPausedAndContinueEvent(String globalTxId);

  @Query(value = "SELECT DISTINCT T2.localTxId FROM TxEvent T2 WHERE T2.globalTxId IN (SELECT T1.globalTxId FROM TxEvent T1 WHERE T1.type = 'SagaEndedEvent' AND T1.globalTxId IN (SELECT T.globalTxId FROM TxEvent T WHERE T.localTxId IN ?1)) AND T2.localTxId IN ?1")
  Set<String> selectEndedGlobalTx(Set<String> localTxIdSet);

  @Query(value = "SELECT * FROM (SELECT count(1) FROM TxEvent T WHERE T.globalTxId = ?1 AND T.localTxId = ?2 AND T.type = ?3) T1", nativeQuery = true)
  long checkIsExistsEventType(String globalTxId, String localTxId, String type);

  @Query(value = "SELECT * FROM (SELECT count(1) FROM TxEvent T WHERE T.globalTxId = ?1 AND T.localTxId = ?2 AND T.type = 'TxStartedEvent' AND T.retries = 0) T1", nativeQuery = true)
  long checkTxIsAborted(String globalTxId, String localTxId);

  @Query("SELECT new org.apache.servicecomb.saga.alpha.core.TxEvent(T.surrogateId, T.globalTxId, T.serviceName, T.instanceId, T.category, T.expiryTime, T.retries, T.creationTime)" +
          " FROM TxEvent T WHERE T.type = 'SagaStartedEvent'")
  List<TxEvent> findTxList(Pageable pageable);

  @Query("FROM TxEvent T WHERE T.globalTxId IN ?1 ")
  List<TxEvent> selectTxEventByGlobalTxIds(List<String> globalTxIdList);

  @Query("SELECT new org.apache.servicecomb.saga.alpha.core.TxEvent(T.surrogateId, T.globalTxId, T.serviceName, T.instanceId, T.category, T.expiryTime, T.retries, T.creationTime)" +
          " FROM TxEvent T WHERE T.type = 'SagaStartedEvent' AND FUNCTION('CONCAT_WS', ',', T.globalTxId, T.instanceId, T.category, T.expiryTime, T.retries, T.creationTime) LIKE CONCAT('%', ?1, '%')")
  List<TxEvent> findTxList(Pageable pageable, String searchText);

  @Query("SELECT COUNT(1) FROM TxEvent T WHERE T.type = 'SagaStartedEvent'")
  long findTxListCount();

  @Query("SELECT COUNT(1) FROM TxEvent T WHERE T.type = 'SagaStartedEvent' AND FUNCTION('CONCAT_WS', ',', T.globalTxId, T.instanceId, T.category, T.expiryTime, T.retries, T.creationTime) LIKE CONCAT('%', ?1, '%')")
  long findTxListCount(String searchText);

  @Query("SELECT new org.apache.servicecomb.saga.alpha.core.TxEvent(T.surrogateId, T.globalTxId, T.localTxId, T.serviceName, T.instanceId, T.type, T.category, T.expiryTime, T.retries, T.creationTime)" +
          " FROM TxEvent T WHERE T.globalTxId IN ?1 ")
  List<TxEvent> selectSpecialColumnsOfTxEventByGlobalTxIds(List<String> globalTxIdList);

  @Query("FROM TxEvent t WHERE t.surrogateId > ?1 AND t.globalTxId NOT IN (SELECT t1.globalTxId FROM TxEvent t1 WHERE t1.type = 'SagaEndedEvent')")
  List<TxEvent> selectUnendedTxEvents(long unendedMinEventId);

  @Query("SELECT coalesce(min(t.surrogateId), 0) FROM TxEvent t WHERE t.surrogateId > ?1 AND t.globalTxId NOT IN (SELECT t1.globalTxId FROM TxEvent t1 WHERE t1.surrogateId > ?1 AND t1.type = 'SagaEndedEvent')")
  long selectMinUnendedTxEventId(long unendedMinEventId);

  @Query(value = "SELECT min(creationTime) FROM TxEvent", nativeQuery = true)
  Date selectMinDateInTxEvent();

  @Query("SELECT T.surrogateId FROM TxEvent T WHERE T.creationTime BETWEEN ?1 AND ?2 AND EXISTS (SELECT 1 FROM TxEvent T1 WHERE T1.type = 'SagaEndedEvent' AND FUNCTION('TO_DAYS', CURRENT_TIMESTAMP) - FUNCTION('TO_DAYS', T1.creationTime) > 10 AND T.globalTxId = T1.globalTxId)")
  List<Long> selectEndedEventIdsWithinSomePeriod(Pageable pageable, Date startTime, Date endTime);

  @Query("FROM TxEvent T WHERE T.globalTxId = ?1 AND T.type = ?2")
  TxEvent selectEventByGlobalTxIdType(String globalTxId, String type);

  @Query(value = "SELECT * FROM (SELECT count(1) FROM TxEvent T WHERE T.globalTxId = ?1 AND T.type = 'TxStartedEvent') T1", nativeQuery = true)
  long selectSubTxCount(String globalTxId);

}
