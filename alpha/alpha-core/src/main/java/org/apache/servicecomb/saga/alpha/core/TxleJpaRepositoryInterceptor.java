/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.core;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;

public class TxleJpaRepositoryInterceptor {
    @Autowired
    private TxleMetrics txleMetrics;

    public Object doFilter(MethodInvocation invocation) throws Throwable {
        Method method = invocation.getMethod();
        Query queryAnnotation = method.getAnnotation(Query.class);
        String sql = method.getName();
        if (queryAnnotation != null) {
            sql = queryAnnotation.value();
            // It'll not have a boundary if append arguments to metrics variables, that's not allowed, because it maybe lead to prometheus' death, so have to abandon arguments.
        }

        String globalTxId = txleMetrics.startMarkSQLDurationAndCount(sql, queryAnnotation == null, invocation.getArguments());

        Object obj = invocation.proceed();

        txleMetrics.endMarkSQLDuration(globalTxId);

        return obj;
    }
}
