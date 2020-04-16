/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */
package com.actionsky.txle.exception;

import com.actionsky.txle.cache.ITxleConsistencyCache;
import com.actionsky.txle.grpc.TxleTxEndAck;
import com.actionsky.txle.grpc.TxleTxStartAck;
import org.apache.servicecomb.saga.common.ConfigCenterType;
import org.apache.servicecomb.saga.common.TxleConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.actionsky.txle.enums.GlobalTxStatus.Aborted;
import static com.actionsky.txle.enums.GlobalTxStatus.FaultTolerant;

/**
 * @author Gannalyo
 * @since 2020/2/28
 */
public class ExceptionFaultTolerance extends RuntimeException {
    private static final Logger LOG = LoggerFactory.getLogger(ExceptionFaultTolerance.class);

    public ExceptionFaultTolerance(String message) {
        super(message);
    }

    public ExceptionFaultTolerance(String cause, Throwable throwable) {
        super(cause, throwable);
    }

    public static void handleErrorWithFaultTolerantCheck(ITxleConsistencyCache consistencyCache, String globalTxId, String cause, String instanceId, String category, TxleTxStartAck.Builder txStartAck, TxleTxEndAck.Builder txEndAck) {
        if (consistencyCache.getBooleanValue(instanceId, category, ConfigCenterType.GlobalTxFaultTolerant)) {
            if (txStartAck != null) {
                txStartAck.setStatus(TxleTxStartAck.TransactionStatus.FAULTTOLERANT).setMessage(cause);
            } else if (txEndAck != null) {
                txEndAck.setStatus(TxleTxEndAck.TransactionStatus.FAULTTOLERANT).setMessage(cause);
            }
            consistencyCache.setKeyValueCache(TxleConstants.constructTxStatusCacheKey(globalTxId), FaultTolerant.toString());
            LOG.error(cause);
        } else {
            if (txStartAck != null) {
                txStartAck.setStatus(TxleTxStartAck.TransactionStatus.ABORTED).setMessage(cause);
            } else if (txEndAck != null) {
                txEndAck.setStatus(TxleTxEndAck.TransactionStatus.ABORTED).setMessage(cause);
            }
            consistencyCache.setKeyValueCache(TxleConstants.constructTxStatusCacheKey(globalTxId), Aborted.toString());
            throw new RuntimeException(cause);
        }
    }
}
