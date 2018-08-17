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

package org.apache.servicecomb.saga.omega.transaction;

import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.transaction.repository.AutoCompensateDao;
import org.apache.servicecomb.saga.omega.transaction.repository.AutoCompensateEntityRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.annotation.Order;

/**
 * This config is imported to the EnableOmega annotation.
 * 
 * @author Gannalyo
 * @since 2018-07-30
 */
@Configuration
@EnableAspectJAutoProxy
public class AutoCompensableAspectConfig {

	@Bean
	AutoCompensateService autoCompensateService(AutoCompensateEntityRepository autoCompensateRepository) {
		return new AutoCompensateService(autoCompensateRepository);
	}

	@Bean
	AutoCompensateDao autoCompensateDao() {
		return new AutoCompensateDao();
	}

	@Order(1)
	@Bean
	AutoCompensableAspect autoCompensableAspect(MessageSender sender, OmegaContext context,
			AutoCompensateService autoCompensateService) {
		return new AutoCompensableAspect(sender, context, autoCompensateService);
	}
}
