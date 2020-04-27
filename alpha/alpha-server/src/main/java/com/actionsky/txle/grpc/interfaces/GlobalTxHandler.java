/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */
package com.actionsky.txle.grpc.interfaces;

import com.actionsky.txle.cache.ClientGlobalTxCache;
import com.actionsky.txle.cache.ITxleConsistencyCache;
import com.actionsky.txle.cache.ITxleEhCache;
import com.actionsky.txle.cache.TxleCacheType;
import com.actionsky.txle.enums.GlobalTxStatus;
import com.actionsky.txle.exception.ExceptionFaultTolerance;
import com.actionsky.txle.grpc.*;
import com.actionsky.txle.grpc.interfaces.eventaddition.ITxEventAdditionService;
import com.actionsky.txle.grpc.interfaces.eventaddition.TxEventAddition;
import io.grpc.stub.StreamObserver;
import org.apache.servicecomb.saga.alpha.core.TxConsistentService;
import org.apache.servicecomb.saga.alpha.core.TxEvent;
import org.apache.servicecomb.saga.alpha.core.TxEventRepository;
import org.apache.servicecomb.saga.alpha.core.accidenthandling.AccidentHandleType;
import org.apache.servicecomb.saga.alpha.core.accidenthandling.AccidentHandling;
import org.apache.servicecomb.saga.alpha.core.accidenthandling.IAccidentHandlingService;
import org.apache.servicecomb.saga.common.ConfigCenterType;
import org.apache.servicecomb.saga.common.EventType;
import org.apache.servicecomb.saga.common.TxleConstants;
import org.apache.servicecomb.saga.common.TxleDefaultTheadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.actionsky.txle.enums.GlobalTxStatus.*;
import static org.apache.servicecomb.saga.common.EventType.*;

/**
 * @author Gannalyo
 * @since 2020/2/25
 */
