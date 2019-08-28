/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction;

import java.util.Arrays;

import org.apache.servicecomb.saga.common.EventType;

public class TxEvent {

  private final long timestamp;
  private final EventType type;
  private final String globalTxId;
  private final String localTxId;
  private final String parentTxId;
  private final String compensationMethod;
  private final int timeout;
  private final Object[] payloads;

  private final String retryMethod;
  private final int retries;

  private final String category;

  public TxEvent(EventType type, String globalTxId, String localTxId, String parentTxId, String compensationMethod,
      int timeout, String retryMethod, int retries, Object... payloads) {
    this.timestamp = System.currentTimeMillis();
    this.type = type;
    this.globalTxId = globalTxId;
    this.localTxId = localTxId;
    this.parentTxId = parentTxId;
    this.compensationMethod = compensationMethod;
    this.timeout = timeout;
    this.retryMethod = retryMethod;
    this.retries = retries;
    this.category = "";
    this.payloads = payloads;
  }

  public TxEvent(EventType type, String globalTxId, String localTxId, String parentTxId, String compensationMethod,
                 int timeout, String retryMethod, int retries, String category, Object... payloads) {
    this.timestamp = System.currentTimeMillis();
    this.type = type;
    this.globalTxId = globalTxId;
    this.localTxId = localTxId;
    this.parentTxId = parentTxId;
    this.compensationMethod = compensationMethod;
    this.timeout = timeout;
    this.retryMethod = retryMethod;
    this.retries = retries;
    this.category = category;
    this.payloads = payloads;
  }

  public long timestamp() {
    return timestamp;
  }

  public String globalTxId() {
    return globalTxId;
  }

  public String localTxId() {
    return localTxId;
  }

  public String parentTxId() {
    return parentTxId;
  }

  public Object[] payloads() {
    return payloads;
  }

  public EventType type() {
    return type;
  }

  public String compensationMethod() {
    return compensationMethod;
  }

  public int timeout() {
    return timeout;
  }

  public String retryMethod() {
    return retryMethod;
  }

  public int retries() {
    return retries;
  }

  public String category() {
    return category;
  }

  @Override
  public String toString() {
    return type.name() + "{" + "globalTxId='" + globalTxId + '\'' + ", localTxId='" + localTxId + '\'' + ", parentTxId='" + parentTxId + '\'' + ", compensationMethod='"
            + compensationMethod + '\'' + ", timeout=" + timeout + ", retryMethod='" + retryMethod + '\'' + ", category='" + category + '\'' + ", retries=" + retries
            + ", payloads=" + Arrays.toString(payloads) + '}';
  }
}
