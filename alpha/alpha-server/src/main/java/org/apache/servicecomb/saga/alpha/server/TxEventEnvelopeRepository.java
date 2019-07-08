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

import org.apache.servicecomb.saga.alpha.core.EventScanner;
import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import javax.transaction.Transactional;
import java.util.*;

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

//  @Query("SELECT t FROM TxEvent t "
//      + "WHERE t.type IN ('TxStartedEvent', 'SagaStartedEvent') "
//      + "  AND t.expiryTime < CURRENT_TIMESTAMP AND NOT EXISTS( "
//      + "  SELECT t1.globalTxId FROM TxEvent t1 "
//      + "  WHERE t1.globalTxId = t.globalTxId "
//      + "    AND t1.localTxId = t.localTxId "
//      + "    AND t1.type != t.type"
//      + ")")
//  List<TxEvent> findTimeoutEvents(Pageable pageable);

  @Query(value = "SELECT * FROM TxEvent t WHERE t.surrogateId > ?1 AND t.type IN ('TxStartedEvent', 'SagaStartedEvent') AND t.expiryTime < ?2" +
          " AND NOT EXISTS (SELECT 1 FROM TxEvent t1 WHERE t1.globalTxId = t.globalTxId AND t1.localTxId = t.localTxId AND t1.type != t.type)" +
          // 查询超时事件要去除带有异常的，因为这种情况是未超时先异常了，所以无需再处理
          " AND NOT EXISTS (SELECT 1 FROM TxEvent t2 WHERE t2.globalTxId = t.globalTxId AND t2.type = 'TxAbortedEvent')" + EventScanner.SCANNER_SQL, nativeQuery = true)
  List<TxEvent> findTimeoutEvents(long unendedMinEventId, Date currentDateTime);

  /**
   * 查询某未结束的全局事务中的超时未处理的记录，如果全局事务和子事务都设置了超时，则优先获取子事务的(其实哪个都可以)
   */
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
//  List<TxEvent> findFirstByTypeAndSurrogateIdGreaterThan(String type, long surrogateId, Pageable pageable);

  // TODO select ifnull(min(t.surrogateId), 0) from TxEvent t WHERE NOT EXISTS (select 1 from TxEvent t1 WHERE t1.type = 'SagaEndedEvent' AND t.globalTxId = t1.globalTxId);
  // min默认值不能为0，默认值应为最大surrogateId
  @Query(value = "SELECT * FROM TxEvent t WHERE" +
          // 从未结束(即无【SagaEndedEvent】状态)的最小事件开始查询
          // nextEndedEventId不推荐使用，原因是某事务超时时间较长在定时器检测时并未超时，并且其后续执行了一些带有异常的事务，定时器检测到这些异常事务后该值被更改，然后该超时事务将无法被补偿，因为查询需要补偿的SQL中含id值大于该nextEndedEventId值条件
          " t.surrogateId > (SELECT coalesce(MIN(t5.surrogateId), 0) FROM TxEvent t5 WHERE NOT EXISTS (SELECT 1 FROM TxEvent t6 WHERE t6.type = 'SagaEndedEvent' AND t5.globalTxId = t6.globalTxId AND t6.surrogateId > ?2) AND t5.surrogateId > ?2)" +
//          " t.surrogateId > (SELECT coalesce(MIN(t5.surrogateId), 0) FROM TxEvent t5 WHERE NOT EXISTS (SELECT 1 FROM TxEvent t6 WHERE t6.type = 'SagaEndedEvent' AND t5.globalTxId = t6.globalTxId))" +
          // 发生异常的
//          " AND ((t.type = 'TxStartedEvent' AND t.expiryTime < now()) OR (t.type = ?1 AND EXISTS (SELECT 1 FROM TxEvent t1 WHERE t1.globalTxId = t.globalTxId AND t1.type = 'TxAbortedEvent')))" +
          " AND t.type = ?1 AND EXISTS (SELECT 1 FROM TxEvent t1 WHERE t1.globalTxId = t.globalTxId AND t1.type = 'TxAbortedEvent')" +
          // 排除已补偿的
          " AND NOT EXISTS (SELECT t3.globalTxId FROM TxEvent t3 WHERE t3.globalTxId = t.globalTxId AND t3.localTxId = t.localTxId AND t3.type = 'TxCompensatedEvent')" +
          // 排除重试的
          " AND t.retries = 0" +
//          " AND (SELECT MIN(t4.retries) FROM TxEvent t4 WHERE t4.globalTxId = t.globalTxId AND t4.localTxId = t.localTxId AND t4.type = 'TxStartedEvent') = 0" +
          " ORDER BY t.surrogateId ASC", nativeQuery = true)
  List<TxEvent> findFirstByTypeAndSurrogateIdGreaterThan(String type, long surrogateId);