public class GlobalTxHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalTxHandler.class);

    @Autowired
    private TxConsistentService txConsistentService;

    @Autowired
    private TxEventRepository eventRepository;

    @Autowired
    private ITxEventAdditionService eventAdditionService;

    @Autowired
    private IAccidentHandlingService accidentHandlingService;

    @Autowired
    private ITxleEhCache txleEhCache;

    @Resource(name = "txleMysqlCache")
    @Autowired
    private ITxleConsistencyCache consistencyCache;

    private final ExecutorService executorService = Executors.newFixedThreadPool(3, new TxleDefaultTheadFactory("txle-rpc-txhandler-"));

    public boolean checkIsExistsGlobalTx(String globalTxId) {
        return this.getGlobalTxCache(globalTxId) != null;
    }

    public void checkTimeout(StreamObserver<TxleGrpcServerStream> serverStreamObserver) {
//        try {
//            List<String> unendedGlobalTxIds = txleEhCache.getKeys(TxleCacheType.GLOBALTX);
//            if (unendedGlobalTxIds != null && !unendedGlobalTxIds.isEmpty()) {
//                // 检测超时，及时通知第三方客户端，通知后如何处理交给第三方自行决定，但无论如何处理都会保证数据的最终一致性
//                List<TxEvent> timeoutEvents = eventRepository.findTimeoutEvents(unendedGlobalTxIds);
//                if (timeoutEvents != null) {
//                    TxleGrpcServerStream.Builder returnClientStream = TxleGrpcServerStream.newBuilder();
//
//                    timeoutEvents.forEach(event -> {
//                        TxleServerExecuteSql.Builder executeSqlInfo = TxleServerExecuteSql.newBuilder();
//                        executeSqlInfo.setGlobalTxId(event.globalTxId()).setMethod("timeout");
//                        returnClientStream.addExecuteSql(executeSqlInfo);
//                    });
//
//                    serverStreamObserver.onNext(returnClientStream.build());
//                }
//            }
//        } catch (Exception e) {
//            LOG.error("Failed to check timeout.", e);
//        }
    }

    public boolean registerStartTx(TxleTransactionStart tx, TxleTxStartAck.Builder txStartAck, boolean isExistsGlobalTx, Map<String, String> localBackupSqls, Map<String, String> localTxCompensateSql) {
        final String globalTxId = tx.getGlobalTxId();
        final String instanceId = TxleConstants.getServiceInstanceId(tx.getServiceName(), tx.getServiceIP());
        if (!isExistsGlobalTx) {
            TxEvent startTxEvent = new TxEvent(tx.getServiceName(), instanceId, globalTxId, globalTxId, "", SagaStartedEvent.name(), "", tx.getTimeout(), null, 0, tx.getServiceCategory(), null);
            boolean registerResult = txConsistentService.registerGlobalTx(startTxEvent);
            if (!registerResult) {
                String cause = "Failed to end global transaction, [" + startTxEvent.toString() + "].";
                ExceptionFaultTolerance.handleErrorWithFaultTolerantCheck(consistencyCache, tx.getGlobalTxId(), cause, instanceId, tx.getServiceCategory(), txStartAck, null);
            }
        }

        tx.getSubTxInfoList().forEach(subTx -> {
            String localTxId = subTx.getLocalTxId();
            String compensateSql = localTxCompensateSql.get(localTxId);
            String backupSqls = localBackupSqls.get(localTxId);
            TxEvent subTxEvent = new TxEvent(tx.getServiceName(), instanceId, globalTxId, localTxId, "", TxStartedEvent.name(), null, subTx.getTimeout(), null, subTx.getRetries(), tx.getServiceCategory(), null);
            TxEventAddition subTxEventAddition = new TxEventAddition(tx.getServiceName(), instanceId, globalTxId, localTxId, subTx.getDbNodeId(), subTx.getDbSchema(), subTx.getSql(), backupSqls, compensateSql, subTx.getOrder());
            if (!txConsistentService.registerSubTx(subTxEvent, subTxEventAddition)) {
                // verify the fault-tolerance for global transaction
                String cause = "Failed to register sub-transaction, [" + subTxEventAddition.toString() + "].";
                ExceptionFaultTolerance.handleErrorWithFaultTolerantCheck(consistencyCache, tx.getGlobalTxId(), cause, instanceId, tx.getServiceCategory(), txStartAck, null);
            }
        });
        return true;
    }

    public boolean registerEndTx(TxleTransactionEnd tx, TxleTxEndAck.Builder txEndAck) {
        Map<String, TxEvent> subTxEventMap = new HashMap<>();
        List<TxEvent> eventList = eventRepository.selectTxEventByGlobalTxIds(Arrays.asList(tx.getGlobalTxId()));
        if (eventList == null || eventList.isEmpty()) {
            txEndAck.setStatus(TxleTxEndAck.TransactionStatus.ABORTED).setMessage("Empty sub-transaction for global transaction [" + tx.getGlobalTxId() + "].");
            return false;
        } else {
            eventList.forEach(subEvent -> subTxEventMap.put(subEvent.localTxId(), subEvent));
        }

        tx.getSubTxInfoList().forEach(subTx -> {
            String localTxId = subTx.getLocalTxId();
            TxEvent subStartedEvent = subTxEventMap.get(localTxId);
            if (subStartedEvent != null) {
                String type = subTx.getIsSuccessful() ? TxEndedEvent.name() : TxAbortedEvent.name();
                TxEvent subTxEvent = new TxEvent(subStartedEvent.serviceName(), subStartedEvent.instanceId(), tx.getGlobalTxId(), localTxId, "", type, null, 0, null, 0, subStartedEvent.category(), null);
                if (!txConsistentService.registerSubTx(subTxEvent, null)) {
                    // verify the fault-tolerance for global transaction
                    String cause = "Failed to register sub-transaction, [" + subTxEvent.toString() + "].";
                    ExceptionFaultTolerance.handleErrorWithFaultTolerantCheck(consistencyCache, tx.getGlobalTxId(), cause, subStartedEvent.instanceId(), subTxEvent.category(), null, txEndAck);
                }
            }
        });
        return true;
    }

    // timeout, global tx aborted, or sub-tx aborted finally
    public boolean checkIsNeedCompensate(TxleTransactionEnd tx, TxleTxEndAck.Builder endAck, List<TxleSubTransactionEnd> abnormalSubTxList) {
        boolean isNeedCompensate = false;
        try {
            if (GlobalTxStatus.Aborted.toString().equals(consistencyCache.getValueByCacheKey(TxleConstants.constructTxStatusCacheKey(tx.getGlobalTxId()))) || endAck.getStatus().ordinal() == TxleTxEndAck.TransactionStatus.ABORTED.ordinal()) {
                return true;
            }
            List<TxEvent> eventList = eventRepository.selectTxEventByGlobalTxIds(Arrays.asList(tx.getGlobalTxId()));
            if (eventList == null || eventList.isEmpty()) {
                String cause = "Empty transaction for global transaction [" + tx.getGlobalTxId() + "].";
                ExceptionFaultTolerance.handleErrorWithFaultTolerantCheck(consistencyCache, tx.getGlobalTxId(), cause, null, null, null, endAck);
            } else {
                for (TxEvent subEvent : eventList) {
                    if (TxStartedEvent.name().equals(subEvent.type())) {
                        // check timeout
                        timeoutChecking(tx.getIsCanOver(), subEvent);
                    }

                    for (TxleSubTransactionEnd subTx : abnormalSubTxList) {
                        // sub-tx aborted finally
                        if (subEvent.retries() == 0 && TxStartedEvent.name().equals(subEvent.type()) && subTx.getLocalTxId().equals(subEvent.localTxId())) {
                            isNeedCompensate = true;
                            break;
                        }
                    }
                }

                // global tx aborted
                String value = consistencyCache.getValueByCacheKey(TxleConstants.constructTxStatusCacheKey(tx.getGlobalTxId()));
                if (value != null && TxleTxStartAck.TransactionStatus.valueOf(value.toUpperCase()).ordinal() == TxleTxStartAck.TransactionStatus.ABORTED.ordinal()) {
                    isNeedCompensate = true;
                }
            }
        } catch (Exception e) {
            isNeedCompensate = true;
            LOG.error("Failed to check whether compensation is required. globalTxId = {}", tx.getGlobalTxId(), e);
        }
        return isNeedCompensate;
    }

    public void compensateInStartingTx(TxleTransactionStart tx, TxleTxStartAck.Builder startAck, StreamObserver<TxleGrpcServerStream> serverStreamObserver) {
        try {
            startAck.setStatus(TxleTxStartAck.TransactionStatus.ABORTED);
            // search ended sub-txs in reverse order
            List<TxEventAddition> eventAdditions = eventAdditionService.selectDescEventByGlobalTxId(tx.getGlobalTxId());
            if (eventAdditions != null && !eventAdditions.isEmpty()) {
                TxleGrpcServerStream.Builder serverStream = TxleGrpcServerStream.newBuilder();
                // send compensation sqls to client and will be executed in order by client
                eventAdditions.forEach(subTx -> compensate(tx.getGlobalTxId(), subTx, serverStream));
                serverStreamObserver.onNext(serverStream.build());
            }
        } catch (Exception e) {
            LOG.error("Failed to compensate for global transaction. id = {}", tx.getGlobalTxId(), e);
            this.reportMsgToAccidentPlatform(tx.getGlobalTxId());
        } finally {
            this.endGlobalTx(tx.getGlobalTxId(), true, startAck, null);
        }
    }

    // compensation conditions: timeout, or abnormal sub-txs which have no more retry times.
    public void compensateInEndingTx(TxleTransactionEnd tx, StreamObserver<TxleGrpcServerStream> serverStreamObserver, List<TxleSubTransactionEnd> abnormalSubTxList) {
        Set<String> abnormalLocalTxIdSet = new HashSet<>();
        try {
            abnormalSubTxList.forEach(subTx -> abnormalLocalTxIdSet.add(subTx.getLocalTxId()));

            // search ended sub-txs in reverse order
            List<TxEventAddition> eventAdditions = eventAdditionService.selectDescEventByGlobalTxId(tx.getGlobalTxId());
            if (eventAdditions != null && !eventAdditions.isEmpty()) {
                TxleGrpcServerStream.Builder serverStream = TxleGrpcServerStream.newBuilder();
                // send compensation sqls to client and will be executed in order by client
                eventAdditions.forEach(subTx -> {
                    if (!abnormalLocalTxIdSet.contains(subTx.getLocalTxId())) {
                        compensate(tx.getGlobalTxId(), subTx, serverStream);
                    }
                });
                serverStreamObserver.onNext(serverStream.build());
            }
        } catch (Exception e) {
            LOG.error("Failed to compensate for global transaction. id = {}", tx.getGlobalTxId(), e);
            this.reportMsgToAccidentPlatform(tx.getGlobalTxId());
        }
    }

    private void compensate(String globalTxId, TxEventAddition subTx, TxleGrpcServerStream.Builder serverStream) {
        TxleServerExecuteSql.Builder executeSqlInfo = TxleServerExecuteSql.newBuilder();
        executeSqlInfo.setDbNodeId(subTx.getDbNodeId())
                .setDbSchema(subTx.getDbSchema())
                .setGlobalTxId(subTx.getGlobalTxId())
                .setLocalTxId(subTx.getLocalTxId())
                .addSubTxSql(subTx.getCompensateSql())
                .setMethod("compensate");
        serverStream.addExecuteSql(executeSqlInfo.build()).build();

        // register for compensation
        executorService.execute(() -> {
            try {
                TxEvent subEvent = eventRepository.selectMinRetriesEventByTxIdType(globalTxId, subTx.getLocalTxId(), EventType.TxStartedEvent.name());
                subEvent.setSurrogateId(-1L);
                subEvent.setType(EventType.TxCompensatedEvent.name());
                subEvent.setRetries(0);
                subEvent.setRetryMethod(null);
                subEvent.setExpiryTime(new Date(TxEvent.MAX_TIMESTAMP));
                subEvent.setCreationTime(new Date());
                txConsistentService.registerSubTx(subEvent, null);
            } catch (Exception e) {
                LOG.error("Failed to register 'TxCompensatedEvent'. globalTxId = {}, localTxId = {}", globalTxId, subTx.getLocalTxId(), e);
            }
        });
    }

    private void reportMsgToAccidentPlatform(String globalTxId) {
        // To report accident to Accident Platform.
        try {
            TxEvent event = this.eventRepository.selectEventByGlobalTxIdType(globalTxId, EventType.SagaStartedEvent.name());
            String serviceName = "", instanceId = "";
            if (event != null) {
                serviceName = event.serviceName();
                instanceId = event.instanceId();
            }
            AccidentHandling accidentHandling = new AccidentHandling(serviceName, instanceId, globalTxId, globalTxId, AccidentHandleType.ROLLBACK_ERROR, "", "");
            this.accidentHandlingService.reportMsgToAccidentPlatform(accidentHandling.toJsonString());
        } catch (Exception e1) {
            LOG.error("Failed to report msg to Accident Platform. globalTxId = {}", globalTxId, e1);
        }
    }

    public boolean retry(TxleTransactionEnd tx, TxleTxEndAck.Builder endAck, StreamObserver<TxleGrpcServerStream> serverStreamObserver) {
        try {
            consistencyCache.setKeyValueCache(TxleConstants.constructTxStatusCacheKey(tx.getGlobalTxId()), Paused.toString());
            endAck.setStatus(TxleTxEndAck.TransactionStatus.PAUSED);
            ClientGlobalTxCache txCache = this.getGlobalTxCache(tx.getGlobalTxId());

            List<TxEvent> eventList = eventRepository.selectTxEventByGlobalTxIds(Arrays.asList(tx.getGlobalTxId()));
            if (eventList == null || eventList.isEmpty()) {
                String cause = "Empty transaction for global transaction [" + tx.getGlobalTxId() + "].";
                ExceptionFaultTolerance.handleErrorWithFaultTolerantCheck(consistencyCache, tx.getGlobalTxId(), cause, txCache.getInstanceId(), txCache.getServiceCategory(), null, endAck);
            } else {
                List<TxEventAddition> eventAdditions = eventAdditionService.selectDescEventByGlobalTxId(tx.getGlobalTxId());
                if (eventAdditions != null && !eventAdditions.isEmpty()) {
                    LinkedHashMap<String, TxEvent> subStartedEventList = new LinkedHashMap<>();
                    eventList.forEach(subTx -> {
                        if (TxStartedEvent.name().equals(subTx.type())) {
                            subStartedEventList.put(subTx.localTxId(), subTx);
                        }
                    });

                    LinkedHashMap<String, TxEventAddition> subTxEventAddition = new LinkedHashMap<>();
                    eventAdditions.forEach(subTx -> subTxEventAddition.put(subTx.getLocalTxId(), subTx));

                    for (TxleSubTransactionEnd subTx : tx.getSubTxInfoList()) {
                        if (!subTx.getIsSuccessful()) {
                            TxEvent subStartedEvent = subStartedEventList.get(subTx.getLocalTxId());
                            // retries==-1, retry forever
                            if (subStartedEvent.retries() == -1 || subStartedEvent.retries() > 0) {
                                TxEventAddition eventAddition = subTxEventAddition.get(subTx.getLocalTxId());
                                LinkedHashSet<String> backupSqls = new LinkedHashSet<>();
                                for (String sql : eventAddition.getBackupSql().split(TxleConstants.STRING_SEPARATOR)) {
                                    backupSqls.add(sql);
                                }

                                TxleGrpcServerStream.Builder serverStream = TxleGrpcServerStream.newBuilder();
                                TxleServerExecuteSql.Builder executeSqlInfo = TxleServerExecuteSql.newBuilder();
                                executeSqlInfo.setDbNodeId(eventAddition.getDbNodeId())
                                        .setDbSchema(eventAddition.getDbSchema())
                                        .setGlobalTxId(eventAddition.getGlobalTxId())
                                        .setLocalTxId(eventAddition.getLocalTxId())
                                        .addAllSubTxSql(backupSqls)
                                        .setMethod("retry");
                                serverStream.addExecuteSql(executeSqlInfo.build()).build();

                                if (!tryAgainUntilSuccessOrOvertime(eventAddition.getLocalTxId(), subStartedEvent, serverStreamObserver, serverStream)) {
                                    endAck.setStatus(TxleTxEndAck.TransactionStatus.ABORTED);
                                    return false;
                                }
                            }
                        }
                    }
                    // reset to RUNNING status after retrying successfully
                    endAck.setStatus(TxleTxEndAck.TransactionStatus.RUNNING);
                    consistencyCache.delete(TxleConstants.constructTxStatusCacheKey(tx.getGlobalTxId()));
                    return true;
                }
            }
        } catch (Exception e) {
            endAck.setStatus(TxleTxEndAck.TransactionStatus.ABORTED);
            LOG.error("Failed to retry for global transaction. id = {}", tx.getGlobalTxId(), e);
        }
        return false;
    }

    // retries == -1, retry forever until success or timeout
    // retries > 0, retry for retries times until success or timeout
    // retry logic: listen retry result continually, once failure and no more retries, or timeout, then terminate retry immediately for all sub-txs, and compensate
    // if the last retry result exists and it's failed, then execute the next retry, so that guarantee idempotence
    private boolean tryAgainUntilSuccessOrOvertime(String localTxId, TxEvent subStartedEvent, StreamObserver<TxleGrpcServerStream> serverStreamObserver, TxleGrpcServerStream.Builder serverStream) {
        if (subStartedEvent.retries() == -1) {
            while (true) {
                if (doRetry(localTxId, subStartedEvent, serverStreamObserver, serverStream)) {
                    return true;
                }
            }
        } else {
            for (int i = 0; i < subStartedEvent.retries(); i++) {
                if (doRetry(localTxId, subStartedEvent, serverStreamObserver, serverStream)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean doRetry(String localTxId, TxEvent subStartedEvent, StreamObserver<TxleGrpcServerStream> serverStreamObserver, TxleGrpcServerStream.Builder serverStream) {
        // timeout will throw an exception under retry circumstance
        if (new Date().compareTo(subStartedEvent.expiryTime()) > 0) {
            throw new RuntimeException("Current global transaction was overtime. globalTxId = " + subStartedEvent.globalTxId());
        }

        // start current retry
        subStartedEvent.setSurrogateId(-1L);
        if (subStartedEvent.retries() > 0) {
            subStartedEvent.setRetries(subStartedEvent.retries() - 1);
        }
        txConsistentService.registerSubTx(subStartedEvent, null);

        // do retry by sending sqls- to client
        serverStreamObserver.onNext(serverStream.build());

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // *** record result after retrying, if the number of starting tx is equals to the abort's, then can try it again. avoid to find all data in case of trying forever
        while (true) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (eventRepository.checkIsAlreadyRetried(subStartedEvent.globalTxId(), localTxId)) {
                break;
            }
        }

        // 'TxEndedEvent' represents success
        return eventRepository.checkIsExistsEventType(subStartedEvent.globalTxId(), localTxId, TxEndedEvent.name());
    }

    private void timeoutChecking(boolean isCanOver, TxEvent subStartedEvent) {
        // if global transaction can over(there're no more txs), even timeout occurs, still end tx normally
        if (new Date().compareTo(subStartedEvent.expiryTime()) > 0 && !isCanOver) {
            TxEvent endTxEvent = new TxEvent(subStartedEvent.serviceName(), subStartedEvent.instanceId(), subStartedEvent.globalTxId(), subStartedEvent.localTxId(), "", TxAbortedEvent.name(), "", 0, null, 0, subStartedEvent.category(), null);
            txConsistentService.registerGlobalTx(endTxEvent);
            throw new RuntimeException("Current global transaction was overtime. globalTxId = " + subStartedEvent.globalTxId());
        }
    }

    public void endGlobalTx(String globalTxId, boolean isCanOver, TxleTxStartAck.Builder startAck, TxleTxEndAck.Builder endAck) {
        try {
            if (isCanOver) {
                ClientGlobalTxCache txCache = this.getGlobalTxCache(globalTxId);
                TxEvent endTxEvent = new TxEvent(txCache.getServiceName(), txCache.getInstanceId(), globalTxId, globalTxId, "", SagaEndedEvent.name(), "", 0, null, 0, txCache.getServiceCategory(), null);
                endTxEvent.setSurrogateId(-1L);
                boolean registerResult = txConsistentService.registerGlobalTx(endTxEvent);
                if (!registerResult) {
                    String cause = "Failed to end global transaction, [" + endTxEvent.toString() + "].";
                    ExceptionFaultTolerance.handleErrorWithFaultTolerantCheck(consistencyCache, globalTxId, cause, txCache.getInstanceId(), txCache.getServiceCategory(), startAck, endAck);
                }
                txleEhCache.remove(TxleCacheType.GLOBALTX, globalTxId);
            }
        } catch (Exception e) {
            LOG.error("Failed to end global transaction. id = {}", globalTxId, e);
        }
    }

    // verifications: downgraded, paused, (overtime)aborted and exists, verify by cache as much as possible
    public boolean verifyGlobalTxBeforeStarting(TxleTransactionStart tx, TxleTxStartAck.Builder startAck) {
        ClientGlobalTxCache txCache = this.getGlobalTxCache(tx.getGlobalTxId());
        try {
            if (!consistencyCache.getBooleanValue(txCache.getInstanceId(), txCache.getServiceCategory(), ConfigCenterType.GlobalTx)) {
                startAck.setStatus(TxleTxStartAck.TransactionStatus.DEGRADED);
                // return business sql immediately in case of degradation
                for (TxleSubTransactionStart subTx : tx.getSubTxInfoList()) {
                    TxleSubTxSql.Builder subTxSql = TxleSubTxSql.newBuilder().setLocalTxId(subTx.getLocalTxId()).setDbNodeId(subTx.getDbNodeId()).setDbSchema(subTx.getDbSchema()).setOrder(subTx.getOrder());
                    subTxSql.addSubTxSql(subTx.getSql());
                    startAck.addSubTxSql(subTxSql.build());
                }
                return false;
            }

            // 检测当前全局事务是否暂停：如果暂停则进入循环，等待一定时间，再检测暂停，如果未获取到值/异常/终止均跳出循环，若暂停则不设置继续执行循环
            TxleTxStartAck.TransactionStatus txStatus = checkTxStatus(txCache.getInstanceId(), tx.getServiceCategory(), tx.getGlobalTxId());
            if (TxleTxStartAck.TransactionStatus.RUNNING.ordinal() != txStatus.ordinal()) {
                startAck.setStatus(txStatus);
                return false;
            }
        } catch (Exception e) {
            String cause = "Failed to verify before starting. globalTxId = " + tx.getGlobalTxId();
            ExceptionFaultTolerance.handleErrorWithFaultTolerantCheck(consistencyCache, tx.getGlobalTxId(), cause, txCache.getInstanceId(), txCache.getServiceCategory(), startAck, null);
        }
        return true;
    }

    private TxleTxStartAck.TransactionStatus checkTxStatus(String instanceId, String category, String globalTxId) throws Exception {
        // 是否暂停所有
        boolean paused = this.consistencyCache.getBooleanValue(instanceId, category, ConfigCenterType.PauseGlobalTx);
        // 当前全局事务是否暂停
        String statusCacheValue = this.consistencyCache.getValueByCacheKey(TxleConstants.constructTxStatusCacheKey(globalTxId));
        while (paused || (statusCacheValue != null && Paused.toString().equals(statusCacheValue))) {
//                Thread.sleep(TxleStaticConfig.getIntegerConfig("txle.transaction.pause-check-interval", 60)  * 1000);
            Thread.sleep(60 * 1000);
            paused = this.consistencyCache.getBooleanValue(instanceId, category, ConfigCenterType.PauseGlobalTx);
            statusCacheValue = this.consistencyCache.getValueByCacheKey(TxleConstants.constructTxStatusCacheKey(globalTxId));
            if (statusCacheValue != null) {
                if (GlobalTxStatus.valueOf(statusCacheValue.toUpperCase()).ordinal() == Aborted.ordinal()) {
                    break;
                }
            }
        }
        TxleTxStartAck.TransactionStatus status = TxleTxStartAck.TransactionStatus.RUNNING;
        if (statusCacheValue != null) {
            status = TxleTxStartAck.TransactionStatus.valueOf(statusCacheValue.toUpperCase());
        }
        return status;
    }

    // verifications: downgraded and paused
    public boolean verifyGlobalTxBeforeEnding(TxleTransactionEnd tx, TxleTxEndAck.Builder endAck) {
        ClientGlobalTxCache txCache = this.getGlobalTxCache(tx.getGlobalTxId());

        try {
            String instanceId = txCache == null ? null : txCache.getInstanceId();
            String category = txCache == null ? null : txCache.getServiceCategory();
            if (!consistencyCache.getBooleanValue(instanceId, category, ConfigCenterType.GlobalTx)) {
                endAck.setStatus(TxleTxEndAck.TransactionStatus.DEGRADED);
                return false;
            }

            // 检测当前全局事务是否暂停：如果暂停则进入循环，等待一定时间，再检测暂停，如果未获取到值/异常/终止均跳出循环，若暂停则不设置继续执行循环
            checkTxStatus(instanceId, category, tx.getGlobalTxId());

            // in end tx interface, need to compensate/retry/end if an abort occurs, so do not return false
        } catch (Exception e) {
            String cause = "Failed to verify before ending. globalTxId = " + tx.getGlobalTxId();
            ExceptionFaultTolerance.handleErrorWithFaultTolerantCheck(consistencyCache, tx.getGlobalTxId(), cause, txCache.getInstanceId(), txCache.getServiceCategory(), null, endAck);
        }

        return true;
    }

    private ClientGlobalTxCache getGlobalTxCache(String globalTxId) {
        ClientGlobalTxCache globalCache = (ClientGlobalTxCache) txleEhCache.get(TxleCacheType.GLOBALTX, globalTxId);
        if (globalCache == null) {
            TxEvent event = this.eventRepository.selectEventByGlobalTxIdType(globalTxId, EventType.SagaStartedEvent.name());
            if (event != null) {
                globalCache = new ClientGlobalTxCache(event);
            }
        }
        return globalCache;
    }

}
