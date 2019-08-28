/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction;

import javax.transaction.TransactionalException;

import org.apache.servicecomb.saga.omega.context.OmegaContext;

class SagaStartAnnotationProcessor implements EventAwareInterceptor {

  private final OmegaContext omegaContext;
  private final MessageSender sender;

  SagaStartAnnotationProcessor(OmegaContext omegaContext, MessageSender sender) {
    this.omegaContext = omegaContext;
    this.sender = sender;
  }

  @Override
  public AlphaResponse preIntercept(String parentTxId, String compensationMethod, int timeout, String retriesMethod,
      int retries, Object... message) {
    try {
      return sender.send(new SagaStartedEvent(omegaContext.globalTxId(), omegaContext.localTxId(), timeout, omegaContext.category()));
    } catch (OmegaException e) {
      throw new TransactionalException(e.getMessage(), e.getCause());
    }
  }

  @Override
  public void postIntercept(String parentTxId, String compensationMethod) {
    AlphaResponse response = sender.send(new SagaEndedEvent(omegaContext.globalTxId(), omegaContext.localTxId(), omegaContext.category()));
    if (response.aborted()) {
      throw new OmegaException("transaction " + parentTxId + " is aborted");
    }
  }

  @Override
  public void onError(String parentTxId, String compensationMethod, Throwable throwable) {
    String globalTxId = omegaContext.globalTxId();
    sender.send(new TxAbortedEvent(globalTxId, omegaContext.localTxId(), null, compensationMethod, omegaContext.category(), throwable));
  }
}
