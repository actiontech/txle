/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction;

import org.apache.servicecomb.saga.common.ConfigCenterType;
import org.apache.servicecomb.saga.common.TxleConstants;
import org.apache.servicecomb.saga.omega.context.OmegaContext;
import org.apache.servicecomb.saga.omega.transaction.monitor.CompensableSqlMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CompensableInterceptor implements EventAwareInterceptor {
  private static final Logger LOG = LoggerFactory.getLogger(CompensableInterceptor.class);
  private final OmegaContext context;
  private final MessageSender sender;

  CompensableInterceptor(OmegaContext context, MessageSender sender) {
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
    sender.send(
        new TxAbortedEvent(context.globalTxId(), context.localTxId(), parentTxId, compensationMethod, context.category(), throwable));
  }

  private void readConfigFromServer() {
    try {
      CompensableSqlMetrics.setIsMonitorSql(sender.readConfigFromServer(ConfigCenterType.SqlMonitor.toInteger(), context.category()).getStatus());
    } catch (Exception e) {
      LOG.error(TxleConstants.LOG_ERROR_PREFIX + "Failed to execute method 'readConfigFromServer'.", e);
    }
  }
}
