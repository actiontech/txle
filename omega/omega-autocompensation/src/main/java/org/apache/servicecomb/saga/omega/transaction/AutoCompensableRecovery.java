/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction;

import org.apache.servicecomb.saga.common.ConfigCenterType;
import org.apache.servicecomb.saga.common.TxleConstants;
import org.apache.servicecomb.saga.omega.context.ApplicationContextUtil;
import org.apache.servicecomb.saga.omega.context.CurrentThreadOmegaContext;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.context.OmegaContextServiceConfig;
import org.apache.servicecomb.saga.omega.transaction.annotations.AutoCompensable;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.InvalidTransactionException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AutoCompensableRecovery is used to execute business logic once. The corresponding
 * events will report to alpha server before and after the execution of business
 * logic. If there are errors while executing the business logic, a
 * TxAbortedEvent will be reported to alpha.
 *
 * pre post request --------- 2.business logic --------- response \ |
 * 1.TxStartedEvent \ | 3.TxEndedEvent \ | ---------------------- alpha
 */
public class AutoCompensableRecovery implements AutoCompensableRecoveryPolicy {
	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	public Object apply(ProceedingJoinPoint joinPoint, AutoCompensable compensable,
			AutoCompensableInterceptor interceptor, OmegaContext context, String parentTxId, int retries, IAutoCompensateService autoCompensateService)
			throws Throwable {
		Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
		LOG.debug(TxleConstants.logDebugPrefixWithTime() + "Intercepting autoCompensable method {} with context {}", method.toString(), context);

		// String retrySignature = (retries != 0 || compensationSignature.isEmpty()) ? method.toString() : "";
		String retrySignature = "";
		boolean isProceed = false;
		boolean enabledTx = false;
		try {
			String localTxId = context.localTxId();

			// Recording current thread identify, globalTxId and localTxId, the aim is to relate auto-compensation SQL by current thread identify. By Gannalyo
			CurrentThreadOmegaContext.putThreadGlobalLocalTxId(new OmegaContextServiceConfig(context, true, false));

			AlphaResponse response = interceptor.preIntercept(parentTxId, TxleConstants.AUTO_COMPENSABLE_METHOD, compensable.timeout(),
					retrySignature, retries, joinPoint.getArgs());
			enabledTx = response.enabledTx();
			CurrentThreadOmegaContext.getContextFromCurThread().setIsEnabledAutoCompensateTx(enabledTx);

			Object result = joinPoint.proceed();
			isProceed = true;

			if (enabledTx) {
				if (response.aborted()) {
					context.setLocalTxId(parentTxId);
					throw new InvalidTransactionException(TxleConstants.LOG_ERROR_PREFIX + "Abort sub transaction " + localTxId
							+ " because global transaction " + context.globalTxId() + " has already aborted.");
				}

				// Remove cache after ending compensation preparation and business.
				CurrentThreadOmegaContext.clearCache();

				// To submit the TxEndedEvent.
				interceptor.postIntercept(parentTxId, TxleConstants.AUTO_COMPENSABLE_METHOD);
			}

			return result;
		} catch (InvalidTransactionException e) {
			throw e;
		} catch (Throwable e) {
			LOG.error(TxleConstants.LOG_ERROR_PREFIX + "Fail to proceed business, context {}, method {}", context, method.toString(), e);

			boolean isFaultTolerant = ApplicationContextUtil.getApplicationContext().getBean(MessageSender.class).readConfigFromServer(ConfigCenterType.CompensationFaultTolerant.toInteger(), context.category()).getStatus();
			if (enabledTx && !isFaultTolerant) {
				interceptor.onError(parentTxId, TxleConstants.AUTO_COMPENSABLE_METHOD, e);
			}

			// In case of exception, to execute business if it is not proceed yet when the fault-tolerant degradation is enabled fro global transaction.
			if (!isProceed && isFaultTolerant) {
				return joinPoint.proceed();
			}

			// So far, it cannot call auto-compensation method immediately due to every Omega has itself DB-link. By Gannalyo
			throw e;
		} finally {
			clearDataSourceMappingCache(interceptor);
		}
	}

	private void clearDataSourceMappingCache(AutoCompensableInterceptor interceptor) {
		try {
			// To clear cache for datasource mapping when the cache size is more than fifty.
			Set<String> localTxIdSet = DataSourceMappingCache.getCacheLocalTxIdSet();
			if (localTxIdSet != null && localTxIdSet.size() > 0) {
				// Open a new thread for saving time of the main thread.
				// TODO batch to clear
				ExecutorService executorService = Executors.newSingleThreadExecutor();
				executorService.execute(() -> DataSourceMappingCache.clear(interceptor.fetchLocalTxIdOfEndedGlobalTx(localTxIdSet)));
				executorService.shutdown();
			}
		} catch (Exception e) {
			LOG.error(TxleConstants.LOG_ERROR_PREFIX + "Failed to clear cache for the datasource mapping. " + e.getMessage());
		}
	}

}
