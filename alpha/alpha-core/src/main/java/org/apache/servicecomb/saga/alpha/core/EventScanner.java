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

package org.apache.servicecomb.saga.alpha.core;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.health.model.Check;
import com.ecwid.consul.v1.session.model.NewSession;
import org.apache.servicecomb.saga.alpha.core.kafka.IKafkaMessageProducer;
import org.apache.servicecomb.saga.common.TxleConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.servicecomb.saga.alpha.core.TaskStatus.NEW;
import static org.apache.servicecomb.saga.common.EventType.*;
import static org.apache.servicecomb.saga.common.TxleConstants.CONSUL_LEADER_KEY;
import static org.apache.servicecomb.saga.common.TxleConstants.CONSUL_LEADER_KEY_VALUE;

public class EventScanner implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final byte[] EMPTY_PAYLOAD = new byte[0];

  private final ScheduledExecutorService scheduler;
  private final TxEventRepository eventRepository;
  private final CommandRepository commandRepository;
  private final TxTimeoutRepository timeoutRepository;
  private final OmegaCallback omegaCallback;
  private IKafkaMessageProducer kafkaMessageProducer;
  private TxleMetrics txleMetrics;

  private final int eventPollingInterval;

  private long nextEndedEventId;

  // 未完成的最小全局事务的id  使用HashMap即使及时移除其中大部分元素，内存也不会及时释放，最终导致内存耗费过高，若尝试软引用/弱引用则无法保证数据准确性
