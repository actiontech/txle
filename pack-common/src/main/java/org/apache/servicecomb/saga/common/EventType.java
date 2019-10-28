/*
 * Copyright (c) 2018-2019 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.common;

public enum EventType {
  SagaStartedEvent,
  TxStartedEvent,
  TxEndedEvent,
  TxAbortedEvent,
  TxCompensatedEvent,
  SagaEndedEvent
}
