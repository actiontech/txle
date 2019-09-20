/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.core;

import org.apache.servicecomb.saga.alpha.core.cache.ITxleCache;
import org.apache.servicecomb.saga.common.TxleConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.servicecomb.saga.alpha.core.TaskStatus.NEW;
import static org.apache.servicecomb.saga.common.EventType.TxAbortedEvent;
import static org.apache.servicecomb.saga.common.EventType.TxStartedEvent;

public class EventScanner implements Runnable {
  private final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final ScheduledExecutorService scheduler;
  private final TxEventRepository eventRepository;
  private final CommandRepository commandRepository;
  private final TxTimeoutRepository timeoutRepository;
  private final OmegaCallback omegaCallback;

  private final long eventPollingInterval;

  // Could not find the max id directly. In some cases, the minimum identify of undone event may be not max. Such ids: 1, 2, 3, 3 is done, but 2 is running yet.
  private static volatile long unendedMinEventId;
  // Initial value is 1, increase 1 after starting global transaction and decrease 1 after executing 'getMinUnendedEventId()' method.
  // Do not execute 'getMinUnendedEventId()' method in case of 0 or equals the value of the variable 'unendedMinEventId'.
  public static final AtomicInteger UNENDED_MIN_EVENT_ID_SELECT_COUNT = new AtomicInteger(1);

  public static final String SCANNER_SQL = " /**scanner_sql**/";

  private ITxleCache txleCache;
  private StartingTask startingTask;

  public EventScanner(ScheduledExecutorService scheduler,
                      TxEventRepository eventRepository,
                      CommandRepository commandRepository,
                      TxTimeoutRepository timeoutRepository,
                      OmegaCallback omegaCallback,
                      int eventPollingInterval,
                      ITxleCache txleCache,
                      StartingTask startingTask) {
    this.scheduler = scheduler;
    this.eventRepository = eventRepository;
    this.commandRepository = commandRepository;
    this.timeoutRepository = timeoutRepository;
    this.omegaCallback = omegaCallback;
    this.eventPollingInterval = eventPollingInterval;
    this.txleCache = txleCache;
    this.startingTask = startingTask;
  }

  @Override
  public void run() {
    pollEvents();
  }

  private void pollEvents() {
    /**
     * 1.check timeout by scheduler
     *    Produce aborted event and compensating command after checking timeout out.
     * 2.check timeout by TM (TM(Transaction Manager): TxConsistentService#handleSupportTxPause(TxEvent))
     *    Check timeout after ending sub-transaction. Produce aborted event and compensating command after checking timeout out.
     * 3.compensate for all of compensating commands.
     */
    scheduler.scheduleWithFixedDelay(
            () -> {
              try {
                if (startingTask.isMaster()) {
                  // Use a new scheduler for lessening the latency of checking timeout.
                  updateTimeoutStatus();
                  findTimeoutEvents();
                  abortTimeoutEvents();
                }
              } catch (Exception e) {
                // to avoid stopping this scheduler in case of exception By Gannalyo
                log.error(TxleConstants.LOG_ERROR_PREFIX + "Failed to detect timeout in scheduler.", e);
              }
            },
            0,
            eventPollingInterval,
            MILLISECONDS);

    scheduler.scheduleWithFixedDelay(
            () -> {
              try {
                if (startingTask.isMaster()) {
                  compensate();
                  updateCompensatedCommands();
                  getMinUnendedEventId();
                }
              } catch (Exception e) {
                // to avoid stopping this scheduler in case of exception By Gannalyo
                log.error(TxleConstants.LOG_ERROR_PREFIX + "Failed to execute method 'compensate' in scheduler.", e);
              }
            },
            0,
            eventPollingInterval * 2,
            MILLISECONDS);
  }

  private void updateTimeoutStatus() {
    List<Long> timeoutIdList = timeoutRepository.selectTimeoutIdList();
    if (timeoutIdList != null && !timeoutIdList.isEmpty()) {
      timeoutRepository.markTimeoutAsDone(timeoutIdList);
    }
  }

  private void findTimeoutEvents() {
    // check and record timeout
    // SELECT t.surrogateId FROM TxTimeout t, TxEvent t1 WHERE t1.globalTxId = t.globalTxId AND t1.localTxId = t.localTxId AND t1.type != t.type
    eventRepository.findTimeoutEvents(unendedMinEventId)
            .forEach(event -> {
              CurrentThreadContext.put(event.globalTxId(), event);
              log.info("Found timeout event {}", event);
              try {
                if (timeoutRepository.findTxTimeoutByEventId(event.id()) < 1) {
                  timeoutRepository.save(txTimeoutOf(event));
                }
              } catch (Exception e) {
                log.error("Failed to save timeout {} in method 'EventScanner.findTimeoutEvents()'.", event, e);
              }
            });
  }

