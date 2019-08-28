/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transport.servicecomb;

import static org.apache.servicecomb.saga.omega.context.OmegaContext.GLOBAL_TX_ID_KEY;
import static org.apache.servicecomb.saga.omega.context.OmegaContext.LOCAL_TX_ID_KEY;

import java.lang.invoke.MethodHandles;

import org.apache.servicecomb.core.Handler;
import org.apache.servicecomb.core.Invocation;
import org.apache.servicecomb.foundation.common.utils.BeanUtils;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.swagger.invocation.AsyncResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SagaProviderHandler implements Handler {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final OmegaContext omegaContext;

  public SagaProviderHandler() {
    OmegaContext context = null;
    try {
      context = BeanUtils.getBean("omegaContext");
    } catch (NullPointerException npe) {
      LOG.warn("SagaProviderHandler cannot work rightly, please make sure omegaContext is in the spring application context.\"");
    }
    this.omegaContext = context;
  }

  public SagaProviderHandler(OmegaContext omegaContext) {
    this.omegaContext = omegaContext;
  }

  @Override
  public void handle(Invocation invocation, AsyncResponse asyncResponse) throws Exception {
    if (omegaContext != null) {
      String globalTxId = invocation.getContext().get(GLOBAL_TX_ID_KEY);
      if (globalTxId == null) {
        LOG.debug("no such header: {}", GLOBAL_TX_ID_KEY);
      } else {

        omegaContext.setGlobalTxId(globalTxId);
        omegaContext.setLocalTxId(invocation.getContext().get(LOCAL_TX_ID_KEY));
      }
    } else {
      LOG.info("Cannot inject transaction ID, as the OmegaContext is null or cannot get the globalTxId.");
    }

    invocation.next(asyncResponse);
  }
}
