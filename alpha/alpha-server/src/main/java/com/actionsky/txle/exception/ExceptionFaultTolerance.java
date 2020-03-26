/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */
package com.actionsky.txle.exception;

import com.actionsky.txle.cache.ITxleEhCache;
import com.actionsky.txle.grpc.TxleTxEndAck;
import com.actionsky.txle.grpc.TxleTxStartAck;
import org.apache.servicecomb.saga.common.ConfigCenterType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public static void handleErrorWithFaultTolerantCheck(ITxleEhCache txleEhCache, String cause, String instanceId, String category, TxleTxStartAck.Builder txStartAck, TxleTxEndAck.Builder txEndAck) {
        if (txleEhCache.readConfigCache(instanceId, category, ConfigCenterType.GlobalTxFaultTolerant)) {
            if (txStartAck != null) {
                txStartAck.setStatus(TxleTxStartAck.TransactionStatus.FAULTTOLERANT).setMessage(cause);
            } else if (txEndAck != null) {
                txEndAck.setStatus(TxleTxEndAck.TransactionStatus.FAULTTOLERANT).setMessage(cause);
            }
            LOG.error(cause);
        } else {
            if (txStartAck != null) {
                txStartAck.setStatus(TxleTxStartAck.TransactionStatus.ABORTED).setMessage(cause);
            } else if (txEndAck != null) {
                txEndAck.setStatus(TxleTxEndAck.TransactionStatus.ABORTED).setMessage(cause);
            }
            throw new RuntimeException(cause);
        }
    }
}