  private void abortTimeoutEvents() {
    // select timeout records which have a 'NEW' status.
    List<TxTimeout> txTimeoutList = timeoutRepository.findFirstTimeout();
    if (txTimeoutList != null && !txTimeoutList.isEmpty()) {
      txTimeoutList.forEach(timeout -> {
        log.info("Found timeout event {} to abort", timeout);
        // set cache for aborted tx as soon as possible so that next sub-transaction can get the aborted status when it verifies the aborted status.
        txleCache.putDistributedTxAbortStatusCache(timeout.globalTxId(), true, 2);
      });

      txTimeoutList.forEach(timeout -> {
        TxEvent abortedEvent = toTxAbortedEvent(timeout);
        CurrentThreadContext.put(abortedEvent.globalTxId(), abortedEvent);
        if (!eventRepository.checkIsExistsEventType(abortedEvent.globalTxId(), abortedEvent.localTxId(), abortedEvent.type())) {
          // record abort event in case of timeout.
          eventRepository.save(abortedEvent);
          // save compensating record
          commandRepository.saveWillCompensateCommandsForTimeout(abortedEvent.globalTxId());
        }
      });
    }
  }

  private void compensate() {
    List<Command> commandList = commandRepository.findFirstCommandToCompensate();
    if (commandList == null || commandList.isEmpty()) {
      return;
    }
    commandList.forEach(command -> {
      log.error("Compensating transaction with globalTxId {} and localTxId {}", command.globalTxId(), command.localTxId());
      // call the client method 'org.apache.servicecomb.saga.omega.transaction.CompensationMessageHandler.onReceive()' to execute compensation.
      omegaCallback.compensate(txStartedEventOf(command));
    });
  }

  private void updateCompensatedCommands() {
    // The 'findFirstCompensatedEventByIdGreaterThan' interface did not think about the 'SagaEndedEvent' type so that would do too many thing those were wasted.
    List<TxEvent> compensatedUnendEventList = eventRepository.findSequentialCompensableEventOfUnended(unendedMinEventId);
    if (compensatedUnendEventList == null || compensatedUnendEventList.isEmpty()) {
      return;
    }
    compensatedUnendEventList.forEach(event -> {
      CurrentThreadContext.put(event.globalTxId(), event);
      log.info("Found compensated event {}", event);
      updateCompensationStatus(event);
    });
  }

  private void updateCompensationStatus(TxEvent event) {
    commandRepository.markCommandAsDone(event.globalTxId(), event.localTxId());
    log.info("Transaction with globalTxId {} and localTxId {} was compensated", event.globalTxId(), event.localTxId());
    CurrentThreadContext.put(event.globalTxId(), event);
  }

  private TxEvent toTxAbortedEvent(TxTimeout timeout) {
    return new TxEvent(
            timeout.serviceName(),
            timeout.instanceId(),
            timeout.globalTxId(),
            timeout.localTxId(),
            timeout.parentTxId(),
            TxAbortedEvent.name(),
            "",
            timeout.category(),
            "Transaction timeout".getBytes());
  }

  private TxEvent txStartedEventOf(Command command) {
    return new TxEvent(
            command.serviceName(),
            command.instanceId(),
            command.globalTxId(),
            command.localTxId(),
            command.parentTxId(),
            TxStartedEvent.name(),
            command.compensationMethod(),
            command.category(),
            command.payloads()
    );
  }

  private TxTimeout txTimeoutOf(TxEvent event) {
    return new TxTimeout(
            event.id(),
            event.serviceName(),
            event.instanceId(),
            event.globalTxId(),
            event.localTxId(),
            event.parentTxId(),
            event.type(),
            event.expiryTime(),
            NEW.name(),
            event.category()
    );
  }

  private void getMinUnendedEventId() {
    try {
      if (UNENDED_MIN_EVENT_ID_SELECT_COUNT.get() == 0) {
        return;
      }
      UNENDED_MIN_EVENT_ID_SELECT_COUNT.decrementAndGet();
      long currentMinId = eventRepository.selectMinUnendedTxEventId(unendedMinEventId);
      if (unendedMinEventId < currentMinId) {
        unendedMinEventId = currentMinId;
      } else if (currentMinId == 0 || currentMinId == unendedMinEventId) {
        UNENDED_MIN_EVENT_ID_SELECT_COUNT.set(0);
      }
    } catch (Exception e) {
      log.error(TxleConstants.LOG_ERROR_PREFIX + "Failed to get the min id of global transaction which is not ended.", e);
    }
  }

  public static long getUnendedMinEventId() {
    return unendedMinEventId;
  }

}
