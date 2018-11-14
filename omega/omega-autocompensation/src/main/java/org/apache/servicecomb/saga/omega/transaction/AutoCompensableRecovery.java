package org.apache.servicecomb.saga.omega.transaction;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import javax.transaction.InvalidTransactionException;

import org.apache.servicecomb.saga.omega.context.CurrentThreadOmegaContext;
import org.apache.servicecomb.saga.omega.context.OmegaContextServiceConfig;
import org.apache.servicecomb.saga.omega.context.UtxConstants;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.transaction.annotations.AutoCompensable;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		LOG.debug(UtxConstants.logDebugPrefixWithTime() + "Intercepting autoCompensable method {} with context {}", method.toString(), context);

		String compensationSignature = AutoCompensableConstants.AUTO_COMPENSABLE_METHOD;

		// String retrySignature = (retries != 0 || compensationSignature.isEmpty()) ? method.toString() : "";
		String retrySignature = "";
		
		String localTxId = context.localTxId();

		// Recoding current thread identify, globalTxId and localTxId, the aim is to relate auto-compensation SQL by current thread identify. By Gannalyo
		CurrentThreadOmegaContext.putThreadGlobalLocalTxId(new OmegaContextServiceConfig(context));

		AlphaResponse response = interceptor.preIntercept(parentTxId, compensationSignature, compensable.timeout(),
				retrySignature, retries, joinPoint.getArgs());
		if (response.aborted()) {
			context.setLocalTxId(parentTxId);
			throw new InvalidTransactionException(UtxConstants.LOG_ERROR_PREFIX + "Abort sub transaction " + localTxId
					+ " because global transaction " + context.globalTxId() + " has already aborted.");
		}

		try {
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
		}
	}

}
