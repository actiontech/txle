/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transport.resttemplate;

import brave.Tracing;
import brave.spring.web.TracingClientHttpRequestInterceptor;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Configuration
public class RestTemplateConfig {

  @Bean
  public RestTemplate restTemplate(@Autowired(required = false) OmegaContext context, @Autowired Tracing tracing) {
    RestTemplate template = new RestTemplate();
    List<ClientHttpRequestInterceptor> interceptors = template.getInterceptors();
    interceptors.add(new TransactionClientHttpRequestInterceptor(context));
    // add interceptor for rest's request server By Gannalyo
    interceptors.add(TracingClientHttpRequestInterceptor.create(tracing));
    template.setInterceptors(interceptors);
    return template;
  }
}
