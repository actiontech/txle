/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.core;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PushBackOmegaCallback implements OmegaCallback {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final BlockingQueue<Runnable> pendingCompensations;
  private final OmegaCallback underlying;

  public PushBackOmegaCallback(BlockingQueue<Runnable> pendingCompensations, OmegaCallback underlying) {
    this.pendingCompensations = pendingCompensations;
    this.underlying = underlying;
  }

  @Override
  public void compensate(TxEvent event) {
    try {
      underlying.compensate(event);
    } catch (Exception e) {
      logError(event, e);
      pendingCompensations.offer(() -> compensate(event));
    }
  }

  private void logError(TxEvent event, Exception e) {
    LOG.error(
        "Failed to {} service [{}] instance [{}] with method [{}], global tx id [{}] and local tx id [{}]",
        event.retries() == 0 ? "compensate" : "retry",
        event.serviceName(),
        event.instanceId(),
        event.retries() == 0 ? event.compensationMethod() : event.retryMethod(),
        event.globalTxId(),
        event.localTxId(),
        e);
  }
}
