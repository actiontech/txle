/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.transaction;

public interface EventAwareInterceptor {
  EventAwareInterceptor NO_OP_INTERCEPTOR = new EventAwareInterceptor() {
    @Override
    public AlphaResponse preIntercept(String parentTxId, String compensationMethod, int timeout, String retriesMethod,
        int retries, Object... message) {
      return new AlphaResponse(false);
    }

    @Override
    public void postIntercept(String parentTxId, String compensationMethod) {
    }

    @Override
    public void onError(String parentTxId, String compensationMethod, Throwable throwable) {
    }
  };

  AlphaResponse preIntercept(String parentTxId, String compensationMethod, int timeout, String retriesMethod,
      int retries, Object... message);

  void postIntercept(String parentTxId, String compensationMethod);

  void onError(String parentTxId, String compensationMethod, Throwable throwable);
}
