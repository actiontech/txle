/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.server;

import static org.apache.servicecomb.saga.alpha.core.TaskStatus.DONE;
import static org.apache.servicecomb.saga.alpha.core.TaskStatus.NEW;
import static org.apache.servicecomb.saga.alpha.core.TaskStatus.PENDING;
import static org.apache.servicecomb.saga.common.EventType.TxCompensatedEvent;

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
    // 查询已结束但未补偿的子事务，之后保存该子事务的补偿命令，供后续补偿使用
    List<TxEvent> events = eventRepository.findStartedEventsWithMatchingEndedButNotCompensatedEvents(globalTxId);
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
    try {
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
            LOG.error("Failed to save command {} in method {}.", event, method);
          }
        });
      }
    } catch (Exception e) {
      LOG.error("Failed to save command {} in method {}.", txStartedEvents.toArray().toString(), method);
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
