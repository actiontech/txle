/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction;

import org.apache.servicecomb.saga.omega.context.ApplicationContextUtil;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.transaction.repository.AutoCompensateDao;
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
	AutoCompensateService autoCompensateService() {
		return new AutoCompensateService();
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

    @Bean
    ApplicationContextUtil applicationContextUtil() {
        return new ApplicationContextUtil();
    }
}
