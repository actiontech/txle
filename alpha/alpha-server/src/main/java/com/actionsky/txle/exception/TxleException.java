/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */
package com.actionsky.txle.exception;

import com.actionsky.txle.cache.TxleCache;
import com.actionsky.txle.grpc.TxleTxStartAck;
import org.apache.servicecomb.saga.common.ConfigCenterType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Gannalyo
 * @since 2020/2/28
 */
public class TxleException extends RuntimeException {
    private static final Logger LOG = LoggerFactory.getLogger(TxleCache.class);

    public TxleException(String message) {
        super(message);
    }

    public TxleException(String cause, Throwable throwable) {
        super(cause, throwable);
    }

    public TxleException(String cause, TxleCache txleCache, String instanceId, String category, TxleTxStartAck.Builder startTxAck) {
        this(cause, null, txleCache, instanceId, category, startTxAck);
    }

    public TxleException(String cause, Throwable throwable, TxleCache txleCache, String instanceId, String category, TxleTxStartAck.Builder startTxAck) {
        if (txleCache.readConfigCache(instanceId, category, ConfigCenterType.GlobalTxFaultTolerant)) {
            startTxAck.setStatus(TxleTxStartAck.TransactionStatus.FAULTTOLERANT);
            LOG.error(cause);
        } else {
            startTxAck.setStatus(TxleTxStartAck.TransactionStatus.ABORTED);
            if (throwable != null) {
                throw new RuntimeException(cause, throwable);
            } else {
                throw new RuntimeException(cause);
            }
        }
    }
}
