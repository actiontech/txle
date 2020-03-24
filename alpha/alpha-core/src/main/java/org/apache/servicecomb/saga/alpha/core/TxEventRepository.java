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

package org.apache.servicecomb.saga.alpha.core;

import org.apache.servicecomb.saga.common.EventType;

import java.util.*;

/**
 * Repository for {@link TxEvent}
 */
public interface TxEventRepository {

  /**
   * Save a {@link TxEvent}.
   *
   * @param event for global/sub transaction
   */
  void save(TxEvent event);

  /**
   * Find timeout {@link TxEvent}s. A timeout TxEvent satisfies below requirements:
   *
   * <ol>
   *  <li>{@link TxEvent#type} is {@link EventType#TxStartedEvent} or {@link EventType#SagaStartedEvent}</li>
   *  <li>Current time greater than {@link TxEvent#expiryTime}</li>
   *  <li>There are no corresponding {@link TxEvent} which type is <code>TxEndedEvent</code> or <code>SagaEndedEvent</code></li>
   * </ol>
   *
   * @param unendedMinEventId the min identify of undone event
   * @return event list
   */
  List<TxEvent> findTimeoutEvents(long unendedMinEventId);

  TxEvent findTimeoutEventsBeforeEnding(String globalTxId);

  List<TxEvent> findTimeoutEvents(List<String> globalTxId);

  /**
   * Find a {@link TxEvent} which satisfies below requirements:
   * <ol>
   *   <li>{@link TxEvent#type} is {@link EventType#TxStartedEvent}</li>
   *   <li>{@link TxEvent#globalTxId} equals to param <code>globalTxId</code></li>
   *   <li>{@link TxEvent#localTxId} equals to param <code>localTxId</code></li>
   * </ol>
   *
   * @param globalTxId global transaction identify
   * @param localTxId sub-transaction identify
   * @return {@link TxEvent}
   */
  Optional<TxEvent> findTxStartedEvent(String globalTxId, String localTxId);

  List<TxEvent> findSequentialCompensableEventOfUnended(long unendedMinEventId);

  List<String> selectAllTypeByGlobalTxId(String globalTxId);

  List<TxEvent> selectPausedAndContinueEvent(String globalTxId);

  Set<String> selectEndedGlobalTx(Set<String> localTxIdSet);

  boolean checkIsExistsEventType(String globalTxId, String localTxId, String type);

  boolean checkTxIsAborted(String globalTxId, String localTxId);

  List<Map<String, Object>> findTxList(int pageIndex, int pageSize, String orderName, String direction, String searchText);

  List<TxEvent> selectTxEventByGlobalTxIds(List<String> globalTxIdList);

  long findTxCount(String searchText);

  List<Map<String, Object>> findSubTxList(String globalTxIds);

  List<TxEvent> selectUnendedTxEvents(long unendedMinEventId);

  long selectMinUnendedTxEventId(long unendedMinEventId);

  Date selectMinDateInTxEvent();

  List<Long> selectEndedEventIdsWithinSomePeriod(int pageIndex, int pageSize, Date startTime, Date endTime);

  TxEvent selectEventByGlobalTxIdType(String globalTxId, String type);

  TxEvent selectMinRetriesEventByTxIdType(String globalTxId, String localTxId, String type);

  boolean checkIsAlreadyRetried(String globalTxId, String localTxId);

}
