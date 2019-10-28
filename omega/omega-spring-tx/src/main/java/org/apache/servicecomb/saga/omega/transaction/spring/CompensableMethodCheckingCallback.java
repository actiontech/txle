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

package org.apache.servicecomb.saga.omega.transaction.spring;

import org.apache.servicecomb.saga.common.TxleConstants;
import org.apache.servicecomb.saga.omega.context.CompensationContext;
import org.apache.servicecomb.saga.omega.transaction.OmegaException;
import org.apache.servicecomb.saga.omega.transaction.annotations.Compensable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils.MethodCallback;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

class CompensableMethodCheckingCallback implements MethodCallback {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final Object bean;

  private final CompensationContext compensationContext;

  CompensableMethodCheckingCallback(Object bean, CompensationContext compensationContext) {
    this.bean = bean;
    this.compensationContext = compensationContext;
  }

  @Override
  public void doWith(Method method) throws IllegalArgumentException {
    if (!method.isAnnotationPresent(Compensable.class)) {
    	// for auto-compensation By Gannalyo
    	if (TxleConstants.AUTO_COMPENSABLE_METHOD.equals(method.toString())) {
    		compensationContext.addCompensationContext(method, bean);
    	}
    	return;
    }

    String compensationMethod = method.getAnnotation(Compensable.class).compensationMethod();

    try {
      compensationContext.addCompensationContext(method, bean);

      if (compensationMethod.isEmpty()) {
        throw new OmegaException("Compensation method is empty in " + bean.getClass().getCanonicalName());
      }

      Method signature = bean.getClass().getDeclaredMethod(compensationMethod, method.getParameterTypes());
      compensationContext.addCompensationContext(signature, bean);
      LOG.debug("Found compensation method [{}] in {}", compensationMethod, bean.getClass().getCanonicalName());
    } catch (NoSuchMethodException e) {
      throw new OmegaException(
          "No such compensation method [" + compensationMethod + "] found in " + bean.getClass().getCanonicalName(),
          e);
    }
  }
}
