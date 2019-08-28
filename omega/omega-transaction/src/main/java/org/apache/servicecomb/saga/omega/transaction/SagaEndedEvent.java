/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction;

import org.apache.servicecomb.saga.common.EventType;

public class SagaEndedEvent extends TxEvent {
  SagaEndedEvent(String globalTxId, String localTxId) {
    super(EventType.SagaEndedEvent, globalTxId, localTxId, null, "", 0, "", 0);
  }

  SagaEndedEvent(String globalTxId, String localTxId, String category) {
    super(EventType.SagaEndedEvent, globalTxId, localTxId, null, "", 0, "", 0, category);
  }
}
