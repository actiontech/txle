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

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.apache.servicecomb.saga.alpha.core.TxEventRepository;
import org.springframework.data.domain.PageRequest;

class SpringTxEventRepository implements TxEventRepository {
  private static final PageRequest SINGLE_TX_EVENT_REQUEST = new PageRequest(0, 1);
  private final TxEventEnvelopeRepository eventRepo;

  SpringTxEventRepository(TxEventEnvelopeRepository eventRepo) {
    this.eventRepo = eventRepo;
  }

  @Override
  public void save(TxEvent event) {
    eventRepo.save(event);
  }

  @Override
  public Optional<List<TxEvent>> findFirstAbortedGlobalTransaction() {
    return eventRepo.findFirstAbortedGlobalTxByType();
  }

  @Override
  public List<TxEvent> findTimeoutEvents() {
//    return eventRepo.findTimeoutEvents(SINGLE_TX_EVENT_REQUEST);
    return eventRepo.findTimeoutEvents(new Date());
  }

  @Override
  public TxEvent findTimeoutEventsBeforeEnding(String globalTxId) {
    return eventRepo.findTimeoutEventsBeforeEnding(globalTxId, new Date());
  }

  @Override
  public Optional<TxEvent> findTxStartedEvent(String globalTxId, String localTxId) {
    return eventRepo.findFirstStartedEventByGlobalTxIdAndLocalTxId(globalTxId, localTxId);
  }

  @Override
  public List<TxEvent> findTransactions(String globalTxId, String type) {
    return eventRepo.findByEventGlobalTxIdAndEventType(globalTxId, type);
  }

  @Override
  public List<TxEvent> findFirstUncompensatedEventByIdGreaterThan(long id, String type) {
//    return eventRepo.findFirstByTypeAndSurrogateIdGreaterThan(type, SINGLE_TX_EVENT_REQUEST);
    return eventRepo.findFirstByTypeAndSurrogateIdGreaterThan(type, id);
  }

  @Override
  public List<TxEvent> findSequentialCompensableEventOfUnended() {
    return eventRepo.findSequentialCompensableEventOfUnended();
  }

  @Override
  public void deleteDuplicateEvents(String type) {
    eventRepo.deleteByType(type);
  }

  @Override
  public void deleteDuplicateEventsByTypeAndSurrogateIds(String type, List<Long> maxSurrogateIdList) {
    eventRepo.deleteDuplicateEventsByTypeAndSurrogateIds(type, maxSurrogateIdList);
  }

  @Override
  public List<Long> getMaxSurrogateIdGroupByGlobalTxIdByType(String type) {
    return eventRepo.getMaxSurrogateIdGroupByGlobalTxIdByType(type);
  }

  @Override
  public Iterable<TxEvent> findAll() {
    return eventRepo.findAll();
  }

  @Override
	public TxEvent findOne(long id) {
		return eventRepo.findOne(id);
	}

	@Override
	public List<TxEvent> selectPausedAndContinueEvent(String globalTxId) {
		return eventRepo.selectPausedAndContinueEvent(globalTxId);
	}

  @Override
  public long count() {
    return eventRepo.count();
  }

  @Override
  public boolean checkIsRetriedEvent(String globalTxId) {
    return eventRepo.checkIsRetriedEvent(globalTxId) > 0;
  }

  @Override
  public Set<String> selectEndedGlobalTx(Set<String> localTxIdSet) {
    return eventRepo.selectEndedGlobalTx(localTxIdSet);
  }

  @Override
  public boolean checkIsExistsTxCompensatedEvent(String globalTxId, String localTxId, String type) {
    return eventRepo.checkIsExistsTxCompensatedEvent(globalTxId, localTxId, type) > 0;
  }

}
