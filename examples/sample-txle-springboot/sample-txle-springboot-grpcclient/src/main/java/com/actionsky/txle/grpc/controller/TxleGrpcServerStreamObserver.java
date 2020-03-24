/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.grpc.controller;

import com.actionsky.txle.grpc.TxleClientSqlResult;
import com.actionsky.txle.grpc.TxleGrpcClientStream;
import com.actionsky.txle.grpc.TxleGrpcServerStream;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TxleGrpcServerStreamObserver implements StreamObserver<TxleGrpcServerStream> {
    private static final Logger LOG = LoggerFactory.getLogger(TxleGrpcServerStreamObserver.class);
    private StreamObserver<TxleGrpcClientStream> clientStreamObserver;
    private final IntegrateTxleController integrateTxleController;

    TxleGrpcServerStreamObserver(IntegrateTxleController integrateTxleController) {
        this.integrateTxleController = integrateTxleController;
    }

    public void setClientStreamObserver(StreamObserver<TxleGrpcClientStream> clientStreamObserver) {
        this.clientStreamObserver = clientStreamObserver;
    }

    @Override
    public void onNext(TxleGrpcServerStream serverStream) {
        TxleGrpcClientStream.Builder clientStream = TxleGrpcClientStream.newBuilder();
        try {
            // getExecuteSqlList - 代表多个子事务执行的SQL相关信息
            serverStream.getExecuteSqlList().forEach(executeSql -> {
                // 一个子事务可能有多条执行语句，比如重试场景可能会有多条备份SQL
                executeSql.getSubTxSqlList().forEach(sql -> System.err.println(sql));
                boolean isExecutedOK = false;
                try {
                    if ("db2".equals(executeSql.getDbSchema())) {
//                        isExecutedOK = integrateTxleController.getPrimaryCustomService().executeSubTxSqls(executeSql.getSubTxSqlList()) == executeSql.getSubTxSqlCount();
                        integrateTxleController.getPrimaryCustomService().executeSubTxSqls(executeSql.getSubTxSqlList());
                    } else if ("db3".equals(executeSql.getDbSchema())) {
//                        isExecutedOK = integrateTxleController.getSecondaryCustomService().executeSubTxSqls(executeSql.getSubTxSqlList()) == executeSql.getSubTxSqlCount();
                        integrateTxleController.getSecondaryCustomService().executeSubTxSqls(executeSql.getSubTxSqlList());
                    }
                    isExecutedOK = true;
                } catch (Exception e) {
                    LOG.error("Failed to execute " + executeSql.getMethod() + " method.", e);
                }

                // 此处需要注意，无论重试或补偿方法执行成功与否，都需要返回结果，txle端会将结果写入数据库，供重试自旋程序检索，若不返回，那面检索不到会导致一直检索直到整个全局事务超时
                TxleClientSqlResult result = TxleClientSqlResult.newBuilder()
                        .setDbNodeId(executeSql.getDbNodeId())
                        .setGlobalTxId(executeSql.getGlobalTxId())
                        .setLocalTxId(executeSql.getLocalTxId())
                        .setMethod(executeSql.getMethod())
                        .setIsExecutedOK(isExecutedOK)
                        .build();

                clientStream.addSqlResult(result);
            });
        } catch (Throwable e) {
            LOG.error("xxx", e);
        } finally {
            this.clientStreamObserver.onNext(clientStream.build());
        }
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
