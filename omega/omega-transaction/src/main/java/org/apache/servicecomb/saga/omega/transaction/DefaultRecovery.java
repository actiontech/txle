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

package org.apache.servicecomb.saga.omega.transaction;

import org.apache.servicecomb.saga.common.ConfigCenterType;
import org.apache.servicecomb.saga.omega.context.ApplicationContextUtil;
import org.apache.servicecomb.saga.omega.context.CurrentThreadOmegaContext;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.context.OmegaContextServiceConfig;
import org.apache.servicecomb.saga.omega.transaction.annotations.Compensable;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.InvalidTransactionException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

/**
 * DefaultRecovery is used to execute business logic once.
 * The corresponding events will report to alpha server before and after the execution of business logic.
 * If there are errors while executing the business logic, a TxAbortedEvent will be reported to alpha.
 *
 *                 pre                       post
 *     request --------- 2.business logic --------- response
 *                 \                          |
 * 1.TxStartedEvent \                        | 3.TxEndedEvent
 *                   \                      |
 *                    ----------------------
 *                            alpha
 */
public class DefaultRecovery implements RecoveryPolicy {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Override
  public Object apply(ProceedingJoinPoint joinPoint, Compensable compensable, CompensableInterceptor interceptor,
      OmegaContext context, String parentTxId, int retries) throws Throwable {
    Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
    LOG.debug("Intercepting compensable method {} with context {}", method.toString(), context);
    String compensationSignature = compensable.compensationMethod().isEmpty() ? "" : compensationMethodSignature(joinPoint, compensable, method);
    String retrySignature = (retries != 0 || compensationSignature.isEmpty()) ? method.toString() : "";
    boolean isProceed = false;
    boolean enabledTx = false;

    try {
      // Recoding current thread identify, globalTxId and localTxId, the aim is to relate auto-compensation SQL by current thread identify. By Gannalyo
      CurrentThreadOmegaContext.putThreadGlobalLocalTxId(new OmegaContextServiceConfig(context, false, false));

      AlphaResponse response = interceptor.preIntercept(parentTxId, compensationSignature, compensable.timeout(), retrySignature, retries, joinPoint.getArgs());
      enabledTx = response.enabledTx();

      isProceed = true;
      Object result = null;
      if (!response.aborted()) {
        result = joinPoint.proceed();
      }

      if (enabledTx) {
        if (response.aborted()) {
          String abortedLocalTxId = context.localTxId();
          context.setLocalTxId(parentTxId);
          throw new InvalidTransactionException("Abort sub transaction " + abortedLocalTxId + " because global transaction " + context.globalTxId() + " has already aborted.");
        }

        CurrentThreadOmegaContext.clearCache();
        interceptor.postIntercept(parentTxId, compensationSignature);
      }

      return result;
    } catch (InvalidTransactionException ite) {
      throw  ite;
    } catch (Throwable throwable) {
      boolean isFaultTolerant = ApplicationContextUtil.getApplicationContext().getBean(MessageSender.class).readConfigFromServer(ConfigCenterType.CompensationFaultTolerant.toInteger(), context.category()).getStatus();
      if (enabledTx && !isFaultTolerant) {
        interceptor.onError(parentTxId, compensationSignature, throwable);
      }

      // In case of exception, to execute business if it is not proceed yet when the fault-tolerant degradation is enabled fro global transaction.
      if (!isProceed && isFaultTolerant) {
          return joinPoint.proceed();
      }
      throw throwable;
    }
  }

  String compensationMethodSignature(ProceedingJoinPoint joinPoint, Compensable compensable, Method method)
      throws NoSuchMethodException {
    return joinPoint.getTarget().getClass().getDeclaredMethod(compensable.compensationMethod(), method.getParameterTypes()).toString();
  }
}
