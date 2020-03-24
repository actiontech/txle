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

package org.apache.servicecomb.saga.alpha.core;

import com.actionsky.txle.grpc.interfaces.eventaddition.ITxEventAdditionService;
import com.actionsky.txle.grpc.interfaces.eventaddition.TxEventAddition;
import org.apache.servicecomb.saga.alpha.core.cache.ITxleCache;
import org.apache.servicecomb.saga.alpha.core.kafka.IKafkaMessageRepository;
import org.apache.servicecomb.saga.alpha.core.kafka.KafkaMessage;
import org.apache.servicecomb.saga.common.ConfigCenterType;
import org.apache.servicecomb.saga.common.TxleConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.servicecomb.saga.alpha.core.TaskStatus.NEW;
import static org.apache.servicecomb.saga.common.EventType.*;

public class TxConsistentService {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final TxEventRepository eventRepository;
  private final CommandRepository commandRepository;
  private final TxTimeoutRepository timeoutRepository;

  @Autowired
  private IKafkaMessageRepository kafkaMessageRepository;

	@Autowired
    private TxleMetrics txleMetrics;

	@Autowired
	private ITxleCache txleCache;

	@Autowired
	private ITxEventAdditionService eventAdditionService;

	private final List<String> types = Arrays.asList(TxEndedEvent.name(), TxAbortedEvent.name());

  public TxConsistentService(TxEventRepository eventRepository, CommandRepository commandRepository, TxTimeoutRepository timeoutRepository) {
    this.eventRepository = eventRepository;
    this.commandRepository = commandRepository;
    this.timeoutRepository = timeoutRepository;
  }

  public boolean handle(TxEvent event) {
	  // start duration.
	  txleMetrics.startMarkTxDuration(event);
	  if (types.contains(event.type()) && isGlobalTxAborted(event)) {
		  LOG.info("Transaction event {} rejected, because its parent with globalTxId {} was already aborted",
				  event.type(), event.globalTxId());
		  // end duration.
		  txleMetrics.endMarkTxDuration(event);
		  return false;
	  }

	  eventRepository.save(event);
	  // end duration.
	  txleMetrics.endMarkTxDuration(event);

	  return true;
  }

