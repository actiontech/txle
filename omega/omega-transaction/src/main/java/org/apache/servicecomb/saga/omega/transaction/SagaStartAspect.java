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
 */

package org.apache.servicecomb.saga.omega.transaction;

import org.apache.servicecomb.saga.common.ConfigCenterType;
import org.apache.servicecomb.saga.omega.context.ApplicationContextUtil;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.context.annotations.SagaStart;
import org.apache.servicecomb.saga.omega.transaction.monitor.CompensableSqlMetrics;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;

@Aspect
public class SagaStartAspect {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final SagaStartAnnotationProcessor sagaStartAnnotationProcessor;

  private final OmegaContext context;

  public SagaStartAspect(MessageSender sender, OmegaContext context) {
    this.context = context;
    this.sagaStartAnnotationProcessor = new SagaStartAnnotationProcessor(context, sender);
  }

  @Around("execution(@org.apache.servicecomb.saga.omega.context.annotations.SagaStart * *(..)) && @annotation(sagaStart)")
  Object advise(ProceedingJoinPoint joinPoint, SagaStart sagaStart) throws Throwable {
    long a = System.nanoTime();
    Method method = null;
    boolean isProceed = false;
    try {
      initializeOmegaContext(sagaStart);
//      LOG.info("\r\n---- [{}] starting SagaStartedEvent, globalTxId = [{}], initialized context time [{}].", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new Date()), context.globalTxId(), ((System.nanoTime() - a) / 1000));
      method = ((MethodSignature) joinPoint.getSignature()).getMethod();

      AlphaResponse alphaResponse = sagaStartAnnotationProcessor.preIntercept(context.globalTxId(), method.toString(), sagaStart.timeout(), "", 0);
//      LOG.debug("Initialized context {} before execution of method {}", context, method.toString());
      if (!alphaResponse.enabledTx()) {
        CompensableSqlMetrics.setIsMonitorSql(ApplicationContextUtil.getApplicationContext().getBean(MessageSender.class).readConfigFromServer(ConfigCenterType.SqlMonitor.toInteger(), context.category()).getStatus());
      }
//      LOG.info("\r\n---- [{}] finished SagaStartedEvent, globalTxId = [{}].", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new Date()));

      // no matter following result.
      isProceed = true;
      Object result = joinPoint.proceed();

      if (alphaResponse.enabledTx()) {
//        LOG.info("\r\n---- [{}] starting SagaEndedEvent, globalTxId = [{}].", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new Date()), context.globalTxId());
        sagaStartAnnotationProcessor.postIntercept(context.globalTxId(), method.toString());
//        LOG.debug("Transaction with context {} has finished.", context);
//        LOG.info("\r\n---- [{}] finished SagaEndedEvent, globalTxId = [{}].", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new Date()), context.globalTxId());
      }

      return result;
    } catch (Throwable throwable) {
      boolean isFaultTolerant = ApplicationContextUtil.getApplicationContext().getBean(MessageSender.class).readConfigFromServer(ConfigCenterType.GlobalTxFaultTolerant.toInteger(), context.category()).getStatus();
      // We don't need to handle the OmegaException here
      if (!(throwable instanceof OmegaException) && !isFaultTolerant) {
        try {
          sagaStartAnnotationProcessor.onError(context.globalTxId(), method == null ? null : method.toString(), throwable);
          LOG.error("Transaction {} failed.", context.globalTxId());
        } catch (Throwable e) {
          throw e;
        }
      }

      // In case of exception, to execute business if it is not proceed yet when the fault-tolerant degradation is enabled for global transaction.
      // Fault-tolerant logic: 1.enabled fault-tolerant degradation 2.do not execute business yet
      if (!isProceed && isFaultTolerant) {
          return joinPoint.proceed();
      }
      throw throwable;
    } finally {
      context.clear();
//      LOG.info("\r\n---- [{}] exit cur request, globalTxId = [{}].", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new Date()), context.globalTxId());
    }
  }

  private void initializeOmegaContext(SagaStart sagaStart) {
    context.setLocalTxId(context.newGlobalTxId());
    context.setCategory(sagaStart.category());
  }
}
