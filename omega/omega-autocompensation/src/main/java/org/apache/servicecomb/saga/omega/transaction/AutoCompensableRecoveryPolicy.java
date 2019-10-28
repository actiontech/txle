/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction;

import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.transaction.annotations.AutoCompensable;
import org.aspectj.lang.ProceedingJoinPoint;

public interface AutoCompensableRecoveryPolicy {
  Object apply(ProceedingJoinPoint joinPoint, AutoCompensable compensable, AutoCompensableInterceptor interceptor,
      OmegaContext context, String parentTxId, int retries, IAutoCompensateService autoCompensateService) throws Throwable;
}
