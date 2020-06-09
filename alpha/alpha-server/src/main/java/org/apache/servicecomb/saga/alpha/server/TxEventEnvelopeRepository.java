/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
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
          // 查询超时事件要去除带有异常的，因为这种情况是未超时先异常了，所以无需再处理
          " AND NOT EXISTS (SELECT 1 FROM TxEvent t2 WHERE t2.globalTxId = t.globalTxId AND t2.type = 'TxAbortedEvent')" + EventScanner.SCANNER_SQL, nativeQuery = true)
  List<TxEvent> findTimeoutEvents(long unendedMinEventId, Date currentDateTime);

  // 查询某未结束的全局事务中的超时未处理的记录，如果全局事务和子事务都设置了超时，则优先获取子事务的(其实哪个都可以)
  @Query(value = "SELECT * FROM TxEvent t WHERE t.globalTxId = ?1 AND t.type IN ('TxStartedEvent', 'SagaStartedEvent') AND t.expiryTime < ?2" +
          " AND NOT EXISTS (SELECT 1 FROM TxEvent WHERE globalTxId = ?1 AND type = 'TxAbortedEvent')" +
          " AND NOT EXISTS (SELECT 1 FROM TxEvent WHERE globalTxId = ?1 AND type = 'SagaEndedEvent')" +
          " ORDER BY surrogateId DESC LIMIT 1", nativeQuery = true)
  TxEvent findTimeoutEventsBeforeEnding(String globalTxId, Date currentDateTime);

  @Query(value = "SELECT * FROM TxEvent t WHERE t.globalTxId IN ?1 AND t.type IN ('TxStartedEvent', 'SagaStartedEvent') AND t.expiryTime < ?2" +
          " AND NOT EXISTS (SELECT 1 FROM TxEvent WHERE globalTxId IN ?1 AND type = 'SagaEndedEvent')", nativeQuery = true)
  List<TxEvent> findTimeoutEvents(List<String> globalTxId, Date currentDateTime);

  @Query("SELECT t FROM TxEvent t "
      + "WHERE t.globalTxId = ?1 "
      + "  AND t.localTxId = ?2 "
      + "  AND t.type = 'TxStartedEvent'")
  Optional<TxEvent> findFirstStartedEventByGlobalTxIdAndLocalTxId(String globalTxId, String localTxId);

  @Query("FROM TxEvent t WHERE t.globalTxId = ?1 AND t.localTxId = ?2 AND t.type = 'TxStartedEvent'")
  List<TxEvent> selectTxStartedEventByLocalTxId(String globalTxId, String localTxId);

  @Query("FROM TxEvent t WHERE t.globalTxId = ?1 AND t.localTxId = ?2")
  List<TxEvent> selectSubEventByLocalTxId(String globalTxId, String localTxId);

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

  @Query(value = "SELECT * FROM TxEvent t WHERE t.surrogateId > ?1 AND t.globalTxId NOT IN (SELECT t1.globalTxId FROM TxEvent t1 WHERE t1.type = 'SagaEndedEvent') AND t.type = 'TxCompensatedEvent' ORDER BY surrogateId" + EventScanner.SCANNER_SQL, nativeQuery = true)
  List<TxEvent> findSequentialCompensableEventOfUnended(long unendedMinEventId);

  @Query("SELECT T.type FROM TxEvent T WHERE T.globalTxId = ?1")
  List<String> selectAllTypeByGlobalTxId(String globalTxId);

  @Query(value = "SELECT T FROM TxEvent T WHERE T.type IN ('SagaPausedEvent', 'SagaContinuedEvent', 'SagaAutoContinuedEvent') AND T.globalTxId = ?1 ORDER BY T.surrogateId DESC")
  List<TxEvent> selectPausedAndContinueEvent(String globalTxId);

  @Query(value = "SELECT DISTINCT T2.localTxId FROM TxEvent T2 WHERE T2.globalTxId IN (SELECT T1.globalTxId FROM TxEvent T1 WHERE T1.type = 'SagaEndedEvent' AND T1.globalTxId IN (SELECT T.globalTxId FROM TxEvent T WHERE T.localTxId IN ?1)) AND T2.localTxId IN ?1")
  Set<String> selectEndedGlobalTx(Set<String> localTxIdSet);

  @Query(value = "SELECT * FROM (SELECT count(T) FROM TxEvent T WHERE T.globalTxId = ?1 AND T.localTxId = ?2 AND T.type = ?3) T1", nativeQuery = true)
  long checkIsExistsEventType(String globalTxId, String localTxId, String type);

  @Query(value = "SELECT * FROM (SELECT count(T) FROM TxEvent T WHERE T.globalTxId = ?1 AND T.localTxId = ?2 AND T.type = 'TxStartedEvent' AND T.retries = 0) T1", nativeQuery = true)
  long checkTxIsAborted(String globalTxId, String localTxId);

  @Query("SELECT new org.apache.servicecomb.saga.alpha.core.TxEvent(T.surrogateId, T.globalTxId, T.serviceName, T.instanceId, T.category, T.expiryTime, T.retries, T.creationTime)" +
          " FROM TxEvent T WHERE T.type = 'SagaStartedEvent'")
  List<TxEvent> findTxList(Pageable pageable);

  @Query("FROM TxEvent T WHERE T.globalTxId IN ?1 ")
  List<TxEvent> selectTxEventByGlobalTxIds(List<String> globalTxIdList);

  // FUNCTION('CONCAT_WS', ',', field1, field2...   以逗号分割，并支持字段值为null，当字段值为null时会视为空字符串
  @Query("SELECT new org.apache.servicecomb.saga.alpha.core.TxEvent(T.surrogateId, T.globalTxId, T.serviceName, T.instanceId, T.category, T.expiryTime, T.retries, T.creationTime)" +
          " FROM TxEvent T WHERE T.type = 'SagaStartedEvent' AND FUNCTION('CONCAT_WS', ',', T.globalTxId, T.instanceId, T.category, T.expiryTime, T.retries, T.creationTime) LIKE CONCAT('%', ?1, '%')")
  List<TxEvent> findTxList(Pageable pageable, String searchText);

  @Query("SELECT count(T) FROM TxEvent T WHERE T.type = 'SagaStartedEvent'")
  long findTxListCount();

  @Query("SELECT count(T) FROM TxEvent T WHERE T.type = 'SagaStartedEvent' AND FUNCTION('CONCAT_WS', ',', T.globalTxId, T.instanceId, T.category, T.expiryTime, T.retries, T.creationTime) LIKE CONCAT('%', ?1, '%')")
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

  @Query(value = "SELECT * FROM TxEvent T WHERE T.globalTxId = ?1 AND T.localTxId = ?2 AND T.type = ?3 ORDER BY T.retries LIMIT 1", nativeQuery = true)
  TxEvent selectMinRetriesEventByTxIdType(String globalTxId, String localTxId, String type);

  @Query(value = "SELECT count(T) % 2 FROM TxEvent T WHERE T.globalTxId = ?1 AND T.localTxId = ?2", nativeQuery = true)
  int selectStartedAndAbortedEndRate(String globalTxId, String localTxId);

}