//  List<TxEvent> findFirstByTypeAndSurrogateIdGreaterThan(String type, Pageable pageable);

  // 全局事务异常时，查询需要补偿的子事务。超时场景同全局事务异常场景。
  // 对于超时场景，仅补偿已完成且未被补偿过的子事务，因为未完成的子事务不确定最终是否会完成，如果最终为完成则会由客户端的本地事务回滚，如果最终成功完成，则是上报TxEndedEvent事件时对其进行补偿
  // ps：语句中的t.globalTxId = t1.globalTxId条件不影响结果，但加此条件可触发saga_globalid_localid_type的联合索引，即不加会扫描无数条，加的话会直接一句联合索引定位到具体的一条
  @Query(value = "FROM TxEvent t WHERE t.globalTxId = ?1 AND t.type = 'TxStartedEvent'" +
          " AND EXISTS (SELECT 1 FROM TxEvent t1 WHERE t.globalTxId = t1.globalTxId AND t1.localTxId = t.localTxId AND t1.type = 'TxEndedEvent')" +
          " AND NOT EXISTS (SELECT 1 FROM TxEvent t2 WHERE t.globalTxId = t2.globalTxId AND t2.localTxId = t.localTxId AND t2.type = 'TxCompensatedEvent')")
  List<TxEvent> findNeedCompensateEventForGlobalTxAborted(String globalTxId);

  // 其实和超时场景检测语句findNeedCompensateEventForTimeout应该是一样的
  // ps：语句中的t.globalTxId = t1.globalTxId条件不影响结果，但加此条件可触发saga_globalid_localid_type的联合索引，即不加会扫描无数条，加的话会直接一句联合索引定位到具体的一条
  @Query(value = "FROM TxEvent t WHERE t.globalTxId = ?1 AND t.localTxId != ?2 AND t.type = 'TxStartedEvent'" +
          " AND NOT EXISTS (SELECT 1 FROM TxEvent t2 WHERE t.globalTxId = t2.globalTxId AND t2.localTxId = t.localTxId AND t2.type = 'TxCompensatedEvent')")
  List<TxEvent> findNeedCompensateEventForException(String globalTxId, String localTxId);

