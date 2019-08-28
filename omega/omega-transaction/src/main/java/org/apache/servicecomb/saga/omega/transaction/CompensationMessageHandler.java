/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction;

import org.apache.servicecomb.saga.omega.context.CompensationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class CompensationMessageHandler implements MessageHandler {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final MessageSender sender;

  private final CompensationContext context;

  public CompensationMessageHandler(MessageSender sender, CompensationContext context) {
    this.sender = sender;
    this.context = context;
  }

  @Override
  public void onReceive(String globalTxId, String localTxId, String parentTxId, String compensationMethod,
      Object... payloads) {
    try {
      context.apply(globalTxId, localTxId, compensationMethod, payloads);
    } catch (Exception e) {
      LOG.error("Failed to execute 'onReceive.context.apply' localTxId {}", localTxId, e);
    }
    sender.send(new TxCompensatedEvent(globalTxId, localTxId, parentTxId, compensationMethod));
  }
}
