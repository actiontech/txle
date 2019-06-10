/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