//  @Query(value = "SELECT * FROM TxEvent t WHERE t.globalTxId NOT IN (SELECT t1.globalTxId FROM TxEvent t1 WHERE t1.type = 'SagaEndedEvent') AND (t.type = 'TxCompensatedEvent' or (t.type = 'TxAbortedEvent' AND t.globalTxId != t.localTxId)) ORDER BY surrogateId", nativeQuery = true)
  @Query(value = "SELECT * FROM TxEvent t WHERE t.surrogateId > ?1 AND t.globalTxId NOT IN (SELECT t1.globalTxId FROM TxEvent t1 WHERE t1.type = 'SagaEndedEvent') AND t.type = 'TxCompensatedEvent' ORDER BY surrogateId" + EventScanner.SCANNER_SQL, nativeQuery = true)
  List<TxEvent> findSequentialCompensableEventOfUnended(long unendedMinEventId);

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

  @Query("SELECT T.type FROM TxEvent T WHERE T.globalTxId = ?1")
  List<String> selectAllTypeByGlobalTxId(String globalTxId);

  @Query(value = "SELECT T FROM TxEvent T WHERE T.type IN ('SagaPausedEvent', 'SagaContinuedEvent', 'SagaAutoContinuedEvent') AND T.globalTxId = ?1 ORDER BY T.surrogateId DESC")
  List<TxEvent> selectPausedAndContinueEvent(String globalTxId);

  @Query(value = "SELECT count(1) FROM (SELECT count(1) FROM TxEvent T WHERE T.retries > 0 AND T.globalTxId = ?1 GROUP BY T.localTxId HAVING count(1) > 1) T1", nativeQuery = true)
  long checkIsRetriedEvent(String globalTxId);

  @Query(value = "SELECT DISTINCT T2.localTxId FROM TxEvent T2 WHERE T2.globalTxId IN (SELECT T1.globalTxId FROM TxEvent T1 WHERE T1.type = 'SagaEndedEvent' AND T1.globalTxId IN (SELECT T.globalTxId FROM TxEvent T WHERE T.localTxId IN ?1)) AND T2.localTxId IN ?1")
  Set<String> selectEndedGlobalTx(Set<String> localTxIdSet);

  @Query(value = "SELECT * FROM (SELECT count(1) FROM TxEvent T WHERE T.globalTxId = ?1 AND T.localTxId = ?2 AND T.type = ?3) T1", nativeQuery = true)
  long checkIsExistsTxCompensatedEvent(String globalTxId, String localTxId, String type);

  @Query(value = "SELECT * FROM TxEvent T WHERE T.globalTxId = ?1 AND T.type = 'TxAbortedEvent' LIMIT 1", nativeQuery = true)
  TxEvent selectAbortedTxEvent(String globalTxId);

  @Query(value = "SELECT * FROM (SELECT count(1) FROM TxEvent T WHERE T.globalTxId = ?1 AND T.localTxId = ?2 AND T.type = 'TxStartedEvent' AND T.retries = 0) T1", nativeQuery = true)
  long checkTxIsAborted(String globalTxId, String localTxId);

  @Query("SELECT new org.apache.servicecomb.saga.alpha.core.TxEvent(T.surrogateId, T.globalTxId, T.serviceName, T.instanceId, T.category, T.expiryTime, T.retries, T.creationTime)" +
          " FROM TxEvent T WHERE T.type = 'SagaStartedEvent'")
  List<TxEvent> findTxList(Pageable pageable);

  @Query("FROM TxEvent T WHERE T.globalTxId IN ?1 ")
  List<TxEvent> selectTxEventByGlobalTxIds(List<String> globalTxIdList);

  @Query("SELECT new org.apache.servicecomb.saga.alpha.core.TxEvent(T.surrogateId, T.globalTxId, T.serviceName, T.instanceId, T.category, T.expiryTime, T.retries, T.creationTime)" +
          " FROM TxEvent T WHERE T.type = 'SagaStartedEvent' AND CONCAT(T.globalTxId, T.instanceId, T.category, T.expiryTime, T.retries, T.creationTime) LIKE CONCAT('%', ?1, '%')")
  List<TxEvent> findTxList(Pageable pageable, String searchText);

  @Query("SELECT COUNT(1) FROM TxEvent T WHERE T.type = 'SagaStartedEvent'")
  long findTxListCount();

  @Query("SELECT COUNT(1) FROM TxEvent T WHERE T.type = 'SagaStartedEvent' AND CONCAT(T.globalTxId, T.instanceId, T.category, T.expiryTime, T.retries, T.creationTime) LIKE CONCAT('%', ?1, '%')")
  long findTxListCount(String searchText);

  @Query("SELECT new org.apache.servicecomb.saga.alpha.core.TxEvent(T.surrogateId, T.globalTxId, T.localTxId, T.serviceName, T.instanceId, T.type, T.category, T.expiryTime, T.retries, T.creationTime)" +
          " FROM TxEvent T WHERE T.globalTxId IN ?1 ")
  List<TxEvent> selectSpecialColumnsOfTxEventByGlobalTxIds(List<String> globalTxIdList);

  @Query("FROM TxEvent t WHERE t.surrogateId > ?1 AND t.globalTxId NOT IN (SELECT t1.globalTxId FROM TxEvent t1 WHERE t1.type = 'SagaEndedEvent')")
  List<TxEvent> selectUnendedTxEvents(long unendedMinEventId);

  @Query("SELECT coalesce(min(t.surrogateId), 0) FROM TxEvent t WHERE t.surrogateId > ?1 AND t.globalTxId NOT IN (SELECT t1.globalTxId FROM TxEvent t1 WHERE t1.type = 'SagaEndedEvent')")
  long selectMinUnendedTxEventId(long unendedMinEventId);

}