	/**
	 * handle the event. support transaction pause/continue/auto-continue.
	 *
	 * @param event event for global/sub transaction
	 * @return result
	 * @author Gannalyo
	 */
	public int handleSupportTxPause(TxEvent event) {
		// start duration.
		txleMetrics.startMarkTxDuration(event);
		// child transaction count
		txleMetrics.countChildTxNumber(event);

		String globalTxId = event.globalTxId(), localTxId = event.localTxId(), type = event.type();
		if (!types.contains(type) && isGlobalTxAborted(event)) {
			LOG.info("Transaction event {} rejected, because its parent with globalTxId {} was already aborted", type, globalTxId);
			// Should return wrong result in case of aborted transaction, even though all of businesses were completed.
			if (SagaEndedEvent.name().equals(type)) {
				eventRepository.save(event);
			}
			txleMetrics.countTxNumber(event, false, event.retries() > 0);
			// end duration.
			txleMetrics.endMarkTxDuration(event);
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
			try {
				eventRepository.save(event);

				// 此处继续检测超时的意义是，如果超时，则不再继续执行全局事务中此子事务后面其它子事务
				if (TxEndedEvent.name().equals(type)) {
					// 若定时器检测超时后结束了当前全局事务，但超时子事务的才刚刚完成，此时检测全局事务是否已经终止，如果终止，则补偿当前刚刚完成的子事务
					if (isGlobalTxAborted(event)) {
						// subA ok, timeout, compensate subA, subB ok without exception(need to save ended even though aborted), compensate subB.
						commandRepository.saveWillCompensateCmdForCurSubTx(globalTxId, localTxId);
					} else {
						// 由于定时扫描器中检测超时会存在一定误差，如定时器中任务需3s完成，但某事务超时设置的是2秒，此时还未等对该事物进行检测，该事务就已经结束了，所以此处在正常结束前需检测是否超时
						// 如果有值，说明在EventScanner中已检测到并处理了
						TxEvent unhandleTimeoutEvent = eventRepository.findTimeoutEventsBeforeEnding(globalTxId);
						if (unhandleTimeoutEvent != null) {
							// ps: 在未保存event前，将其转换成timeout，timeout中将无法获取到event的id值(默认为-1)，故上一行代码查询已保存的超时事件记录
							TxTimeout txTimeout = txTimeoutOf(unhandleTimeoutEvent);
							try {
								LOG.debug("TxConsistentService Detected the Timeout {}.", txTimeout);
								// 结束全局事务前，检测到超时，保存超时记录
								timeoutRepository.save(txTimeout);
								TxEvent abortedEvent = toTxAbortedEvent(txTimeout);
								if (!eventRepository.checkIsExistsEventType(globalTxId, localTxId, abortedEvent.type())) {
									// 依据超时记录生成异常事件
									eventRepository.save(abortedEvent);
									txleCache.putDistributedTxAbortStatusCache(globalTxId, true, 2);
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
					// 验证是否最终异常，即排除非最后一次重试时的异常。如果全局事务标识等于子事务标识情况的异常，说明是全局事务异常。否则说明子事务异常，则需验证是否是子事务的最终异常。
					if (globalTxId.equals(localTxId) || eventRepository.checkTxIsAborted(globalTxId, localTxId)) {
						txleCache.putDistributedTxAbortStatusCache(globalTxId, true, 2);
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
							TxEvent sagaEndedEvent = new TxEvent(event.serviceName(), event.instanceId(), globalTxId, globalTxId, null, SagaEndedEvent.name(), "", event.category(), new byte[0]);
							eventRepository.save(sagaEndedEvent);
						}
					}
				}
			} catch (Exception e) {
				LOG.error("Failed to save event globalTxId {} localTxId {} type {}", globalTxId, localTxId, type, e);
			} finally {
				txleMetrics.countTxNumber(event, false, event.retries() > 0);
				// end duration.
				txleMetrics.endMarkTxDuration(event);
			}

			return 1;
		}

		// end duration.
		txleMetrics.endMarkTxDuration(event);

		return 0;
	}

  private boolean isGlobalTxAborted(TxEvent event) {
	if (SagaStartedEvent.name().equals(event.type())) {
		return false;
	}
//    return !eventRepository.findTransactions(event.globalTxId(), TxAbortedEvent.name()).isEmpty();
    return txleCache.getTxAbortStatus(event.globalTxId());
    // 先查询是否含Aborted事件，如果含有再确定是否为重试情况，而不是一下子都确定好，因为存在异常事件的不多，这样性能上会快些
    /*TxEvent abortedTxEvent = eventRepository.selectAbortedTxEvent(event.globalTxId());
	if (abortedTxEvent != null) {
		if (abortedTxEvent.globalTxId().equals(abortedTxEvent.localTxId())) {
			// 说明全局事务异常，否则说明子事务异常，继续验证是否重试中的异常还是重试完成最终异常
			return true;
		}
		// 	验证是否最终异常，即排除非最后一次重试时的异常
		return eventRepository.checkTxIsAborted(abortedTxEvent.globalTxId(), abortedTxEvent.localTxId());
	}
  	return false;*/
  }

	public boolean isGlobalTxPaused(String globalTxId, String type) {
		if (SagaEndedEvent.name().equals(type)) {
			return false;
		}
		boolean isPaused = false;
		try {
            final String pauseAllGlobalTxKey = TxleConstants.constructConfigCacheKey(null, null, ConfigCenterType.PauseGlobalTx.toInteger());
            if (txleCache.getConfigCache().getOrDefault(pauseAllGlobalTxKey, false)) {
                // paused all global transactions.
                return true;
            }

            if (!txleCache.getTxSuspendStatus(globalTxId)) {
                // return false directly if it's not suspended.
                return false;
            } else {
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
            }

            // If paused, continue to verify the expire for auto-recovery
            List<TxEvent> pauseContinueEventList = eventRepository.selectPausedAndContinueEvent(globalTxId);
            if (pauseContinueEventList != null && !pauseContinueEventList.isEmpty()) {
                TxEvent event = pauseContinueEventList.get(0);
                if (event != null && AdditionalEventType.SagaAutoContinuedEvent.name().equals(event.type())) {
                    isPaused = true;
                    if (event.expiryTime().compareTo(new Date()) < 1) {
                        try {
                            // was due, create the event 'SagaAutoContinueEvent' to make event to continue running.
                            eventRepository.save(new TxEvent(event.serviceName(), event.instanceId(), event.globalTxId(), event.localTxId(), event.parentTxId(),
                                    AdditionalEventType.SagaAutoContinuedEvent.name(), "", 0, "", 0, event.category(), event.payloads()));
                            isPaused = false;
                        } catch (Exception e) {
                            isPaused = true;
                            LOG.error("Fail to save the event 'SagaAutoContinuedEvent'.", e);
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

	public boolean registerGlobalTx(TxEvent event) {
		try {
			txleMetrics.startMarkTxDuration(event);
			eventRepository.save(event);
		} catch (Exception e) {
			LOG.error("Failed to register global transaction [{}].", event, e);
			return false;
		} finally {
			txleMetrics.endMarkTxDuration(event);
		}
		return true;
	}

	public boolean registerSubTx(TxEvent subTxEvent, TxEventAddition subTxEventAddition) {
		try {
			txleMetrics.countChildTxNumber(subTxEvent);
			txleMetrics.startMarkTxDuration(subTxEvent);

			eventRepository.save(subTxEvent);

			if (subTxEventAddition != null) {
				eventAdditionService.save(subTxEventAddition);
			}

			if (TxCompensatedEvent.name().equals(subTxEvent.type())) {
				eventAdditionService.updateCompensateStatus(subTxEvent.instanceId(), subTxEvent.globalTxId(), subTxEvent.localTxId());
			}
		} catch (Exception e) {
			LOG.error("Failed to register global transaction [{}].", subTxEvent, e);
			return false;
		} finally {
			txleMetrics.countTxNumber(subTxEvent, false, subTxEvent.retries() > 0);
			txleMetrics.endMarkTxDuration(subTxEvent);
		}
		return true;
	}

	public boolean checkIsExistsEventType(String globalTxId, String localTxId, String type) {
		return eventRepository.checkIsExistsEventType(globalTxId, localTxId, type);
	}

}
