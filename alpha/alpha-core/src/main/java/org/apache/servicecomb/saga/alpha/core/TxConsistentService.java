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

import org.apache.servicecomb.saga.alpha.core.kafka.IKafkaMessageProducer;
import org.apache.servicecomb.saga.alpha.core.kafka.IKafkaMessageRepository;
import org.apache.servicecomb.saga.alpha.core.kafka.KafkaMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.apache.servicecomb.saga.alpha.core.TaskStatus.NEW;
import static org.apache.servicecomb.saga.common.EventType.*;

public class TxConsistentService {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final TxEventRepository eventRepository;
  private final CommandRepository commandRepository;
  private final TxTimeoutRepository timeoutRepository;

  @Autowired
  private IKafkaMessageProducer kafkaMessageProducer;

  @Autowired
  private IKafkaMessageRepository kafkaMessageRepository;

	@Autowired
	UtxMetrics utxMetrics;

  private final List<String> types = Arrays.asList(TxStartedEvent.name(), SagaEndedEvent.name());

  public TxConsistentService(TxEventRepository eventRepository, CommandRepository commandRepository, TxTimeoutRepository timeoutRepository) {
    this.eventRepository = eventRepository;
    this.commandRepository = commandRepository;
    this.timeoutRepository = timeoutRepository;
  }

  public boolean handle(TxEvent event) {
	  utxMetrics.startMarkTxDuration(event);// start duration.
	  if (types.contains(event.type()) && isGlobalTxAborted(event)) {
		  LOG.info("Transaction event {} rejected, because its parent with globalTxId {} was already aborted",
				  event.type(), event.globalTxId());
		  utxMetrics.endMarkTxDuration(event);// end duration.
		  return false;
	  }

	  eventRepository.save(event);
	  utxMetrics.endMarkTxDuration(event);// end duration.

	  return true;
  }

	/**
	 * handle the event. support transaction pause/continue/auto-continue.
	 *
	 * @author Gannalyo
	 */
	public int handleSupportTxPause(TxEvent event) {
		utxMetrics.startMarkTxDuration(event);// start duration.
		utxMetrics.countChildTxNumber(event);// child transaction count
		if (types.contains(event.type()) && isGlobalTxAborted(event)) {
			LOG.info("Transaction event {} rejected, because its parent with globalTxId {} was already aborted", event.type(), event.globalTxId());
			boolean isRetried = eventRepository.checkIsRetriedEvent(event.globalTxId());
			utxMetrics.countTxNumber(event, false, isRetried);
			utxMetrics.endMarkTxDuration(event);// end duration.
			return -1;
		}

		/**
		 * To save event only when the status of the global transaction is not paused.
		 * If not, return to client immediately, and client will do something, like sending again.
		 */
		boolean isPaused = isGlobalTxPaused(event.globalTxId());
		if (!isPaused) {
			CurrentThreadContext.put(event.globalTxId(), event);

			// We could intercept this method or use the Observer Design model on it, the aim is to handle some operations around it, but apparently, it is not easy to maintain code, so we reserved this idea.
			// 保存事件前，检查是否已经存在某子事务的某种事件，如果存在则不再保存。如：检测某事务超时后，若在下次检测时做出补偿处理，则会保存多条超时事件信息，为避免则先检测是否存在
			// 即使数据表结构对globalTxI、localTxId、type做了联合唯一约束，但最好还是先检测吧，尽量避免触发数据库唯一约束异常
			if (!eventRepository.checkIsExistsTxCompensatedEvent(event.type(), event.localTxId())) {
				try {
					if (SagaEndedEvent.name().equals(event.type())) {
						// 由于定时扫描器中检测超时会存在一定误差，如定时器中任务需3s完成，但某事务超时设置的是2秒，此时还未等对该事物进行检测，该事务就已经结束了，所以此处在正常结束前需检测是否超时
						if (eventRepository.checkIsTimeoutBeforeEnding(event.globalTxId())) {
							TxTimeout txTimeout = txTimeoutOf(event);
							try {
								timeoutRepository.save(txTimeout);// 结束全局事务前，检测到超时，保存超时记录
							} catch (Exception e) {
								LOG.error("Failed to save timeout {} in method 'TxConsistentService.handleSupportTxPause()'.", txTimeout, e);
							}
							eventRepository.save(toTxAbortedEvent(txTimeout));// 依据超时记录生成异常事件
							// 保存超时情况下的待补偿命令，当前超时全局事务下的所有应该补偿的子事件的待补偿命令 By Gannalyo
							commandRepository.saveWillCompensateCommandsForTimeout(event.globalTxId());
						}
					}

					eventRepository.save(event);

					if (TxAbortedEvent.name().equals(event.type()) && !event.globalTxId().equals(event.localTxId())) {
						// 当出现非超时的异常情况时记录待补偿命令，超时异常由定时器负责
						commandRepository.saveWillCompensateCommandsForException(event.globalTxId(), event.localTxId());
					}
				} catch (Exception e) {
					LOG.error("Failed to save event globalTxId {} localTxId {} type {}", event.globalTxId(), event.localTxId(), event.type(), e);
				}

				boolean isRetried = eventRepository.checkIsRetriedEvent(event.globalTxId());
				utxMetrics.countTxNumber(event, false, isRetried);
				utxMetrics.endMarkTxDuration(event);// end duration.

				// To send message to Kafka.
				kafkaMessageProducer.send(event);
			}

			return 1;
		}

		utxMetrics.endMarkTxDuration(event);// end duration.

		return 0;
	}

  private boolean isGlobalTxAborted(TxEvent event) {
    return !eventRepository.findTransactions(event.globalTxId(), TxAbortedEvent.name()).isEmpty();
  }
  
	public boolean isGlobalTxPaused(String globalTxId) {
		boolean isPaused = false;
		try {
			List<TxEvent> pauseContinueEventList = eventRepository.selectPausedAndContinueEvent(globalTxId);
			if (pauseContinueEventList != null && !pauseContinueEventList.isEmpty()) {
				isPaused = pauseContinueEventList.size() % 2 == 1;
				if (isPaused) {
					TxEvent event = pauseContinueEventList.get(0);
					if (event != null) {
						Date expiryTime = event.expiryTime();
						if (expiryTime.compareTo(new Date()) < 1) {
							try {
								// was due, create the event 'SagaAutoContinueEvent' to make event to continue running.
								eventRepository.save(new TxEvent(event.serviceName(),
										event.instanceId(), event.globalTxId(), event.localTxId(), event.parentTxId(),
										AdditionalEventType.SagaAutoContinuedEvent.name(), "", 0, "", 0, event.category(), event.payloads()));
								isPaused = false;
							} catch (Exception e) {
								isPaused = true;
								LOG.error("Fail to save the event 'SagaAutoContinuedEvent'.", e);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			LOG.error("Fail to execute the method 'isGlobalTxPaused'.", e);
		}
		return isPaused;
	}

	public Set<String> fetchLocalTxIdOfEndedGlobalTx(Set<String> localTxIdSet) {
		return eventRepository.selectEndedGlobalTx(localTxIdSet);
	}

	public boolean saveKafkaMessage(KafkaMessage message) {
		return kafkaMessageRepository.save(message);
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
}
