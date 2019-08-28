/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.connector.grpc;

import static org.apache.servicecomb.saga.common.EventType.SagaStartedEvent;

import java.util.Set;
import java.util.concurrent.BlockingQueue;

import org.apache.servicecomb.saga.omega.transaction.*;
import org.apache.servicecomb.saga.omega.transaction.accidentplatform.AccidentHandling;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcConfigAck;

public class RetryableMessageSender implements MessageSender {
  private final BlockingQueue<MessageSender> availableMessageSenders;

  public RetryableMessageSender(BlockingQueue<MessageSender> availableMessageSenders) {
    this.availableMessageSenders = availableMessageSenders;
  }

  @Override
  public void onConnected() {

  }

  @Override
  public void onDisconnected() {

  }

  @Override
  public void close() {

  }

  @Override
  public String target() {
    return "UNKNOWN";
  }

  @Override
  public AlphaResponse send(TxEvent event) {
    if (event.type() == SagaStartedEvent) {
      throw new OmegaException("Failed to process subsequent requests because no alpha server is available");
    }
    try {
      return availableMessageSenders.take().send(event);
    } catch (InterruptedException e) {
      throw new OmegaException("Failed to send event " + event + " due to interruption", e);
    } catch (Exception e) {
      throw e;
    }
  }

  @Override
  public Set<String> send(Set<String> localTxIdSet) {
    try {
      return availableMessageSenders.take().send(localTxIdSet);
    } catch (InterruptedException e) {
      throw new OmegaException("Failed to send localTxIdSet " + localTxIdSet + " due to interruption", e);
    }
  }

  @Override
  public String reportMessageToServer(KafkaMessage message) {
    try {
      return availableMessageSenders.take().reportMessageToServer(message);
    } catch (InterruptedException e) {
      throw new OmegaException("Failed to report kafka message " + message + " due to interruption", e);
    }
  }

  @Override
  public String reportAccidentToServer(AccidentHandling accidentHandling) {
    try {
      return availableMessageSenders.take().reportAccidentToServer(accidentHandling);
    } catch (InterruptedException e) {
      throw new OmegaException("Failed to report msg to the accident platform " + accidentHandling + " due to interruption", e);
    }
  }

  @Override
  public GrpcConfigAck readConfigFromServer(int type, String category) {
    try {
      return availableMessageSenders.take().readConfigFromServer(type, category);
    } catch (InterruptedException e) {
      throw new OmegaException("Failed to read config (type = " + type + ") from server.", e);
    }
  }
}
