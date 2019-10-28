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

	private final List<String> types = Arrays.asList(TxEndedEvent.name(), TxAbortedEvent.name(), SagaEndedEvent.name());

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
			try {
				CurrentThreadContext.put(globalTxId, event);

				eventRepository.save(event);

				if (TxEndedEvent.name().equals(type)) {
					// There are two places for checking timeout. Here and the scheduler of the 'EventScanner' file.
					// Due to they're not synchronized, hence, is's possible that global transaction is aborted here after timeout is detected by the scheduler.
					if (isGlobalTxAborted(event)) {
						// In the timeout abort case, other passed sub-transactions were compensated when the scheduler checked timeout out.
						// Current sub-transaction need to be compensated due to it is also passed.
						// subA ok, timeout, compensate subA, subB ok without exception(need to save ended even though aborted), compensate subB.
						commandRepository.saveWillCompensateCmdForCurSubTx(globalTxId, localTxId);
					} else {
						// If not aborted, here have to check timeout again to avoid the latency of the timeout scheduler.
						TxEvent unhandleTimeoutEvent = eventRepository.findTimeoutEventsBeforeEnding(globalTxId);
						if (unhandleTimeoutEvent != null) {
							TxTimeout txTimeout = txTimeoutOf(unhandleTimeoutEvent);
							try {
								LOG.debug("TxConsistentService Detected the Timeout {}.", txTimeout);
								timeoutRepository.save(txTimeout);
								TxEvent abortedEvent = toTxAbortedEvent(txTimeout);
								if (!eventRepository.checkIsExistsEventType(globalTxId, localTxId, abortedEvent.type())) {
									eventRepository.save(abortedEvent);
									txleCache.putDistributedTxAbortStatusCache(globalTxId, true, 2);
								}
							} catch (Exception e) {
								LOG.error("Failed to save timeout {} in method 'TxConsistentService.handleSupportTxPause()'.", txTimeout, e);
							} finally {
								commandRepository.saveWillCompensateCommandsForTimeout(globalTxId);
							}
						}
					}
				}

				if (TxAbortedEvent.name().equals(type)) {
					// To verify eventually abort (exclude some abort events which retried successfully).
					if (globalTxId.equals(localTxId) || eventRepository.checkTxIsAborted(globalTxId, localTxId)) {
						txleCache.putDistributedTxAbortStatusCache(globalTxId, true, 2);
						if (!globalTxId.equals(localTxId)) {
							commandRepository.saveWillCompensateCommandsForException(globalTxId, localTxId);
						} else {
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

		txleMetrics.endMarkTxDuration(event);

		return 0;
	}

	private boolean isGlobalTxAborted(TxEvent event) {
		if (SagaStartedEvent.name().equals(event.type())) {
			return false;
		}
		return txleCache.getTxAbortStatus(event.globalTxId());
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
				// It's unusual to pause global transaction. So, just search the list of transaction type first for saving performance.
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
}
