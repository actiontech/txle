/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction;

import org.apache.servicecomb.saga.common.EventType;

public class SagaStartedEvent extends TxEvent {
  public SagaStartedEvent(String globalTxId, String localTxId, int timeout) {
    // use "" instead of null as compensationMethod requires not null in sql
    super(EventType.SagaStartedEvent, globalTxId, localTxId, null, "", timeout, "", 0);
  }
  public SagaStartedEvent(String globalTxId, String localTxId, int timeout, String category) {
    // use "" to instead of null, as compensationMethod could not be null in sql
    super(EventType.SagaStartedEvent, globalTxId, localTxId, null, "", timeout, "", 0, category);
  }
}
