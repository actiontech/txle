/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.spring;

import org.apache.servicecomb.saga.common.CommonConfig;
import org.apache.servicecomb.saga.omega.context.TracingConfiguration;
import org.apache.servicecomb.saga.omega.transaction.AutoCompensableAspectConfig;
import org.apache.servicecomb.saga.omega.transaction.spring.TransactionAspectConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({OmegaSpringConfig.class, TransactionAspectConfig.class, AutoCompensableAspectConfig.class, CommonConfig.class, TracingConfiguration.class})

//@EntityScan(basePackages = {"com.gannalyo.saga.user.entity"})
//@EnableJpaRepositories(basePackages = {"com.gannalyo.saga.user.repository"})
//@EntityScan(basePackages = {"org.apache.servicecomb.saga.omega.transaction.repository.entity"})
//@EnableJpaRepositories(basePackages = {"org.apache.servicecomb.saga.omega.transaction.repository"})
//@EntityScan(basePackages = {"org.apache.servicecomb.saga.omega.transaction.repository.entity", "com.gannalyo.saga.user.entity"})
//@EnableJpaRepositories(basePackages = {"org.apache.servicecomb.saga.omega.transaction.repository", "com.gannalyo.saga.user.repository"})
/**
 * Indicates create the OmegaContext and inject it into the interceptors
 * to pass the transactions id across the application.
 * @see org.apache.servicecomb.saga.omega.context.OmegaContext
 */
public @interface EnableOmega {
}
