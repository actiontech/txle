/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transport.resttemplate;

import static org.apache.servicecomb.saga.omega.context.OmegaContext.GLOBAL_TX_ID_KEY;
import static org.apache.servicecomb.saga.omega.context.OmegaContext.LOCAL_TX_ID_KEY;
import static org.apache.servicecomb.saga.omega.context.OmegaContext.GLOBAL_TX_CATEGORY_KEY;

import java.lang.invoke.MethodHandles;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

class TransactionHandlerInterceptor implements HandlerInterceptor {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final OmegaContext omegaContext;

  TransactionHandlerInterceptor(OmegaContext omegaContext) {
    this.omegaContext = omegaContext;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (omegaContext != null) {
      String globalTxId = request.getHeader(GLOBAL_TX_ID_KEY);
      if (globalTxId == null) {
        LOG.debug("no such header: {}", GLOBAL_TX_ID_KEY);
      } else {
        omegaContext.setGlobalTxId(globalTxId);
        omegaContext.setLocalTxId(request.getHeader(LOCAL_TX_ID_KEY));
        omegaContext.setCategory(request.getHeader(GLOBAL_TX_CATEGORY_KEY));
      }
    }
    return true;
  }

  @Override
  public void postHandle(HttpServletRequest request, HttpServletResponse response, Object o, ModelAndView mv) {
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object o, Exception e) {
  }
}
