/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.servicecomb.saga.alpha.server;

import java.util.*;

import javax.transaction.Transactional;

import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

interface TxEventEnvelopeRepository extends CrudRepository<TxEvent, Long> {
  List<TxEvent> findByGlobalTxId(String globalTxId);

  @Query("SELECT t FROM TxEvent t "
      + "WHERE t.type = 'TxAbortedEvent' AND NOT EXISTS( "
      + "  SELECT t1.globalTxId FROM TxEvent t1"
      + "  WHERE t1.globalTxId = t.globalTxId "
      + "    AND t1.type IN ('TxEndedEvent', 'SagaEndedEvent')) AND NOT EXISTS ( "
      + "  SELECT t3.globalTxId FROM TxEvent t3 "
      + "  WHERE t3.globalTxId = t.globalTxId "
      + "    AND t3.localTxId = t.localTxId "
      + "    AND t3.surrogateId != t.surrogateId "
      + "    AND t3.creationTime > t.creationTime) AND (("
      + "SELECT MIN(t2.retries) FROM TxEvent t2 "
      + "WHERE t2.globalTxId = t.globalTxId "
      + "  AND t2.localTxId = t.localTxId "
      + "  AND t2.type = 'TxStartedEvent') = 0 "
      + "OR t.globalTxId = t.localTxId)")
  Optional<List<TxEvent>> findFirstAbortedGlobalTxByType();

  @Query("SELECT t FROM TxEvent t "
      + "WHERE t.type IN ('TxStartedEvent', 'SagaStartedEvent') "
      + "  AND t.expiryTime < CURRENT_TIMESTAMP AND NOT EXISTS( "
      + "  SELECT t1.globalTxId FROM TxEvent t1 "
      + "  WHERE t1.globalTxId = t.globalTxId "
      + "    AND t1.localTxId = t.localTxId "
      + "    AND t1.type != t.type"
      + ")")
  List<TxEvent> findTimeoutEvents(Pageable pageable);

  @Query("SELECT t FROM TxEvent t "
      + "WHERE t.type IN ('TxStartedEvent', 'SagaStartedEvent') "
      + "  AND t.expiryTime < ?1 AND NOT EXISTS( "
      + "  SELECT t1.globalTxId FROM TxEvent t1 "
      + "  WHERE t1.globalTxId = t.globalTxId "
      + "    AND t1.localTxId = t.localTxId "
      + "    AND t1.type != t.type"
      + ")")
  List<TxEvent> findTimeoutEvents(Pageable pageable, Date currentDateTime);

  @Query("SELECT t FROM TxEvent t "
      + "WHERE t.globalTxId = ?1 "
      + "  AND t.localTxId = ?2 "
      + "  AND t.type = 'TxStartedEvent'")
  Optional<TxEvent> findFirstStartedEventByGlobalTxIdAndLocalTxId(String globalTxId, String localTxId);

  @Query("SELECT DISTINCT new org.apache.servicecomb.saga.alpha.core.TxEvent("
      + "t.serviceName, t.instanceId, t.globalTxId, t.localTxId, t.parentTxId, "
      + "t.type, t.compensationMethod, t.category, t.payloads "
      + ") FROM TxEvent t "
      + "WHERE t.globalTxId = ?1 AND t.type = ?2 "
      + "  AND ( SELECT MIN(t1.retries) FROM TxEvent t1 "
      + "  WHERE t1.globalTxId = t.globalTxId "
      + "    AND t1.localTxId = t.localTxId "
      + "    AND t1.type IN ('TxStartedEvent', 'SagaStartedEvent') ) = 0 ")
  List<TxEvent> findByEventGlobalTxIdAndEventType(String globalTxId, String type);

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
  List<TxEvent> findStartedEventsWithMatchingEndedButNotCompensatedEvents(String globalTxId);

//  @Query("SELECT t FROM TxEvent t "
//      + "WHERE t.type = ?1 AND t.surrogateId > ?2 AND EXISTS ( "
//      + "  SELECT t1.globalTxId FROM TxEvent t1 "
//      + "  WHERE t1.globalTxId = t.globalTxId "
//      + "    AND t1.type = 'TxAbortedEvent' AND NOT EXISTS ( "
//      + "    SELECT t2.globalTxId FROM TxEvent t2 "
//      + "    WHERE t2.globalTxId = t1.globalTxId "
//      + "      AND t2.localTxId = t1.localTxId "
//      + "      AND t2.type = 'TxStartedEvent' "
//      + "      AND t2.creationTime > t1.creationTime)) AND NOT EXISTS ( "
//      + "  SELECT t3.globalTxId FROM TxEvent t3 "
//      + "  WHERE t3.globalTxId = t.globalTxId "
//      + "    AND t3.localTxId = t.localTxId "
//      + "    AND t3.type = 'TxCompensatedEvent') AND ( "
//      + "  SELECT MIN(t4.retries) FROM TxEvent t4 "
//      + "  WHERE t4.globalTxId = t.globalTxId "
//      + "    AND t4.localTxId = t.localTxId "
//      + "    AND t4.type = 'TxStartedEvent' ) = 0"
////      + "    AND t4.type = 'TxStartedEvent' ) = 0 AND NOT EXISTS (SELECT t5.globalTxId FROM TxEvent t5 WHERE t5.globalTxId = t.globalTxId AND t5.type = 'SagaEndedEvent')"
//      + " ORDER BY t.surrogateId ASC")
  List<TxEvent> findFirstByTypeAndSurrogateIdGreaterThan(String type, long surrogateId, Pageable pageable);

