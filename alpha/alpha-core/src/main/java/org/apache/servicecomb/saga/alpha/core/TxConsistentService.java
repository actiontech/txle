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

import org.apache.servicecomb.saga.alpha.core.configcenter.IConfigCenterService;
import org.apache.servicecomb.saga.alpha.core.datadictionary.DataDictionaryItem;
import org.apache.servicecomb.saga.alpha.core.datadictionary.IDataDictionaryService;
import org.apache.servicecomb.saga.alpha.core.kafka.IKafkaMessageProducer;
import org.apache.servicecomb.saga.alpha.core.kafka.IKafkaMessageRepository;
import org.apache.servicecomb.saga.alpha.core.kafka.KafkaMessage;
import org.apache.servicecomb.saga.common.ConfigCenterType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

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
  private IConfigCenterService configCenterService;

	@Autowired
	UtxMetrics utxMetrics;

	@Autowired
	private IDataDictionaryService dataDictionaryService;

  private final List<String> types = Arrays.asList(TxStartedEvent.name(), SagaEndedEvent.name());
  private final Set<String> serverNameIdCategory = new HashSet<>();

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

		String globalTxId = event.globalTxId(), localTxId = event.localTxId(), type = event.type();
		if (isGlobalTxAborted(event)) {
			LOG.info("Transaction event {} rejected, because its parent with globalTxId {} was already aborted", type, globalTxId);
			utxMetrics.countTxNumber(event, false, event.retries() > 0);
			utxMetrics.endMarkTxDuration(event);// end duration.
			return -1;
		}

		/**
		 * To save event only when the status of the global transaction is not paused.
		 * If not, return to client immediately, and client will do something, like sending again.
		 */
		boolean isPaused = isGlobalTxPaused(globalTxId, type);
		if (!isPaused) {
			CurrentThreadContext.put(globalTxId, event);

			// We could intercept this method or use the Observer Design model on it, the aim is to handle some operations around it, but apparently, it is not easy to maintain code, so we reserved this idea.
			// 保存事件前，检查是否已经存在某子事务的某种事件，如果存在则不再保存。如：检测某事务超时后，若在下次检测时做出补偿处理，则会保存多条超时事件信息，为避免则先检测是否存在
			// 即使数据表结构对globalTxI、localTxId、type做了联合唯一约束，但最好还是先检测吧，尽量避免触发数据库唯一约束异常
//			if (!eventRepository.checkIsExistsTxCompensatedEvent(globalTxId, localTxId, type)) {
				try {
					eventRepository.save(event);
					if (SagaStartedEvent.name().equals(type)) {
					    // 当有新的全局事务时，设置最小id查询次数加1，即需要查询最小事件id
                        EventScanner.unendedMinEventIdSelectCount.incrementAndGet();
						this.putServerNameIdCategory(event);
					} else if (TxStartedEvent.name().equals(type)) {
						this.putServerNameIdCategory(event);
					}

						if (TxEndedEvent.name().equals(type)) {// 此处继续检测超时的意义是，如果超时，则不再继续执行全局事务中此子事务后面其它子事务
						// 若定时器检测超时后结束了当前全局事务，但超时子事务的才刚刚完成，此时检测全局事务是否已经终止，如果终止，则补偿当前刚刚完成的子事务
						if (isGlobalTxAborted(event)) {
							commandRepository.saveWillCompensateCmdForCurSubTx(globalTxId, localTxId);
						} else {
							// 由于定时扫描器中检测超时会存在一定误差，如定时器中任务需3s完成，但某事务超时设置的是2秒，此时还未等对该事物进行检测，该事务就已经结束了，所以此处在正常结束前需检测是否超时
							TxEvent unhandleTimeoutEvent = eventRepository.findTimeoutEventsBeforeEnding(globalTxId);// 如果有值，说明在EventScanner中已检测到并处理了
							if (unhandleTimeoutEvent != null) {
								// ps: 在未保存event前，将其转换成timeout，timeout中将无法获取到event的id值(默认为-1)，故上一行代码查询已保存的超时事件记录
								TxTimeout txTimeout = txTimeoutOf(unhandleTimeoutEvent);
								try {
									LOG.debug("TxConsistentService Detected the Timeout {}.", txTimeout);
									timeoutRepository.save(txTimeout);// 结束全局事务前，检测到超时，保存超时记录
									TxEvent abortedEvent = toTxAbortedEvent(txTimeout);
									if (!eventRepository.checkIsExistsTxCompensatedEvent(globalTxId, localTxId, abortedEvent.type())) {
										eventRepository.save(abortedEvent);// 依据超时记录生成异常事件
									}
								} catch (Exception e) {
									LOG.error("Failed to save timeout {} in method 'TxConsistentService.handleSupportTxPause()'.", txTimeout, e);
								} finally {
									// 保存超时情况下的待补偿命令，当前超时全局事务下的所有应该补偿的子事件的待补偿命令 By Gannalyo
									commandRepository.saveWillCompensateCommandsForTimeout(globalTxId);
								}
							}
						}
					}

					if (TxAbortedEvent.name().equals(type)) {
						// 验证是否最终异常，即排除非最后一次重试时的异常
						if (eventRepository.checkTxIsAborted(event.globalTxId(), event.localTxId())) {
							if (!globalTxId.equals(localTxId)) {
								// 当出现非超时的异常情况时记录待补偿命令，超时异常由定时器负责
								// 带有超时的子事务执行失败时，本地事务回滚，记录异常事件【后】，被检测为超时，则该失败的子事务又被回滚一次
								// 解决办法：检测超时SQL追加【无TxAbortedEvent条件】
								// 带有超时的子事务执行失败时，本地事务回滚，记录异常事件【前】，被检测为超时，则该失败的子事务又被回滚一次
								// 解决办法：失败时本地会立即将global和local的id记录到缓存中，后续超时补偿会先对比该缓存，不存在再补偿
								// 带有超时的子事务执行失败前，定时器检测到超时并且进行了补偿，之后子事务中执行失败，又进行了本地回滚，即多回滚了一次
								// 解决办法：超时只对已完成的子事务进行补偿，未完成的子事务，如果后续失败了则无需任何操作，如果成功结束，则在结束时会检测全局事务异常或超时，如果全局事务已终止了，则回滚当前成功完成的子事务
								commandRepository.saveWillCompensateCommandsForException(globalTxId, localTxId);
							} else {
								// 说明是全局事务异常终止
								commandRepository.saveWillCompensateCommandsWhenGlobalTxAborted(globalTxId);
								// To save SagaEndedEvent.
								eventRepository.save(new TxEvent(event.serviceName(), event.instanceId(), event.globalTxId(), event.globalTxId(), null, SagaEndedEvent.name(), "", event.category(), new byte[0]));
							}
						}
					}
				} catch (Exception e) {
					LOG.error("Failed to save event globalTxId {} localTxId {} type {}", globalTxId, localTxId, type, e);
				}

				utxMetrics.countTxNumber(event, false, event.retries() > 0);
				utxMetrics.endMarkTxDuration(event);// end duration.

				// To send message to Kafka.
				kafkaMessageProducer.send(event);
//			}

			return 1;
		}

		utxMetrics.endMarkTxDuration(event);// end duration.

		return 0;
	}

  private boolean isGlobalTxAborted(TxEvent event) {
	if (SagaStartedEvent.name().equals(event.type())) {
		return false;
	}
//    return !eventRepository.findTransactions(event.globalTxId(), TxAbortedEvent.name()).isEmpty();
	// 先查询是否含Aborted事件，如果含有再确定是否为重试情况，而不是一下子都确定好，因为存在异常事件的不多，这样性能上会快些
	TxEvent abortedTxEvent = eventRepository.selectAbortedTxEvent(event.globalTxId());
	if (abortedTxEvent != null) {
		if (abortedTxEvent.globalTxId().equals(abortedTxEvent.localTxId())) return true;// 说明全局事务异常，否则说明子事务异常，继续验证是否重试中的异常还是重试完成最终异常
		// 	验证是否最终异常，即排除非最后一次重试时的异常
		return eventRepository.checkTxIsAborted(event.globalTxId(), event.localTxId());
	}
  	return false;
  }
  
	public boolean isGlobalTxPaused(String globalTxId, String type) {
		if (SagaEndedEvent.name().equals(type)) {
			return false;
		}
		boolean isPaused = false;
		try {
			boolean enabledTx = configCenterService.isEnabledTx(null, null, ConfigCenterType.PauseGlobalTx);
			if (enabledTx) {
				return true;
			}

			// 由于暂停事务可能性极小且selectPausedAndContinueEvent查询较慢，故先快速查询是否有暂停或暂停自动恢复的事件
			List<String> typeList = eventRepository.selectAllTypeByGlobalTxId(globalTxId);
			if (typeList == null || typeList.isEmpty()) {
				return false;
			}

			AtomicBoolean isContainPauseEvent = new AtomicBoolean(false);
			typeList.forEach(t -> {
				if (AdditionalEventType.SagaPausedEvent.name().equals(t) || AdditionalEventType.SagaAutoContinuedEvent.name().equals(t)) {
					isContainPauseEvent.set(true);
				}
			});
			if (!isContainPauseEvent.get()) {
				return false;
			}

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

	private void putServerNameIdCategory(TxEvent event) {
		final String globalTxServer = "global-tx-server-info";
		final String server_nic = event.serviceName() + "__" + event.instanceId() + "__" + event.category();
		boolean result = serverNameIdCategory.add(server_nic);
		if (result) {
			new Thread(() -> {
				int showOrder = dataDictionaryService.selectMaxShowOrder(globalTxServer);
				final DataDictionaryItem ddItem = new DataDictionaryItem(globalTxServer, event.serviceName(), event.instanceId(), event.category(), showOrder + 1, 1, "");
				dataDictionaryService.createDataDictionary(ddItem);
			}).start();
		}
	}
}
