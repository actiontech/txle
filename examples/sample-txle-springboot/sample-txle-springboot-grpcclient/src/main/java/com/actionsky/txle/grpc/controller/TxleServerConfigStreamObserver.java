/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.grpc.controller;

import com.actionsky.txle.grpc.TxleServerConfigStream;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TxleServerConfigStreamObserver implements StreamObserver<TxleServerConfigStream> {
    private static final Logger LOG = LoggerFactory.getLogger(TxleServerConfigStreamObserver.class);
    private final IntegrateTxleController integrateTxleController;

    TxleServerConfigStreamObserver(IntegrateTxleController integrateTxleController) {
        this.integrateTxleController = integrateTxleController;
    }

    @Override
    public void onNext(TxleServerConfigStream serverConfigStream) {
    }

    @Override
    public void onError(Throwable t) {
        try {
            integrateTxleController.onReconnect();
        } catch (Throwable e) {
            LOG.error("Failed to reconnect to txle server - {}.", integrateTxleController.getGrpcServerAddress(), e);
        }
    }

    @Override
    public void onCompleted() {
    }

}
