/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction;

import org.apache.servicecomb.saga.common.EventType;

public class TxStartedEvent extends TxEvent {

  public TxStartedEvent(String globalTxId, String localTxId, String parentTxId, String compensationMethod,
      int timeout, String retryMethod, int retries, Object... payloads) {
    super(EventType.TxStartedEvent, globalTxId, localTxId, parentTxId, compensationMethod, timeout, retryMethod,
        retries, payloads);
  }

  public TxStartedEvent(String globalTxId, String localTxId, String parentTxId, String compensationMethod,
      int timeout, String retryMethod, int retries, String category, Object... payloads) {
    super(EventType.TxStartedEvent, globalTxId, localTxId, parentTxId, compensationMethod, timeout, retryMethod,
        retries, category, payloads);
  }
}