//  public static ConcurrentHashMap<String, Long> unendedMinEvent = new ConcurrentHashMap<>();
  // 不能简单地获取已完成的最大id，因为会存在部分id小的还未完成的场景，如id：1、2、3，可能3完成了，但2也许还未完成
  private static volatile long unendedMinEventId;
  // 需要查询未完成最小id的次数，如果值为0则不需要查询，初始值为1，即系统启动后先查询最小id
  public static AtomicInteger unendedMinEventIdSelectCount = new AtomicInteger(1);

  public static final String SCANNER_SQL = " /**scanner_sql**/";
  
  private final ConsulClient consulClient;
  private final String serverName;
  private int serverPort = 8090;
  private final String consulInstanceId;

  public EventScanner(ScheduledExecutorService scheduler,
      TxEventRepository eventRepository,
      CommandRepository commandRepository,
      TxTimeoutRepository timeoutRepository,
      OmegaCallback omegaCallback,
      IKafkaMessageProducer kafkaMessageProducer,
      TxleMetrics txleMetrics,
      int eventPollingInterval,
      ConsulClient consulClient,
      Object... params) {
    this.scheduler = scheduler;
    this.eventRepository = eventRepository;
    this.commandRepository = commandRepository;
    this.timeoutRepository = timeoutRepository;
    this.omegaCallback = omegaCallback;
    this.kafkaMessageProducer = kafkaMessageProducer;
    this.txleMetrics = txleMetrics;
    this.eventPollingInterval = eventPollingInterval;
    this.consulClient = consulClient;
    this.serverName = params[0] + "";
    try {this.serverPort = Integer.parseInt(params[1] + "");}catch (Exception e) {}
    this.consulInstanceId = params[2] + "";
  }

  @Override
  public void run() {
    pollEvents();
  }

  private void pollEvents() {
    final String consulSessionId = registerConsulSession();

    /**
     * 补偿业务逻辑大换血：
     *    主要需要补偿的分为两大类，超时导致的异常和其它情况导致的异常(重试属于其它情况)；
     *    原逻辑：将二者混在一起，带来了各种繁杂困扰，出现问题很难定位；
     *    现逻辑：将二者分开；TM主要指的是TxConsistentService#handleSupportTxPause(TxEvent)；
     *            其它异常情况：【客户端】负责异常检测，检测到后，由TM保存Aborted事件，再对当前全局事务下的相关子事务记录补偿并及时下发补偿命令；
     *                           排除发生异常的子事务，因为发生异常的子事务已由本地事务回滚；
     *            超时异常情况：【定时器】负责超时检测，检测到后，由TM保存Aborted事件，再对当前全局事务下的相关子事务记录补偿并及时下发补偿命令；
     *                           需对所有子事务进行补偿；
     *            TM超时检测：TM中也会对即将结束的全局事务检测是否超时，因为定时扫描器中检测超时会存在一定误差，如定时器中任务需3s完成，但某事务超时设置的是2秒，此时还未等对该事物进行检测，该事务就已经结束了
     */
    scheduler.scheduleWithFixedDelay(
            () -> {
              try {
                if (consulClient != null && consulClient.setKVValue(CONSUL_LEADER_KEY + "?acquire=" + consulSessionId, CONSUL_LEADER_KEY_VALUE).getValue()) {
                  // 未防止出现部分事务在未检测超时前就已经结束的情况，此处将超时检测单开一个线程，否则其它方法如果执行超过了事务的超时时间，那么下次超时检测将在事务之后检测了，此时事务已经正常结束了
                  updateTimeoutStatus();
                  findTimeoutEvents();
                  abortTimeoutEvents();
                }
              } catch (Exception e) {
                // to avoid stopping this scheduler in case of exception By Gannalyo
                LOG.error(TxleConstants.LOG_ERROR_PREFIX + "Failed to detect timeout in scheduler.", e);
              }
            },
            0,
            eventPollingInterval,
            MILLISECONDS);

    scheduler.scheduleWithFixedDelay(
				() -> {
					try {
                      if (consulClient != null && consulClient.setKVValue(CONSUL_LEADER_KEY + "?acquire=" + consulSessionId, CONSUL_LEADER_KEY_VALUE).getValue()) {
//                        LOG.error("Session " + serverName + "-" + serverPort + " is leader.");
//                        updateTimeoutStatus();
//						findTimeoutEvents();
//						abortTimeoutEvents();
//						saveUncompensatedEventsToCommands();
                        compensate();
                        updateCompensatedCommands();
//						deleteDuplicateSagaEndedEvents();
//						updateTransactionStatus();

                        getMinUnendedEventId();
                      }
					} catch (Exception e) {
						// to avoid stopping this scheduler in case of exception By Gannalyo
						LOG.error(TxleConstants.LOG_ERROR_PREFIX + "Failed to execute method 'compensate' in scheduler.", e);
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
    // 查询未登记过的超时
    // SELECT t.surrogateId FROM TxTimeout t, TxEvent t1 WHERE t1.globalTxId = t.globalTxId AND t1.localTxId = t.localTxId AND t1.type != t.type
    eventRepository.findTimeoutEvents(unendedMinEventId)
        .forEach(event -> {
          CurrentThreadContext.put(event.globalTxId(), event);
          LOG.info("Found timeout event {}", event);
          try {
            if (timeoutRepository.findTxTimeoutByEventId(event.id()) < 1) {
              timeoutRepository.save(txTimeoutOf(event));
            }
          } catch (Exception e) {
            LOG.error("Failed to save timeout {} in method 'EventScanner.findTimeoutEvents()'.", event, e);
          }
        });
  }

  private void abortTimeoutEvents() {
    timeoutRepository.findFirstTimeout().forEach(timeout -> {// 查找超时且状态为 NEW 的超时记录
      LOG.info("Found timeout event {} to abort", timeout);

      TxEvent abortedEvent = toTxAbortedEvent(timeout);
      CurrentThreadContext.put(abortedEvent.globalTxId(), abortedEvent);
      if (!eventRepository.checkIsExistsTxCompensatedEvent(abortedEvent.globalTxId(), abortedEvent.localTxId(), abortedEvent.type())) {
        eventRepository.save(abortedEvent);// 查找到超时记录后，记录相应的(超时)终止状态
        // 保存超时情况下的待补偿命令，当前超时全局事务下的所有应该补偿的子事件的待补偿命令 By Gannalyo
        commandRepository.saveWillCompensateCommandsForTimeout(abortedEvent.globalTxId());

//        boolean isRetried = eventRepository.checkIsRetriedEvent(abortedEvent.type());
//        txleMetrics.countTxNumber(abortedEvent, true, isRetried);

//      if (timeout.type().equals(TxStartedEvent.name())) {
//        eventRepository.findTxStartedEvent(timeout.globalTxId(), timeout.localTxId())
//            .ifPresent(omegaCallback::compensate);// 查找到超时记录后，屏蔽在此处触发补偿功能，完全交给compensate方法处理即可
//      }
      }
    });
  }

  private void saveUncompensatedEventsToCommands() {
    long a = System.currentTimeMillis();
    // nextEndedEventId不推荐使用，原因是某事务超时时间较长在定时器检测时并未超时，并且其后续执行了一些带有异常的事务，定时器检测到这些异常事务后该值被更改，然后该超时事务将无法被补偿，因为查询需要补偿的SQL中含id值大于该nextEndedEventId值条件
    List<TxEvent> eventList = eventRepository.findFirstUncompensatedEventByIdGreaterThan(nextEndedEventId, TxEndedEvent.name());
    if (eventList == null || eventList.isEmpty()) return;
    LOG.info("Method 'find uncompensated event' took {} milliseconds, size = {}, nextEndedEventId = {}.", System.currentTimeMillis() - a, eventList == null ? 0 : eventList.size(), nextEndedEventId);
    eventList.forEach(event -> {
      CurrentThreadContext.put(event.globalTxId(), event);
      LOG.info("Found uncompensated event {}, nextEndedEventId {}", event, nextEndedEventId);
      nextEndedEventId = event.id();
//      commandRepository.saveCompensationCommands(event.globalTxId());
      commandRepository.saveCommandsForNeedCompensationEvent(event.globalTxId(), event.localTxId());
    });
  }

  private void compensate() {
    long a = System.currentTimeMillis();
    List<Command> commandList = commandRepository.findFirstCommandToCompensate();
    if (commandList == null || commandList.isEmpty()) return;
    LOG.info("Method 'find compensated command' took {} milliseconds, size = {}.", System.currentTimeMillis() - a, commandList == null ? 0 : commandList.size());
    commandList.forEach(command -> {
      LOG.info("Compensating transaction with globalTxId {} and localTxId {}",
              command.globalTxId(),
              command.localTxId());

      omegaCallback.compensate(txStartedEventOf(command));// 该方法会最终调用客户端的org.apache.servicecomb.saga.omega.transaction.CompensationMessageHandler.onReceive方法进行补偿和请求存储补偿事件
    });
  }

  private void updateCompensatedCommands() {
    // The 'findFirstCompensatedEventByIdGreaterThan' interface did not think about the 'SagaEndedEvent' type so that would do too many thing those were wasted.
    long a = System.currentTimeMillis();
    List<TxEvent> compensatedUnendEventList = eventRepository.findSequentialCompensableEventOfUnended(unendedMinEventId);
    if (compensatedUnendEventList == null || compensatedUnendEventList.isEmpty()) return;
    LOG.info("Method 'find compensated(unend) event' took {} milliseconds, size = {}.", System.currentTimeMillis() - a, compensatedUnendEventList == null ? 0 : compensatedUnendEventList.size());
    compensatedUnendEventList.forEach(event -> {
      CurrentThreadContext.put(event.globalTxId(), event);
      LOG.info("Found compensated event {}", event);
      updateCompensationStatus(event);
    });
  }

  private void deleteDuplicateSagaEndedEvents() {
    try {
      List<Long> maxSurrogateIdList = eventRepository.getMaxSurrogateIdGroupByGlobalTxIdByType(SagaEndedEvent.name());
      if (maxSurrogateIdList != null && !maxSurrogateIdList.isEmpty()) {
        eventRepository.deleteDuplicateEventsByTypeAndSurrogateIds(SagaEndedEvent.name(), maxSurrogateIdList);
      }
//      eventRepository.deleteDuplicateEvents(SagaEndedEvent.name());
    } catch (Exception e) {
      LOG.warn("Failed to delete duplicate event", e);
    }
  }

  private void updateCompensationStatus(TxEvent event) {
    commandRepository.markCommandAsDone(event.globalTxId(), event.localTxId());
    LOG.info("Transaction with globalTxId {} and localTxId {} was compensated",
        event.globalTxId(),
        event.localTxId());

    CurrentThreadContext.put(event.globalTxId(), event);

    markSagaEnded(event);
  }

  private void updateTransactionStatus() {
    eventRepository.findFirstAbortedGlobalTransaction().ifPresent(this::markGlobalTxEndWithEvents);
  }

  private void markSagaEnded(TxEvent event) {
    CurrentThreadContext.put(event.globalTxId(), event);
    // 如果没有未补偿的命令，则结束当前全局事务
    // TODO 检查当前全局事务对应的事件是否还有需要补偿的子事务???
    if (commandRepository.findUncompletedCommands(event.globalTxId()).isEmpty()) {
      markGlobalTxEndWithEvent(event);
    }
  }

  private void markGlobalTxEndWithEvent(TxEvent event) {
    try {
      CurrentThreadContext.put(event.globalTxId(), event);
      TxEvent sagaEndedEvent = toSagaEndedEvent(event);
      eventRepository.save(sagaEndedEvent);
      // To send message to Kafka.
      kafkaMessageProducer.send(sagaEndedEvent);
      LOG.info("Marked end of transaction with globalTxId {}", event.globalTxId());
    } catch (Exception e) {
      LOG.error("Failed to save event globalTxId {} localTxId {} type {}", event.globalTxId(), event.localTxId(), event.type(), e);
    }
  }

  private void markGlobalTxEndWithEvents(List<TxEvent> events) {
    events.forEach(this::markGlobalTxEndWithEvent);
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
        ("Transaction timeout").getBytes());
  }

  private TxEvent toSagaEndedEvent(TxEvent event) {
    return new TxEvent(
        event.serviceName(),
        event.instanceId(),
        event.globalTxId(),
        event.globalTxId(),
        null,
        SagaEndedEvent.name(),
        "",
        event.category(),
        EMPTY_PAYLOAD);
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
      if (unendedMinEventIdSelectCount.get() == 0) return;
      unendedMinEventIdSelectCount.decrementAndGet();
      // 上面的方法，既能保证准确性，又不节省性能开销，但对性能的开销会越来越大，且也不太适合ServerCluster场景
      // 不保证是未完成最小的，但保证比最小未完成的还小。即：id：1、2、3...10，如果2未完成，此时定时器执行该方法，则返回2，之后2立即结束，3-7结束，8-10执行，此时再次检测，min id仍是2，而不是7/8，相当于多查询了几条数据，但保证足够的准确性。
      long currentMinid = eventRepository.selectMinUnendedTxEventId(unendedMinEventId);
      if (unendedMinEventId < currentMinid) {
        unendedMinEventId = currentMinid;
      } else if (currentMinid == 0 || currentMinid == unendedMinEventId) {
        unendedMinEventIdSelectCount.set(0);
      }
    } catch (Exception e) {
      LOG.error(TxleConstants.LOG_ERROR_PREFIX + "Failed to get the min id of global transaction which is not ended.", e);
    }
  }

  public static long getUnendedMinEventId() {
    return unendedMinEventId;
  }

  /**
   * Multiple txle apps register the same key 'CONSUL_LEADER_KEY', it would be leader in case of getting 'true'.
   * The Session, Checks and Services have to be destroyed/deregistered before shutting down JVM, so that the lock of leader key could be released.
   */
  private String registerConsulSession() {
    if (consulClient == null) return null;
    String consulSessionId = null;
    try {
      // To create a key for leader election no matter if it is exists.
      consulClient.setKVValue(CONSUL_LEADER_KEY, CONSUL_LEADER_KEY_VALUE);
      NewSession session = new NewSession();
      session.setName("session-" + serverName + "-" + serverPort);
      consulSessionId = consulClient.sessionCreate(session, null).getValue();
    } catch (Exception e) {
      LOG.error("Failed to register Consul Session, serverName [{}], serverPort [{}].", serverName, serverPort, e);
    } finally {
      try {
        final String finalConsulSessionId = consulSessionId;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
          // To deregister service could not destroy session so that current service still held the lock for leader's key.
          // So to destroy session was necessary as well.
          consulClient.sessionDestroy(finalConsulSessionId, null);
          // consulClient.agentServiceDeregister(consulInstanceId);
          List<Check> checkList = consulClient.getHealthChecksState(null).getValue();
          checkList.forEach(check -> {
            if (check.getStatus() != Check.CheckStatus.PASSING || check.getServiceId().equals(consulInstanceId)) {
              consulClient.agentCheckDeregister(check.getCheckId());
              consulClient.agentServiceDeregister(check.getServiceId());
            }
          });
        }));
      } catch (Exception e) {
        LOG.error("Failed to add ShutdownHook for destroying/deregistering Consul Session, Checks and Services, serverName [{}], serverPort [{}].", serverName, serverPort, e);
      }
    }
    return consulSessionId;
  }
}
