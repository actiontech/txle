package org.apache.servicecomb.saga.alpha.core.configcenter;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
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
