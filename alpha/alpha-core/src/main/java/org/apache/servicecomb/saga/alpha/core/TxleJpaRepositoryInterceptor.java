package org.apache.servicecomb.saga.alpha.core;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;

public class TxleJpaRepositoryInterceptor {
    @Autowired
    TxleMetrics txleMetrics;

    public Object doFilter(MethodInvocation Invocation) throws Throwable {
        Method method = Invocation.getMethod();
        Query queryAnnotation = method.getAnnotation(Query.class);
        String sql = method.getName();
        if (queryAnnotation != null) {
            sql = queryAnnotation.value();
            // It'll not have a boundary if append arguments to metrics variables, that's not allowed, because it maybe lead to prometheus' death, so have to abandon arguments.
        }

        String globalTxId = txleMetrics.startMarkSQLDurationAndCount(sql, queryAnnotation == null, Invocation.getArguments());

        Object obj = Invocation.proceed();

        txleMetrics.endMarkSQLDuration(globalTxId);

        return obj;
    }
}
