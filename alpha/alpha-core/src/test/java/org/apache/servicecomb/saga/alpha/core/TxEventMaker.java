/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.core;

import static com.seanyinx.github.unit.scaffolding.Randomness.uniquify;
import static org.apache.servicecomb.saga.common.EventType.TxStartedEvent;

import java.util.UUID;

class TxEventMaker {
  static TxEvent someEvent() {
    return new TxEvent(
        uniquify("serviceName"),
        uniquify("instanceId"),
        uniquify("globalTxId"),
        uniquify("localTxId"),
        UUID.randomUUID().toString(),
        TxStartedEvent.name(),
        TxEventMaker.class.getCanonicalName(),
        "",
        uniquify("blah").getBytes());
  }
}
