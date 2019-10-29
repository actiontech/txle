/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.core.configcenter;

import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

@Aspect
public class DegradationConfigAspect {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

//    @Before("execution(* org.apache.servicecomb.saga.omega.transaction.SagaStartAspect.advise(..))")
//    public void onBeforeStartingGlobalTx(ProceedingJoinPoint joinPoint) throws Throwable {
//        System.out.println(this.getClass() + " onBeforeStartingGlobalTx(JoinPoint joinPoint).");
////        joinPoint.proceed();
//    }
}
