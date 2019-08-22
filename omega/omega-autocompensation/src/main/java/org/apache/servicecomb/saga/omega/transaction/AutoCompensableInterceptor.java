package org.apache.servicecomb.saga.omega.transaction;

import org.apache.servicecomb.saga.common.ConfigCenterType;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.transaction.monitor.AutoCompensableSqlMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

class AutoCompensableInterceptor implements EventAwareInterceptor {
    private static final Logger LOG = LoggerFactory.getLogger(AutoCompensableInterceptor.class);
    private final OmegaContext context;
    private final MessageSender sender;

    AutoCompensableInterceptor(OmegaContext context, MessageSender sender) {
        this.sender = sender;
        this.context = context;
    }

    @Override
    public AlphaResponse preIntercept(String parentTxId, String compensationMethod, int timeout, String retriesMethod,
                                      int retries, Object... message) {
        AlphaResponse response = sender.send(new TxStartedEvent(context.globalTxId(), context.localTxId(), parentTxId, compensationMethod,
                timeout, retriesMethod, retries, context.category(), message));
        // read 'sqlmonitor' config before executing business sql, the aim is to monitor business sql or not.
        readConfigFromServer();
        return response;
    }

    @Override
    public void postIntercept(String parentTxId, String compensationMethod) {
        sender.send(new TxEndedEvent(context.globalTxId(), context.localTxId(), parentTxId, compensationMethod, context.category()));
    }

    @Override
    public void onError(String parentTxId, String compensationMethod, Throwable throwable) {
        sender.send(new TxAbortedEvent(context.globalTxId(), context.localTxId(), parentTxId, compensationMethod, context.category(),
                throwable));
    }

    public Set<String> fetchLocalTxIdOfEndedGlobalTx(Set<String> localTxIdSet) {
        return sender.send(localTxIdSet);
    }

    private void readConfigFromServer() {
        try {
            AutoCompensableSqlMetrics.setIsMonitorSql(sender.readConfigFromServer(ConfigCenterType.SqlMonitor.toInteger(), context.category()).getStatus());
        } catch (Exception e) {
            LOG.error("Failed to execute method 'readConfigFromServer'.", e);
        }
    }
}
