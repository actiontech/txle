/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
