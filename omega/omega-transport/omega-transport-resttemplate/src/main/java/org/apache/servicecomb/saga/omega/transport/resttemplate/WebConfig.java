/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transport.resttemplate;

import java.lang.invoke.MethodHandles;

import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
@EnableWebMvc
public class WebConfig extends WebMvcConfigurerAdapter {

  private final OmegaContext omegaContext;

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


  @Autowired
  public WebConfig(@Autowired(required = false) OmegaContext omegaContext) {
    this.omegaContext = omegaContext;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    if (omegaContext == null) {
      LOG.info("The OmegaContext is not injected, The transaction handler is disabled");
    }
    registry.addInterceptor(new TransactionHandlerInterceptor(omegaContext));
  }
}
