/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction.spring;

import org.apache.servicecomb.saga.omega.context.CompensationContext;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.transaction.CompensationMessageHandler;
import org.apache.servicecomb.saga.omega.transaction.MessageHandler;
import org.apache.servicecomb.saga.omega.transaction.MessageSender;
import org.apache.servicecomb.saga.omega.transaction.SagaStartAspect;
import org.apache.servicecomb.saga.omega.transaction.TransactionAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.annotation.Order;

@Configuration
@EnableAspectJAutoProxy
public class TransactionAspectConfig {

  @Bean
  MessageHandler messageHandler(MessageSender sender, CompensationContext context, OmegaContext omegaContext) {
    return new CompensationMessageHandler(sender, context);
  }

  @Order(0)
  @Bean
  SagaStartAspect sagaStartAspect(MessageSender sender, OmegaContext context) {
    return new SagaStartAspect(sender, context);
  }

  @Order(1)
  @Bean
  TransactionAspect transactionAspect(MessageSender sender, OmegaContext context) {
    return new TransactionAspect(sender, context);
  }

  @Bean
  CompensableAnnotationProcessor compensableAnnotationProcessor(OmegaContext omegaContext,
      CompensationContext compensationContext) {
    return new CompensableAnnotationProcessor(omegaContext, compensationContext);
  }
}
