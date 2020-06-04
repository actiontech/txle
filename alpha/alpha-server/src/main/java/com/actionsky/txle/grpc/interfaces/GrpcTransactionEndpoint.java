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
import com.actionsky.txle.grpc.*;
import com.actionsky.txle.grpc.interfaces.bizdbinfo.BusinessDBLatestDetail;
import com.actionsky.txle.grpc.interfaces.bizdbinfo.IBusinessDBLatestDetailService;
import io.grpc.stub.StreamObserver;
import org.apache.servicecomb.saga.alpha.core.TxConsistentService;
import org.apache.servicecomb.saga.alpha.core.TxEventRepository;
import org.apache.servicecomb.saga.alpha.core.accidenthandling.IAccidentHandlingService;
import org.apache.servicecomb.saga.common.TxleConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GrpcTransactionEndpoint extends TxleTransactionServiceGrpc.TxleTransactionServiceImplBase {
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private StreamObserver<TxleGrpcServerStream> serverStreamObserver;

    private GlobalTxHandler globalTxHandler;
    private CompensateService compensateService;
    private ITxleEhCache txleEhCache;
    private ITxleConsistencyCache consistencyCache;
    private IAccidentHandlingService accidentHandlingService;
    private TxEventRepository eventRepository;
    private TxConsistentService txConsistentService;
    private IBusinessDBLatestDetailService businessDBLatestDetailService;

    public GrpcTransactionEndpoint(GlobalTxHandler globalTxHandler, CompensateService compensateService, ITxleEhCache txleEhCache, ITxleConsistencyCache consistencyCache, IAccidentHandlingService accidentHandlingService, TxEventRepository eventRepository,
                                   TxConsistentService txConsistentService, IBusinessDBLatestDetailService businessDBLatestDetailService) {
        this.globalTxHandler = globalTxHandler;
        this.compensateService = compensateService;
        this.txleEhCache = txleEhCache;
        this.consistencyCache = consistencyCache;
        this.accidentHandlingService = accidentHandlingService;
        this.eventRepository = eventRepository;
        this.txConsistentService = txConsistentService;
        this.businessDBLatestDetailService = businessDBLatestDetailService;
    }

    @Override
    public void onInitialize(TxleClientConfig clientConfig, StreamObserver<TxleServerConfigStream> serverConfigStreamObserver) {
        TxleServerConfigStream.Builder serverConfigStream = TxleServerConfigStream.newBuilder()/*.setIsClientCompensate(false)*/;
        try {
            if (clientConfig == null) {
                LOG.info("TXLE initialization interface received the client request, parameter 1 is empty.");
                return;
            }
            LOG.info("TXLE initialization interface received the client request, clientConfig = " + clientConfig.getServiceIP());
            // return server's configurations
//            serverConfigStream.setIsClientCompensate(globalTxHandler.checkIsClientCompensate(instanceId, clientConfig.getServiceCategory()));
        } catch (Exception e) {
            LOG.error("An exception occurred when built rpc connection between client and server, clientConfig = {}.", clientConfig.getServiceIP(), e);
        } finally {
            serverConfigStreamObserver.onNext(serverConfigStream.build());
            serverConfigStreamObserver.onCompleted();
        }
    }

    @Override
    public StreamObserver<TxleGrpcClientStream> onBuildBidirectionalStream(StreamObserver<TxleGrpcServerStream> serverStreamObserver) {
        this.serverStreamObserver = serverStreamObserver;
        globalTxHandler.checkTimeout(serverStreamObserver);
        return new GrpcClientStreamObserver(accidentHandlingService, eventRepository, txConsistentService);
    }

    @Override
    public void onStartTransaction(TxleTransactionStart tx, StreamObserver<TxleTxStartAck> startAckStreamObserver) {
        TxleTxStartAck.Builder startAck = TxleTxStartAck.newBuilder().setStatus(TxleTxStartAck.TransactionStatus.RUNNING);
        try {
            if (tx == null) {
                LOG.info("TXLE start tx interface received the client request, parameter 1 is empty.");
                startAck.setStatus(TxleTxStartAck.TransactionStatus.ABORTED).setMessage("Empty transaction object for parameter 1.");
                return;
            }
            LOG.info("TXLE start tx interface received the client request, globalTxId = {}.", tx.getGlobalTxId());

            // check if current tx exists
            boolean isExistsGlobalTx = globalTxHandler.checkIsExistsGlobalTx(tx.getGlobalTxId());

            txleEhCache.put(TxleCacheType.GLOBALTX, tx.getGlobalTxId(), new ClientGlobalTxCache(tx));

            // verifications: downgraded, paused, (overtime)aborted and exists, verify by cache as much as possible
            if (!globalTxHandler.verifyGlobalTxBeforeStarting(tx, startAck)) {
                return;
            }

            // prepare backup sqls.
            // insert sqls: create db, create table, alter table, formal business and backup new data
            // delete sqls: create db, create table, alter table, normal backup old data and formal business
            // update sqls: create db, create table, alter table, normal backup old data, formal business and backup new data
            // append sub-tx to current global transaction
            final Map<String, String> localTxBackupSql = new HashMap<>(8);
            compensateService.prepareBackupSql(tx, startAck, isExistsGlobalTx, localTxBackupSql);

            // prepare compensation sql
            final Map<String, String> localTxCompensateSql = new HashMap<>(8);
            compensateService.constructCompensateSql(tx, startAck, localTxCompensateSql);

            // register global and subsidiary transactions
            globalTxHandler.registerStartTx(tx, startAck, isExistsGlobalTx, localTxBackupSql, localTxCompensateSql);
        } catch (Exception e) {
            startAck = TxleTxStartAck.newBuilder().setStatus(TxleTxStartAck.TransactionStatus.ABORTED).setMessage("Failed to start global transaction [" + tx.getGlobalTxId() + "].");
            LOG.error("Failed to start global transaction [{}].", tx.getGlobalTxId(), e);
        } finally {
            try {
                if (startAck.getStatus().ordinal() != TxleTxStartAck.TransactionStatus.RUNNING.ordinal()) {
                    consistencyCache.setKeyValueCache(TxleConstants.constructTxStatusCacheKey(tx.getGlobalTxId()), GlobalTxStatus.convertStatusFromValue(startAck.getStatus().getNumber()).toString());
                }
            } catch (Exception e) {
                LOG.error("Failed to set cache, globalTxId = " + tx.getGlobalTxId());
            }

            if (startAck.getStatus().ordinal() == TxleTxStartAck.TransactionStatus.ABORTED.ordinal()) {
                try {
                    // start and end the first sub-tx were all successful, however, it's failed to start current sub-tx, so the first sub-tx should be compensated.
                    globalTxHandler.compensateInStartingTx(tx, startAck, serverStreamObserver);
                } catch (Exception e) {
                    LOG.error("Failed to compensate sub-tx when starting.", e);
                }
            }

            startAckStreamObserver.onNext(startAck.build());
            startAckStreamObserver.onCompleted();
        }
    }

    @Override
    public void onEndTransaction(TxleTransactionEnd tx, StreamObserver<TxleTxEndAck> endAckStreamObserver) {
        TxleTxEndAck.Builder endAck = TxleTxEndAck.newBuilder().setStatus(TxleTxEndAck.TransactionStatus.RUNNING);
        try {
            if (tx == null) {
                LOG.info("TXLE end tx interface received the client request, parameter 1 is empty.");
                endAck.setStatus(TxleTxEndAck.TransactionStatus.ABORTED).setMessage("Empty transaction object for parameter 1.");
                return;
            }
            LOG.info("TXLE end tx interface received the client request, globalTxId = {}.", tx.getGlobalTxId());

            // verifications: downgraded and paused
            if (!globalTxHandler.verifyGlobalTxBeforeEnding(tx, endAck)) {
                return;
            }

            // register sub-txs with executing status
            globalTxHandler.registerEndTx(tx, endAck);

            List<TxleSubTransactionEnd> abnormalSubTxList = new ArrayList<>();
            tx.getSubTxInfoList().forEach(subTx -> {
                if (!subTx.getIsSuccessful()) {
                    abnormalSubTxList.add(subTx);
                }
            });

            // check if compensation is required. if there are abnormal transactions, then compensation/retry is required. compensation is necessary to overtime transaction.
            boolean isNeedCompensate = globalTxHandler.checkIsNeedCompensate(tx, endAck, abnormalSubTxList);
            boolean retryResult = true;
            if (!isNeedCompensate) {
                retryResult = globalTxHandler.retry(tx, endAck, serverStreamObserver);
            }
            if (isNeedCompensate || !retryResult) {
                globalTxHandler.compensateInEndingTx(tx, serverStreamObserver, abnormalSubTxList);
            }

            globalTxHandler.endGlobalTx(tx.getGlobalTxId(), tx.getIsCanOver(), null, endAck);
        } catch (Exception e) {
            endAck = TxleTxEndAck.newBuilder().setStatus(TxleTxEndAck.TransactionStatus.ABORTED).setMessage("Failed to end global transaction [" + tx.getGlobalTxId() + "].");
            LOG.error("Failed to end global transaction [{}].", tx.getGlobalTxId(), e);
        } finally {
            try {
                if (!tx.getIsCanOver() && endAck.getStatus().ordinal() != TxleTxStartAck.TransactionStatus.RUNNING.ordinal()) {
                    consistencyCache.setKeyValueCache(TxleConstants.constructTxStatusCacheKey(tx.getGlobalTxId()), GlobalTxStatus.convertStatusFromValue(endAck.getStatus().getNumber()).toString());
                }
            } catch (Exception e) {
                LOG.error("Failed to set cache, globalTxId = " + tx.getGlobalTxId());
            }
            endAckStreamObserver.onNext(endAck.build());
            endAckStreamObserver.onCompleted();
        }
    }

    @Override
    public void onSynDatabase(TxleBusinessDBInfo databaseInfo, StreamObserver<TxleBasicAck> serverStreamObserver) {
        boolean synResult = false;
        try {
            if (databaseInfo == null) {
                LOG.info("TXLE synchronize database interface received the client request, parameter 1 is empty.");
                return;
            } else {
                LOG.info("TXLE synchronize database interface received the client request, isFullDose = {}.", databaseInfo.getIsFullDose());

                // the latest timestamp must be more than the maximums in database
                if (databaseInfo.getIsFullDose() || databaseInfo.getTimestamp() > businessDBLatestDetailService.selectMaxTimestamp()) {
                    List<BusinessDBLatestDetail> detailList = new ArrayList<>();
                    databaseInfo.getNodeList().forEach(node ->
                            node.getDatabaseList().forEach(db ->
                                    db.getTableList().forEach(tab ->
                                            tab.getFieldList().forEach(field ->
                                                    detailList.add(new BusinessDBLatestDetail(databaseInfo.getTimestamp(), node.getId(), db.getName(), tab.getName(), field.getName(), field.getType(), field.getIsPrimaryKey()))
                                            )
                                    )
                            )
                    );

                    synResult = businessDBLatestDetailService.save(detailList, databaseInfo.getIsFullDose());
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to synchronize the latest detail of business database.", e);
        } finally {
            serverStreamObserver.onNext(TxleBasicAck.newBuilder().setIsReceived(true).setIsSuccessful(synResult).build());
            serverStreamObserver.onCompleted();
        }
    }

}
