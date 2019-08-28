/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.servicecomb.saga.common.EventType;

public class TxAbortedEvent extends TxEvent {

  private static final int PAYLOADS_MAX_LENGTH = 10240;

  public TxAbortedEvent(String globalTxId, String localTxId, String parentTxId, String compensationMethod, Throwable throwable) {
    super(EventType.TxAbortedEvent, globalTxId, localTxId, parentTxId, compensationMethod, 0, "", 0, "",
        stackTrace(throwable));
  }

  public TxAbortedEvent(String globalTxId, String localTxId, String parentTxId, String compensationMethod, String category, Throwable throwable) {
    super(EventType.TxAbortedEvent, globalTxId, localTxId, parentTxId, compensationMethod, 0, "", 0, category,
        stackTrace(throwable));
  }

  private static String stackTrace(Throwable e) {
    StringWriter writer = new StringWriter();
    e.printStackTrace(new PrintWriter(writer));
    String stackTrace = writer.toString();
    if (stackTrace.length() > PAYLOADS_MAX_LENGTH) {
      stackTrace = stackTrace.substring(0, PAYLOADS_MAX_LENGTH);
    }
    return stackTrace;
  }
}
