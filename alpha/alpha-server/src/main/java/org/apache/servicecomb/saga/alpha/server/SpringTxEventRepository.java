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
 *
 *
 *  Copyright (c) 2018-2019 ActionTech.
 *  License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.server;

import org.apache.servicecomb.saga.alpha.core.AdditionalEventType;
import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.apache.servicecomb.saga.alpha.core.TxEventRepository;
import org.apache.servicecomb.saga.alpha.core.datadictionary.DataDictionaryItem;
import org.apache.servicecomb.saga.alpha.core.datadictionary.IDataDictionaryService;
import org.apache.servicecomb.saga.alpha.core.listener.GlobalTxListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.apache.servicecomb.saga.common.EventType.*;

class SpringTxEventRepository implements TxEventRepository {
  private static final Logger LOG = LoggerFactory.getLogger(SpringTxEventRepository.class);

  private final TxEventEnvelopeRepository eventRepo;

  @Autowired
  private IDataDictionaryService dataDictionaryService;

  @Autowired
  private GlobalTxListener globalTxListener;

  SpringTxEventRepository(TxEventEnvelopeRepository eventRepo) {
    this.eventRepo = eventRepo;
  }

  @Override
  public void save(TxEvent event) {
    globalTxListener.listenEvent(event);
    TxEvent saveEvent = eventRepo.save(event);
    if (saveEvent != null) {
      event.setSurrogateId(saveEvent.id());
      globalTxListener.listenEvent(event);
    }
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
  public List<TxEvent> findTimeoutEvents(List<String> globalTxId) {
    return eventRepo.findTimeoutEvents(globalTxId, new Date());
  }

  @Override
  public Optional<TxEvent> findTxStartedEvent(String globalTxId, String localTxId) {
    return eventRepo.findFirstStartedEventByGlobalTxIdAndLocalTxId(globalTxId, localTxId);
  }

  @Override
  public List<TxEvent> findSequentialCompensableEventOfUnended(long unendedMinEventId) {
    return eventRepo.findSequentialCompensableEventOfUnended(unendedMinEventId);
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
  public Set<String> selectEndedGlobalTx(Set<String> localTxIdSet) {
    return eventRepo.selectEndedGlobalTx(localTxIdSet);
  }

  @Override
  public boolean checkIsExistsEventType(String globalTxId, String localTxId, String type) {
    return eventRepo.checkIsExistsEventType(globalTxId, localTxId, type) > 0;
  }

  @Override
  public boolean checkTxIsAborted(String globalTxId, String localTxId) {
    return eventRepo.checkTxIsAborted(globalTxId, localTxId) > 0;
  }

  @Override
  public List<Map<String, Object>> findTxList(int pageIndex, int pageSize, String orderName, String direction, String searchText) {
    // 确定本次分页查询的全局事务
    List<TxEvent> txStartedEventList = this.searchTxList(pageIndex, pageSize, orderName, direction, searchText);
    if (txStartedEventList != null && !txStartedEventList.isEmpty()) {
      List<Map<String, Object>> resultTxEventList = new LinkedList<>();

      List<String> globalTxIdList = new ArrayList<>();
      txStartedEventList.forEach(event -> {
        globalTxIdList.add(event.globalTxId());
        resultTxEventList.add(event.toMap());
      });

      List<TxEvent> txEventList = eventRepo.selectTxEventByGlobalTxIds(globalTxIdList);
      if (txEventList != null && !txEventList.isEmpty()) {
        // 计算全局事务的状态
        computeGlobalTxStatus(txEventList, resultTxEventList);
      }

      return resultTxEventList;
    }
    return null;
  }

  private List<TxEvent> searchTxList(int pageIndex, int pageSize, String orderName, String direction, String searchText) {
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
  public long findTxCount(String searchText) {
    if (searchText == null || searchText.length() == 0) {
      return eventRepo.findTxListCount();
    }
    return eventRepo.findTxListCount(searchText);
  }

  @Override
  public List<Map<String, Object>> findSubTxList(String globalTxIds) {
      if (globalTxIds != null && globalTxIds.length() > 0) {
          List<String> globalTxIdList = Arrays.asList(globalTxIds.split(","));
          List<TxEvent> txEventList = eventRepo.selectSpecialColumnsOfTxEventByGlobalTxIds(globalTxIdList);
          if (txEventList != null && !txEventList.isEmpty()) {
              List<Map<String, Object>> resultTxEventList = new LinkedList<>();
              Set<String> localTxIdSet = new HashSet<>();
              txEventList.forEach(event -> {
                  if (TxStartedEvent.name().equals(event.type()) && !localTxIdSet.contains(event.localTxId())) {
                      localTxIdSet.add(event.localTxId());
                      resultTxEventList.add(event.toMap());
                  }
              });

              computeSubTxStatus(txEventList, resultTxEventList);

              return resultTxEventList;
          }
      }
      return null;
  }

  @Override
  public List<TxEvent> selectUnendedTxEvents(long unendedMinEventId) {
    return eventRepo.selectUnendedTxEvents(unendedMinEventId);
  }

  @Override
  public long selectMinUnendedTxEventId(long unendedMinEventId) {
    return eventRepo.selectMinUnendedTxEventId(unendedMinEventId);
  }

  @Override
  public Date selectMinDateInTxEvent() {
    return eventRepo.selectMinDateInTxEvent();
  }

  @Override
  public List<Long> selectEndedEventIdsWithinSomePeriod(int pageIndex, int pageSize, Date startTime, Date endTime) {
    return eventRepo.selectEndedEventIdsWithinSomePeriod(new PageRequest(pageIndex, pageSize), startTime, endTime);
  }

  public TxEvent selectMinRetriesEventByTxIdType(String globalTxId, String localTxId, String type) {
    return eventRepo.selectMinRetriesEventByTxIdType(globalTxId, localTxId, type);
  }

  @Override
  public TxEvent selectEventByGlobalTxIdType(String globalTxId, String type) {
    return eventRepo.selectEventByGlobalTxIdType(globalTxId, type);
  }

  public boolean checkIsAlreadyRetried(String globalTxId, String localTxId) {
    return eventRepo.selectStartedAndAbortedEndRate(globalTxId, localTxId) == 0;
  }

  // 计算全局事务的状态
  private void computeGlobalTxStatus(List<TxEvent> txEventList, List<Map<String, Object>> resultTxEventList) {
    Map<String, String> statusValueName = new HashMap<>();
    List<DataDictionaryItem> dataDictionaryItemList = dataDictionaryService.selectDataDictionaryList("global-tx-status");
    if (dataDictionaryItemList != null && !dataDictionaryItemList.isEmpty()) {
      dataDictionaryItemList.forEach(dd -> statusValueName.put(dd.getValue(), dd.getName()));
    }

    // 0-运行中，1-运行异常，2-暂停，3-正常结束，4-异常结束
    resultTxEventList.forEach(txMap -> {
      txMap.put("status_db", 0);
      txMap.put("status", statusValueName.get("0"));
    });

    txEventList.forEach(event -> {
      if (TxAbortedEvent.name().equals(event.type())) {
        for (Map<String, Object> txMap : resultTxEventList) {
          if (event.globalTxId().equals(txMap.get("globalTxId").toString())) {
            // 异常状态
            txMap.put("status_db", 1);
            txMap.put("status", statusValueName.get("1"));
            break;
          }
        }
      }
    });

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    txEventList.forEach(event -> {
      if (SagaEndedEvent.name().equals(event.type())) {
        for (Map<String, Object> txMap : resultTxEventList) {
          if (event.globalTxId().equals(txMap.get("globalTxId").toString())) {
            // ****设置结束时间****
            txMap.put("endTime", sdf.format(event.creationTime()));
            if (Integer.parseInt(txMap.get("status_db").toString()) == 0) {
              // 正常结束
              txMap.put("status_db", 3);
              txMap.put("status", statusValueName.get("3"));
            } else {
              // 异常结束
              txMap.put("status_db", 4);
              txMap.put("status", statusValueName.get("4"));
            }
            break;
          }
        }
      }
    });

    resultTxEventList.forEach(txMap -> {
      // 正常状态场景才去验证是否暂停
      if (Integer.parseInt(txMap.get("status_db").toString()) == 0) {
        txEventList.forEach(event -> {
          if (event.globalTxId().equals(txMap.get("globalTxId").toString()) && (AdditionalEventType.SagaPausedEvent.name().equals(event.type()) || AdditionalEventType.SagaAutoContinuedEvent.name().equals(event.type()))) {
            List<TxEvent> pauseContinueEventList = eventRepo.selectPausedAndContinueEvent(event.globalTxId());
            if (pauseContinueEventList != null && !pauseContinueEventList.isEmpty()) {
              // 暂停状态
              if (pauseContinueEventList.size() % 2 == 1) {
                // 暂停
                txMap.put("status_db", 2);
                txMap.put("status", statusValueName.get("2"));
              }
            }
          }
        });
      }
    });
  }

  // 计算子事务的状态
  private void computeSubTxStatus(List<TxEvent> txEventList, List<Map<String, Object>> resultTxEventList) {
    Map<String, String> statusValueName = new HashMap<>();
    List<DataDictionaryItem> dataDictionaryItemList = dataDictionaryService.selectDataDictionaryList("global-tx-status");
    if (dataDictionaryItemList != null && !dataDictionaryItemList.isEmpty()) {
      dataDictionaryItemList.forEach(dd -> statusValueName.put(dd.getValue(), dd.getName()));
    }

    // 0-运行中，1-运行异常，2-暂停，3-正常结束，4-异常结束
    resultTxEventList.forEach(txMap -> {
      txMap.put("status_db", 0);
      txMap.put("status", statusValueName.get("0"));
    });

    txEventList.forEach(event -> {
      if (TxAbortedEvent.name().equals(event.type())) {
        for (Map<String, Object> txMap : resultTxEventList) {
          if (event.localTxId().equals(txMap.get("localTxId").toString())) {
            // 异常状态
            txMap.put("status_db", 1);
            txMap.put("status", statusValueName.get("1"));
            break;
          }
        }
      }
    });

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    txEventList.forEach(event -> {
      if (TxEndedEvent.name().equals(event.type())) {
        for (Map<String, Object> txMap : resultTxEventList) {
          if (event.localTxId().equals(txMap.get("localTxId").toString())) {
            // ****设置结束时间****
            txMap.put("endTime", sdf.format(event.creationTime()));
            if (Integer.parseInt(txMap.get("status_db").toString()) == 0) {
              // 正常结束
              txMap.put("status_db", 3);
              txMap.put("status", statusValueName.get("3"));
            } else {
              // 异常结束
              txMap.put("status_db", 4);
              txMap.put("status", statusValueName.get("4"));
            }
            break;
          }
        }
      }
    });

    resultTxEventList.forEach(txMap -> {
      // 正常状态场景才去验证是否暂停
      if (Integer.parseInt(txMap.get("status_db").toString()) == 0) {
        txEventList.forEach(event -> {
          if (event.localTxId().equals(txMap.get("localTxId").toString()) && (AdditionalEventType.SagaPausedEvent.name().equals(event.type()) || AdditionalEventType.SagaAutoContinuedEvent.name().equals(event.type()))) {
            List<TxEvent> pauseContinueEventList = eventRepo.selectPausedAndContinueEvent(event.globalTxId());
            if (pauseContinueEventList != null && !pauseContinueEventList.isEmpty()) {
              // 暂停状态
              if (pauseContinueEventList.size() % 2 == 1) {
                // 暂停
                txMap.put("status_db", 2);
                txMap.put("status", statusValueName.get("2"));
              }
            }
          }
        });
      }
    });
  }

}