  @Query("SELECT t FROM TxEvent t WHERE t.type = ?1" +
          // 大于未完成的全局事务的最小id值
          " AND t.surrogateId > (select min(t5.surrogateId) from TxEvent t5 WHERE NOT EXISTS (select 1 from TxEvent t6 WHERE t6.type = 'SagaEndedEvent' AND t5.globalTxId = t6.globalTxId))" +
          // 发生异常的
          " AND EXISTS (SELECT t1.globalTxId FROM TxEvent t1 WHERE t1.globalTxId = t.globalTxId AND t1.type = 'TxAbortedEvent')" +
          // 排除已补偿的
          " AND NOT EXISTS (SELECT t3.globalTxId FROM TxEvent t3 WHERE t3.globalTxId = t.globalTxId AND t3.localTxId = t.localTxId AND t3.type = 'TxCompensatedEvent')" +
          // 排除重试的
          " AND (SELECT MIN(t4.retries) FROM TxEvent t4 WHERE t4.globalTxId = t.globalTxId AND t4.localTxId = t.localTxId AND t4.type = 'TxStartedEvent') = 0" +
          " ORDER BY t.surrogateId ASC")
  List<TxEvent> findFirstByTypeAndSurrogateIdGreaterThan(String type, Pageable pageable);

//  @Query(value = "SELECT * FROM TxEvent t WHERE t.globalTxId NOT IN (SELECT t1.globalTxId FROM TxEvent t1 WHERE t1.type = 'SagaEndedEvent') AND (t.type = 'TxCompensatedEvent' or (t.type = 'TxAbortedEvent' AND t.globalTxId != t.localTxId)) ORDER BY surrogateId", nativeQuery = true)
  @Query(value = "SELECT * FROM TxEvent t WHERE t.globalTxId NOT IN (SELECT t1.globalTxId FROM TxEvent t1 WHERE t1.type = 'SagaEndedEvent') AND t.type = 'TxCompensatedEvent' ORDER BY surrogateId", nativeQuery = true)
  List<TxEvent> findSequentialCompensableEventOfUnended();

  @Transactional
  @Modifying(clearAutomatically = true)
  @Query("DELETE FROM TxEvent t "
      + "WHERE t.type = ?1 AND t.surrogateId NOT IN ("
      + " SELECT MAX(t1.surrogateId) FROM TxEvent t1 "
      + " WHERE t1.type = ?1 "
      + " GROUP BY t1.globalTxId"
      + ")")
  void deleteByType(String type);

  @Transactional
  @Modifying(clearAutomatically = true)
  @Query("DELETE FROM TxEvent t WHERE t.type = ?1 AND t.surrogateId IN ?2")
  void deleteDuplicateEventsByTypeAndSurrogateIds(String type, List<Long> maxSurrogateIdList);

  @Query("SELECT t.surrogateId FROM TxEvent t" +
          " WHERE t.surrogateId NOT IN (SELECT MAX(t1.surrogateId) FROM TxEvent t1 WHERE t1.type = ?1 GROUP BY t1.globalTxId HAVING COUNT(1) > 1)" +
          " AND t.globalTxId IN (SELECT t1.globalTxId FROM TxEvent t1 WHERE t1.type = ?1 GROUP BY t1.globalTxId HAVING COUNT(1) > 1)" +
          " AND t.type = ?1")
  List<Long> getMaxSurrogateIdGroupByGlobalTxIdByType(String type);

  @Query(value = "SELECT T FROM TxEvent T WHERE T.type IN ('SagaPausedEvent', 'SagaContinuedEvent', 'SagaAutoContinuedEvent') AND T.globalTxId = ?1 ORDER BY T.surrogateId DESC")
  List<TxEvent> selectPausedAndContinueEvent(String globalTxId);

  @Query(value = "SELECT count(1) FROM (SELECT count(1) FROM TxEvent T WHERE T.retries > 0 AND T.globalTxId = ?1 GROUP BY T.localTxId HAVING count(1) > 1) T1", nativeQuery = true)
  long checkIsRetriedEvent(String globalTxId);

  @Query(value = "SELECT DISTINCT T2.localTxId FROM TxEvent T2 WHERE T2.globalTxId IN (SELECT T1.globalTxId FROM TxEvent T1 WHERE T1.type = 'SagaEndedEvent' AND T1.globalTxId IN (SELECT T.globalTxId FROM TxEvent T WHERE T.localTxId IN ?1)) AND T2.localTxId IN ?1")
  Set<String> selectEndedGlobalTx(Set<String> localTxIdSet);

  @Query(value = "SELECT * FROM (SELECT count(1) FROM TxEvent T WHERE T.type = ?1 AND T.localTxId = ?2) T1", nativeQuery = true)
  long checkIsExistsTxCompensatedEvent(String type, String localTxId);

}
