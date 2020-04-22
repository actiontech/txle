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

import com.actionsky.txle.cache.ITxleConsistencyCache;
import com.actionsky.txle.enums.GlobalTxStatus;
import com.actionsky.txle.grpc.interfaces.eventaddition.ITxEventAdditionService;
import com.actionsky.txle.grpc.interfaces.eventaddition.TxEventAddition;
import org.apache.servicecomb.saga.alpha.core.kafka.IKafkaMessageRepository;
import org.apache.servicecomb.saga.alpha.core.kafka.KafkaMessage;
import org.apache.servicecomb.saga.common.ConfigCenterType;
import org.apache.servicecomb.saga.common.TxleConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
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
  private IKafkaMessageRepository kafkaMessageRepository;

    @Resource(name = "txleMysqlCache")
    @Autowired
    private ITxleConsistencyCache consistencyCache;

	@Autowired
	private ITxEventAdditionService eventAdditionService;

	private final List<String> types = Arrays.asList(TxEndedEvent.name(), TxAbortedEvent.name());

  public TxConsistentService(TxEventRepository eventRepository, CommandRepository commandRepository, TxTimeoutRepository timeoutRepository) {
    this.eventRepository = eventRepository;
    this.commandRepository = commandRepository;
    this.timeoutRepository = timeoutRepository;
  }

  public boolean handle(TxEvent event) {
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
		String globalTxId = event.globalTxId(), localTxId = event.localTxId(), type = event.type();
		StringBuilder globalTxStatusCache = new StringBuilder();
		boolean isAborted = false;
		if (!types.contains(type)) {
			isAborted = isGlobalTxAborted(event, globalTxStatusCache);
			if (isAborted) {
				LOG.info("Transaction event {} rejected, because its parent with globalTxId {} was already aborted", type, globalTxId);
				// Should return wrong result in case of aborted transaction, even though all of businesses were completed.
				if (SagaEndedEvent.name().equals(type)) {
					eventRepository.save(event);
				}
				return -1;
			}
		}

		/**
		 * To save event only when the status of the global transaction is not paused.
		 * If not, return to client immediately, and client will do something, like sending again.
		 */
		boolean isPaused = isGlobalTxPaused(event, type, globalTxStatusCache.toString());
		if (!isPaused) {
			CurrentThreadContext.put(globalTxId, event);

			// We could intercept this method or use the Observer Design model on it, the aim is to handle some operations around it, but apparently, it is not easy to maintain code, so we reserved this idea.
			// 保存事件前，检查是否已经存在某子事务的某种事件，如果存在则不再保存。如：检测某事务超时后，若在下次检测时做出补偿处理，则会保存多条超时事件信息，为避免则先检测是否存在
			try {
				eventRepository.save(event);

				// 此处继续检测超时的意义是，如果超时，则不再继续执行全局事务中此子事务后面其它子事务
				if (TxEndedEvent.name().equals(type)) {
					// 若定时器检测超时后结束了当前全局事务，但超时子事务的才刚刚完成，此时检测全局事务是否已经终止，如果终止，则补偿当前刚刚完成的子事务
					if (isAborted) {
						// subA ok, timeout, compensate subA, subB ok without exception(need to save ended even though aborted), compensate subB.
						// 此处可以非同步操作，其内主要是保存补偿命令和事件，补偿操作不在其内执行而是由扫描器执行，且事务已经处于异常状态，保存的补偿命令和事件对当前事务的后续操作无影响，故额外线程执行
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
								}
							} catch (Exception e) {
								LOG.error("Failed to save timeout {} in method 'TxConsistentService.handleSupportTxPause()'.", txTimeout, e);
							} finally {
								// 保存超时情况下的待补偿命令，当前超时全局事务下的所有应该补偿的子事件的待补偿命令 By Gannalyo
								commandRepository.saveWillCompensateCommandsForTimeout(globalTxId);
							}
						}
					}
				} else if (TxAbortedEvent.name().equals(type)) {
					// 验证是否最终异常，即排除非最后一次重试时的异常。如果全局事务标识等于子事务标识情况的异常，说明是全局事务异常。否则说明子事务异常，则需验证是否是子事务的最终异常。
					if (globalTxId.equals(localTxId) || eventRepository.checkTxIsAborted(globalTxId, localTxId)) {
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
			}

			return 1;
		}

		return 0;
	}

  private boolean isGlobalTxAborted(TxEvent event, StringBuilder globalTxStatusCache) {
	if (SagaStartedEvent.name().equals(event.type())) {
		return false;
	}
	String value = consistencyCache.getValueByCacheKey(TxleConstants.constructTxStatusCacheKey(event.globalTxId()));
	globalTxStatusCache.append(value);
	return value != null && GlobalTxStatus.Aborted.toString().equals(value);
  }

	private boolean isGlobalTxPaused(TxEvent event, String type, String globalTxStatusCache) {
		if (SagaEndedEvent.name().equals(type)) {
			return false;
		}

        try {
            // 验证是否暂停所有事务，有值代表暂停所有，非暂停所有情况无值
            if (consistencyCache.getBooleanValue(event.instanceId(), event.category(), ConfigCenterType.PauseGlobalTx)) {
                return true;
            }

            // 验证当前全局事务是否暂停，globalTxStatusCache与上面验证异常共用同一个查询结果，节省一次查询性能
            return "paused".equals(globalTxStatusCache);
        } catch (Exception e) {
            LOG.error("Failed to execute the method 'isGlobalTxPaused'.", e);
        }
        return false;
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
			eventRepository.save(event);
		} catch (Exception e) {
			LOG.error("Failed to register global transaction [{}].", event, e);
			return false;
		}
		return true;
	}

	public boolean registerSubTx(TxEvent subTxEvent, TxEventAddition subTxEventAddition) {
		try {
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
		}
		return true;
	}

	public boolean checkIsExistsEventType(String globalTxId, String localTxId, String type) {
		return eventRepository.checkIsExistsEventType(globalTxId, localTxId, type);
	}

}
