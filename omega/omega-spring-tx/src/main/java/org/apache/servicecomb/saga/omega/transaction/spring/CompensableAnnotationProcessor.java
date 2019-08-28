/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction.spring;

import org.apache.servicecomb.saga.omega.context.CompensationContext;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.util.ReflectionUtils;

class CompensableAnnotationProcessor implements BeanPostProcessor {

  private final OmegaContext omegaContext;

  private final CompensationContext compensationContext;

  CompensableAnnotationProcessor(OmegaContext omegaContext, CompensationContext compensationContext) {
    this.omegaContext = omegaContext;
    this.compensationContext = compensationContext;
  }

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    checkMethod(bean);
    checkFields(bean);
    return bean;
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    return bean;
  }

  private void checkMethod(Object bean) {
    ReflectionUtils.doWithMethods(
        bean.getClass(),
        new CompensableMethodCheckingCallback(bean, compensationContext));
  }

  private void checkFields(Object bean) {
    ReflectionUtils.doWithFields(bean.getClass(), new ExecutorFieldCallback(bean, omegaContext));
  }
}
