package org.apache.servicecomb.saga.omega.transaction;

import org.apache.servicecomb.saga.omega.context.CurrentThreadOmegaContext;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.context.OmegaContextServiceConfig;
import org.apache.servicecomb.saga.common.UtxConstants;
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
		String compensationSignature = AutoCompensableConstants.AUTO_COMPENSABLE_METHOD;
		try {
			Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
			LOG.debug(UtxConstants.logDebugPrefixWithTime() + "Intercepting autoCompensable method {} with context {}", method.toString(), context);

			// String retrySignature = (retries != 0 || compensationSignature.isEmpty()) ? method.toString() : "";
			String retrySignature = "";

			String localTxId = context.localTxId();

			// Recoding current thread identify, globalTxId and localTxId, the aim is to relate auto-compensation SQL by current thread identify. By Gannalyo
			CurrentThreadOmegaContext.putThreadGlobalLocalTxId(new OmegaContextServiceConfig(context, true));

			AlphaResponse response = interceptor.preIntercept(parentTxId, compensationSignature, compensable.timeout(),
					retrySignature, retries, joinPoint.getArgs());
			if (response.aborted()) {
				context.setLocalTxId(parentTxId);
				throw new InvalidTransactionException(UtxConstants.LOG_ERROR_PREFIX + "Abort sub transaction " + localTxId
						+ " because global transaction " + context.globalTxId() + " has already aborted.");
			}

			Object result = null;
			try {
				// To execute business logic.
				result = joinPoint.proceed();
			} catch (Exception e) {
				LOG.error(UtxConstants.LOG_ERROR_PREFIX + "Fail to proceed business, context {}, method {}", context, method.toString(), e);
				throw e;
			}
			
			CurrentThreadOmegaContext.clearCache();
			
			// To submit the TxEndedEvent.
			interceptor.postIntercept(parentTxId, compensationSignature);

			return result;
		} catch (Throwable e) {
			interceptor.onError(parentTxId, compensationSignature, e);
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
			if (localTxIdSet != null && localTxIdSet.size() > 50) {
				// Open a new thread for saving time of the main thread.
				ExecutorService executorService = Executors.newSingleThreadExecutor();
				executorService.execute(() -> {
					DataSourceMappingCache.clear(interceptor.fetchLocalTxIdOfEndedGlobalTx(localTxIdSet));
				});
				executorService.shutdown();
			}
		} catch (Exception e) {
			LOG.error(UtxConstants.LOG_ERROR_PREFIX + "Failed to clear cache for the datasource mapping. " + e.getMessage());
		}
	}

}
