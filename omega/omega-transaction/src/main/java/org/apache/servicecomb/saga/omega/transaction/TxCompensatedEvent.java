/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction;

import org.apache.servicecomb.saga.common.EventType;

public class TxCompensatedEvent extends TxEvent {
  public TxCompensatedEvent(String globalTxId, String localTxId, String parentTxId, String compensationMethod) {
    super(EventType.TxCompensatedEvent, globalTxId, localTxId, parentTxId, compensationMethod, 0, "", 0);
  }

  public TxCompensatedEvent(String globalTxId, String localTxId, String parentTxId, String compensationMethod, String category) {
    super(EventType.TxCompensatedEvent, globalTxId, localTxId, parentTxId, compensationMethod, 0, "", 0, category);
  }
}
