/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import org.apache.servicecomb.saga.common.TxleConstants;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.transaction.annotations.AutoCompensable;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An interceptor for some methods which annotate the AutoCompensable annotation.
 *
 * @author Gannalyo
 * @since 2018-07-30
 */
@Aspect
public class AutoCompensableAspect {

	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final OmegaContext context;

	private IAutoCompensateService autoCompensateService;
	private AutoCompensableInterceptor autoCompensableInterceptor;

	public AutoCompensableAspect(MessageSender sender, OmegaContext context, AutoCompensateService autoCompensateService) {
		this.context = context;
		this.autoCompensateService = autoCompensateService;
		this.autoCompensableInterceptor = new AutoCompensableInterceptor(context, sender);
	}

	@Around("execution(@org.apache.servicecomb.saga.omega.transaction.annotations.AutoCompensable * *(..)) && @annotation(autoCompensable)")
	Object advise(ProceedingJoinPoint joinPoint, AutoCompensable autoCompensable) throws Throwable {
		Method method = null;
		try {
			method = ((MethodSignature) joinPoint.getSignature()).getMethod();
			String localTxId = context.newLocalTxId();
			LOG.debug(TxleConstants.logDebugPrefixWithTime() + "Updated context [{}] for autoCompensable method [{}] ", context, method.toString());

			int retries = autoCompensable.retries();
			AutoCompensableRecoveryPolicy recoveryPolicy = AutoCompensableRecoveryPolicyFactory.getRecoveryPolicy(retries);

			return recoveryPolicy.apply(joinPoint, autoCompensable, autoCompensableInterceptor, context, localTxId, retries, autoCompensateService);
		} catch (Throwable e) {
			LOG.error(TxleConstants.LOG_ERROR_PREFIX + "Fail to execute AutoCompensableAspect, context [{}], method [{}]", context,
					method == null ? "" : method.toString(), e);
			throw e;
		}
	}

}
