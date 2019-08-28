/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transport.feign;

import feign.RequestInterceptor;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignAutoConfiguration {

    @Bean
    @ConditionalOnClass(RequestInterceptor.class)
    @ConditionalOnBean(OmegaContext.class)
    public RequestInterceptor feignClientRequestInterceptor(OmegaContext omegaContext) {
        return new FeignClientRequestInterceptor(omegaContext);
    }
}
