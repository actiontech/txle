/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.common;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * This config is imported to the EnableOmega annotation.
 *
 * @author Gannalyo
 * @since 2018-07-30
 */
@Configuration
@EnableAspectJAutoProxy
public class CommonConfig {

    @Bean
    public RestTemplate restTemplate(@Qualifier("simpleClientHttpRequestFactory") ClientHttpRequestFactory clientHttpRequestFactory) {
        return new RestTemplate(clientHttpRequestFactory);
    }

    @Bean
    public ClientHttpRequestFactory simpleClientHttpRequestFactory() {
        // setReadTimeout(5000); setConnectTimeout(15000);
        return new SimpleClientHttpRequestFactory();
    }

}
