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

import static org.apache.servicecomb.saga.alpha.core.TaskStatus.DONE;
import static org.apache.servicecomb.saga.alpha.core.TaskStatus.NEW;
import static org.apache.servicecomb.saga.alpha.core.TaskStatus.PENDING;

import java.lang.invoke.MethodHandles;
import java.util.*;

import javax.transaction.Transactional;

import org.apache.servicecomb.saga.alpha.core.Command;
import org.apache.servicecomb.saga.alpha.core.CommandRepository;
import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    List<TxEvent> events = eventRepository
        .findStartedEventsWithMatchingEndedButNotCompensatedEvents(globalTxId);// 查询已结束但未补偿的子事务，之后保存该子事务的补偿命令，供后续补偿使用
    if (events == null || events.isEmpty()) {
      LOG.debug("Executed method 'TxEventEnvelopeRepository.findStartedEventsWithMatchingEndedButNotCompensatedEvents' globalTxId {}.", globalTxId);
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
        if (!existEventIdList.contains(command.getEventId())) {// To avoid to save if the eventId is exists. If not, it will print 'duplicate eventId....' exception.
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
    List<TxEvent> txStartedEvents = eventRepository.findNeedCompensateEventForTimeout(globalTxId);
    if (txStartedEvents != null && !txStartedEvents.isEmpty()) {
      txStartedEvents.forEach(event -> {
        try {
          commandRepository.save(new Command(event));
        } catch (Exception e) {
          LOG.warn("Failed to save command {} in method 'saveWillCompensateCommandsForTimeout'.", event);
        }
      });
    }
  }

  @Override
  public void saveWillCompensateCommandsForException(String globalTxId, String localTxId) {
    List<TxEvent> txStartedEvents = eventRepository.findNeedCompensateEventForException(globalTxId, localTxId);
    if (txStartedEvents != null && !txStartedEvents.isEmpty()) {
      txStartedEvents.forEach(event -> {
        try {
          commandRepository.save(new Command(event));
        } catch (Exception e) {
          LOG.warn("Failed to save command {} in method 'saveWillCompensateCommandsForException'.", event);
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
//    return commandRepository.findByGlobalTxIdAndStatus(globalTxId, NEW.name());
    return commandRepository.findUncompletedCommandByGlobalTxIdAndStatus(globalTxId, DONE.name());
  }

  @Transactional
  @Override
  public List<Command> findFirstCommandToCompensate() {
//    List<Command> commands = commandRepository.findFirstGroupByGlobalTxIdWithoutPendingOrderByIdDesc();
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
