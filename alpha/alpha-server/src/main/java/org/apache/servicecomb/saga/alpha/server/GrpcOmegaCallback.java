/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.server;

import org.apache.servicecomb.saga.alpha.core.OmegaCallback;
import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.apache.servicecomb.saga.pack.contract.grpc.GrpcCompensateCommand;

import com.google.protobuf.ByteString;

import io.grpc.stub.StreamObserver;

class GrpcOmegaCallback implements OmegaCallback {

  private final StreamObserver<GrpcCompensateCommand> observer;

  GrpcOmegaCallback(StreamObserver<GrpcCompensateCommand> observer) {
    this.observer = observer;
  }

  @Override
  public void compensate(TxEvent event) {
    GrpcCompensateCommand command = GrpcCompensateCommand.newBuilder()
        .setGlobalTxId(event.globalTxId())
        .setLocalTxId(event.localTxId())
        .setParentTxId(event.parentTxId() == null ? "" : event.parentTxId())
        .setCompensationMethod(event.compensationMethod())
        .setPayloads(ByteString.copyFrom(event.payloads()))
        .build();
    observer.onNext(command);
  }

  @Override
  public void disconnect() {
    observer.onCompleted();
  }
}
