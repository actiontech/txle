/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.grpc.interfaces;

import com.actionsky.txle.grpc.TxleClientSqlResult;
import com.actionsky.txle.grpc.TxleGrpcClientStream;
import io.grpc.stub.StreamObserver;
import org.apache.servicecomb.saga.alpha.core.TxConsistentService;
import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.apache.servicecomb.saga.alpha.core.TxEventRepository;
import org.apache.servicecomb.saga.alpha.core.accidenthandling.AccidentHandleType;
import org.apache.servicecomb.saga.alpha.core.accidenthandling.AccidentHandling;
import org.apache.servicecomb.saga.alpha.core.accidenthandling.IAccidentHandlingService;
import org.apache.servicecomb.saga.common.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

class GrpcClientStreamObserver implements StreamObserver<TxleGrpcClientStream> {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcClientStreamObserver.class);
    private final IAccidentHandlingService accidentHandlingService;
    private final TxEventRepository eventRepository;
    private TxConsistentService txConsistentService;

    GrpcClientStreamObserver(IAccidentHandlingService accidentHandlingService, TxEventRepository eventRepository, TxConsistentService txConsistentService) {
        this.accidentHandlingService = accidentHandlingService;
        this.eventRepository = eventRepository;
        this.txConsistentService = txConsistentService;
    }

    @Override
    public void onNext(TxleGrpcClientStream clientStream) {
        try {
            LOG.info("Received retry/compensation sql result from client. " + System.nanoTime());

            clientStream.getSqlResultList().forEach(feedback -> {
                /**
                 * 处理补偿/重试反馈结果
                 *
                 * 重试：无论成功失败，均登记重试结果(后续工作在结束事务接口处理)
                 */
                if ("compensate".equals(feedback.getMethod())) {
                    handleCompensateResult(feedback);
                } else if ("retry".equals(feedback.getMethod())) {
                    handleRetryResult(feedback);
                }
            });
        } catch (Throwable e) {
            LOG.error("xxx", e);
        }
    }

    // 补偿：若失败，需上报差错平台；无论成功和失败，均结束当前全局事务和清除当前全局事务的缓存信息(在触发补偿后就已执行)
    private void handleCompensateResult(TxleClientSqlResult feedback) {
        if (!feedback.getIsExecutedOK()) {
            // To report accident to Accident Platform.
            TxEvent event = this.eventRepository.selectEventByGlobalTxIdType(feedback.getGlobalTxId(), EventType.SagaStartedEvent.name());
            String serviceName = "", instanceId = "";
            if (event != null) {
                serviceName = event.serviceName();
                instanceId = event.instanceId();
            }
            AccidentHandling accidentHandling = new AccidentHandling(serviceName, instanceId, feedback.getGlobalTxId(), feedback.getLocalTxId(), AccidentHandleType.ROLLBACK_ERROR, "", "");
            this.accidentHandlingService.reportMsgToAccidentPlatform(accidentHandling.toJsonString());
        }
    }

    private void handleRetryResult(TxleClientSqlResult feedback) {
        // 成功，则记录TxEndedEvent事件，失败记录TxAbortedEvent事件并次数减1
        TxEvent subEvent = eventRepository.selectMinRetriesEventByTxIdType(feedback.getGlobalTxId(), feedback.getLocalTxId(), EventType.TxStartedEvent.name());
        subEvent.setSurrogateId(null);
        subEvent.setRetries(0);
        subEvent.setExpiryTime(new Date(TxEvent.MAX_TIMESTAMP));
        subEvent.setType(feedback.getIsExecutedOK() ? EventType.TxEndedEvent.name() : EventType.TxAbortedEvent.name());
        txConsistentService.registerSubTx(subEvent, null);
    }

    @Override
    public void onError(Throwable t) {
    }

    @Override
    public void onCompleted() {
    }

}
