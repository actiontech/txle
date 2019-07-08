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

import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.apache.servicecomb.saga.alpha.core.TxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

class SpringTxEventRepository implements TxEventRepository {
  private static final Logger LOG = LoggerFactory.getLogger(SpringTxEventRepository.class);

  private final TxEventEnvelopeRepository eventRepo;

  SpringTxEventRepository(TxEventEnvelopeRepository eventRepo) {
    this.eventRepo = eventRepo;
  }

  @Override
  public void save(TxEvent event) {
    TxEvent saveEvent = eventRepo.save(event);
    if (saveEvent != null) {
      event.setSurrogateId(saveEvent.id());
    }
  }

  @Override
  public Optional<List<TxEvent>> findFirstAbortedGlobalTransaction() {
    return eventRepo.findFirstAbortedGlobalTxByType();
  }

  @Override
  public List<TxEvent> findTimeoutEvents(long unendedMinEventId) {
//    return eventRepo.findTimeoutEvents(SINGLE_TX_EVENT_REQUEST);
    return eventRepo.findTimeoutEvents(unendedMinEventId, new Date());
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
  public List<TxEvent> findSequentialCompensableEventOfUnended(long unendedMinEventId) {
    return eventRepo.findSequentialCompensableEventOfUnended(unendedMinEventId);
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
  public List<String> selectAllTypeByGlobalTxId(String globalTxId) {
    return eventRepo.selectAllTypeByGlobalTxId(globalTxId);
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

  @Override
  public TxEvent selectAbortedTxEvent(String globalTxId) {
    return eventRepo.selectAbortedTxEvent(globalTxId);
  }

  @Override
  public boolean checkTxIsAborted(String globalTxId, String localTxId) {
    return eventRepo.checkTxIsAborted(globalTxId, localTxId) > 0;
  }

  @Override
  public List<TxEvent> findTxList(int pageIndex, int pageSize, String orderName, String direction, String searchText) {
    // TODO 检测是否有非数字，如果有非数字则过滤掉数字类型字段
    // TODO 检测如果是字符“-”，则视为无searchText处理，因为每一行的日期都含有“-”，或者是当已完成的查询
    try {
      pageIndex = pageIndex < 1 ? 0 : pageIndex;
      pageSize = pageSize < 1 ? 100 : pageSize;

      Sort.Direction sd = Sort.Direction.DESC;
      if (orderName == null || orderName.length() == 0) {
        orderName = "creationTime";
      }
      if ("asc".equalsIgnoreCase(direction)) {
        sd = Sort.Direction.ASC;
      }

      PageRequest pageRequest = new PageRequest(pageIndex, pageSize, sd, orderName);
      if (searchText == null || searchText.length() == 0) {
        return eventRepo.findTxList(pageRequest);
      }
      return eventRepo.findTxList(pageRequest, searchText);
    } catch (Exception e) {
      LOG.error("Failed to find the list of Global Transaction. params {pageIndex: [{}], pageSize: [{}], orderName: [{}], direction: [{}], searchText: [{}]}.", pageIndex, pageSize, orderName, direction, searchText, e);
    }
    return null;
  }

  @Override
  public List<TxEvent> selectTxEventByGlobalTxIds(List<String> globalTxIdList) {
    return eventRepo.selectTxEventByGlobalTxIds(globalTxIdList);
  }

  @Override
  public long findTxListCount(String searchText) {
    if (searchText == null || searchText.length() == 0) {
      return eventRepo.findTxListCount();
    }
    return eventRepo.findTxListCount(searchText);
  }

  @Override
  public List<TxEvent> selectSpecialColumnsOfTxEventByGlobalTxIds(List<String> globalTxIdList) {
    return eventRepo.selectSpecialColumnsOfTxEventByGlobalTxIds(globalTxIdList);
  }

  @Override
  public List<TxEvent> selectUnendedTxEvents(long unendedMinEventId) {
    return eventRepo.selectUnendedTxEvents(unendedMinEventId);
  }

  @Override
  public long selectMinUnendedTxEventId(long unendedMinEventId) {
    return eventRepo.selectMinUnendedTxEventId(unendedMinEventId);
  }

}
