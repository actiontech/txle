/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.connector.grpc;

import java.lang.invoke.MethodHandles;

import org.apache.servicecomb.saga.omega.transaction.MessageDeserializer;
import org.apache.servicecomb.saga.omega.transaction.MessageHandler;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcCompensateCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.stub.StreamObserver;

class GrpcCompensateStreamObserver implements StreamObserver<GrpcCompensateCommand> {

  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final MessageHandler messageHandler;
  private final Runnable errorHandler;
  private final MessageDeserializer deserializer;

  GrpcCompensateStreamObserver(MessageHandler messageHandler, Runnable errorHandler, MessageDeserializer deserializer) {
    this.messageHandler = messageHandler;
    this.errorHandler = errorHandler;
    this.deserializer = deserializer;
  }

  @Override
  public void onNext(GrpcCompensateCommand command) {
    LOG.error("Received compensate command, global tx id: {}, local tx id: {}, compensation method: {}",
        command.getGlobalTxId(), command.getLocalTxId(), command.getCompensationMethod());

    // receive alpha compensation command. TODO to verify if alpha establish a connection actively.
    messageHandler.onReceive(
        command.getGlobalTxId(),
        command.getLocalTxId(),
        command.getParentTxId().isEmpty() ? null : command.getParentTxId(),
        command.getCompensationMethod(),
        deserializer.deserialize(command.getPayloads().toByteArray()));
  }

  @Override
  public void onError(Throwable t) {
    LOG.error("failed to process grpc compensate command.", t);
    errorHandler.run();
  }

  @Override
  public void onCompleted() {
  }
}
