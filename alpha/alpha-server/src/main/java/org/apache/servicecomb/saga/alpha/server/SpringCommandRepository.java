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

import org.apache.servicecomb.saga.alpha.core.Command;
import org.apache.servicecomb.saga.alpha.core.CommandRepository;
import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.Transactional;
import java.lang.invoke.MethodHandles;
import java.util.*;

import static org.apache.servicecomb.saga.alpha.core.TaskStatus.*;
import static org.apache.servicecomb.saga.common.EventType.TxCompensatedEvent;

public class SpringCommandRepository implements CommandRepository {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final TxEventEnvelopeRepository eventRepository;
  private final CommandEntityRepository commandRepository;

  SpringCommandRepository(TxEventEnvelopeRepository eventRepository, CommandEntityRepository commandRepository) {
    this.eventRepository = eventRepository;
    this.commandRepository = commandRepository;
  }

  @Override
  public void saveCompensationCommands(String globalTxId) {
    // When some sub-transaction has been done with exception and it is still not compensated, search its 'StartedEvent' for compensating.
    List<TxEvent> events = eventRepository.findDoneAndUncompensatedSubTx(globalTxId);
    if (events == null || events.isEmpty()) {
      LOG.debug("Executed method 'TxEventEnvelopeRepository.findDoneAndUncompensatedSubTx' globalTxId {}.", globalTxId);
      return;
    }

    Map<String, Command> commands = new LinkedHashMap<>();

    Set<Long> eventIdSet = new HashSet<>();
    for (TxEvent event : events) {
      commands.computeIfAbsent(event.localTxId(), k -> new Command(event));
      eventIdSet.add(event.id());
    }

    Set<Long> existEventIdList = commandRepository.findExistCommandList(eventIdSet);
    if (existEventIdList == null) {
      existEventIdList = new HashSet<>();
    }

    for (Command command : commands.values()) {
      try {
        // To avoid to save if the eventId is exists. If not, it will print 'duplicate eventId....' exception.
        if (!existEventIdList.contains(command.getEventId())) {
          if (commandRepository.save(command) != null) {
            LOG.info("Saved compensation command {}", command);
          }
        }
      } catch (Exception e) {
        LOG.warn("Failed to save some command {}", command);
      }
    }
  }

  @Override
  public void saveCommandsForNeedCompensationEvent(String globalTxId, String localTxId) {
    List<TxEvent> txStartedEvents = eventRepository.selectTxStartedEventByLocalTxId(globalTxId, localTxId);
    if (txStartedEvents != null && !txStartedEvents.isEmpty()) {
      Set<Long> eventIdSet = new HashSet<>();
      txStartedEvents.forEach(event -> eventIdSet.add(event.id()));
      Set<Long> existEventIdList = new HashSet<>();
      if (!eventIdSet.isEmpty()) {
        Set<Long> existCommandEventIdList = commandRepository.findExistCommandList(eventIdSet);
        if (existCommandEventIdList != null && !existCommandEventIdList.isEmpty()) {
          existEventIdList.addAll(existCommandEventIdList);
        }
      }

      txStartedEvents.forEach(event -> {
        try {
          if (!existEventIdList.contains(event.id())) {
            commandRepository.save(new Command(event));
            existEventIdList.add(event.id());
            LOG.info("Saved compensation command {}", event);
          }
        } catch (Exception e) {
          LOG.warn("Failed to save some command {}", event);
        }
      });
    }
  }

  @Override
  public void saveWillCompensateCommandsForTimeout(String globalTxId) {
    saveWillCompensateCommands(eventRepository.findNeedCompensateEventForGlobalTxAborted(globalTxId), "saveWillCompensateCommandsForTimeout");
  }

  @Override
  public void saveWillCompensateCommandsForException(String globalTxId, String localTxId) {
    saveWillCompensateCommands(eventRepository.findNeedCompensateEventForException(globalTxId, localTxId), "saveWillCompensateCommandsForException");
  }

  @Override
  public void saveWillCompensateCommandsWhenGlobalTxAborted(String globalTxId) {
    saveWillCompensateCommands(eventRepository.findNeedCompensateEventForGlobalTxAborted(globalTxId), "saveWillCompensateCommandsWhenGlobalTxAborted");
  }

  @Override
  public void saveWillCompensateCmdForCurSubTx(String globalTxId, String localTxId) {
    saveWillCompensateCommands(eventRepository.selectTxStartedEventByLocalTxId(globalTxId, localTxId), "saveWillCompensateCmdForCurSubTx");
  }

  private void saveWillCompensateCommands(List<TxEvent> txStartedEvents, String method) {
    if (txStartedEvents != null && !txStartedEvents.isEmpty()) {
      Set<Long> eventIdSet = new HashSet<>();
      txStartedEvents.forEach(event -> eventIdSet.add(event.id()));
      Set<Long> existCommandEventIdList = commandRepository.findExistCommandList(eventIdSet);

      txStartedEvents.forEach(event -> {
        try {
          if (!existCommandEventIdList.contains(event.id())) {
            commandRepository.save(new Command(event));
            eventRepository.save(new TxEvent(event.serviceName(), event.instanceId(), event.globalTxId(), event.localTxId(), event.parentTxId(), TxCompensatedEvent.name(), event.compensationMethod(), event.category(), event.payloads()));
          }
        } catch (Exception e) {
          LOG.warn("Failed to save command {} in method {}.", event, method);
        }
      });
    }
  }

  @Override
  public void markCommandAsDone(String globalTxId, String localTxId) {
    commandRepository.updateStatusByGlobalTxIdAndLocalTxId(DONE.name(), globalTxId, localTxId);
  }

  @Override
  public List<Command> findUncompletedCommands(String globalTxId) {
    return commandRepository.findUncompletedCommandByGlobalTxIdAndStatus(globalTxId, DONE.name());
  }

  @Transactional
  @Override
  public List<Command> findFirstCommandToCompensate() {
    List<Command> commands = commandRepository.findCommandByStatus("NEW");

    commands.forEach(command -> {
      try {
        commandRepository.updateStatusByGlobalTxIdAndLocalTxId(
                NEW.name(),
                PENDING.name(),
                command.globalTxId(),
                command.localTxId());
      } catch (Exception e) {
        LOG.error("Failed to execute method 'updateStatusByGlobalTxIdAndLocalTxId' localTxId {}.", command.localTxId(), e);
      }
    });

    return commands;
  }
}
