/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.alpha.core;

import static java.util.Collections.emptyMap;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompositeOmegaCallback implements OmegaCallback {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private final Map<String, Map<String, OmegaCallback>> callbacks;

  public CompositeOmegaCallback(Map<String, Map<String, OmegaCallback>> callbacks) {
    this.callbacks = callbacks;
  }

  @Override
  public void compensate(TxEvent event) {
    Map<String, OmegaCallback> serviceCallbacks = callbacks.getOrDefault(event.serviceName(), emptyMap());

    if (serviceCallbacks.isEmpty()) {
      throw new AlphaException("No such omega callback found for service " + event.serviceName());
    }

    OmegaCallback omegaCallback = serviceCallbacks.get(event.instanceId());
    if (omegaCallback == null) {
      LOG.info("Cannot find the service with the instanceId {}, call the other instance.", event.instanceId());
      omegaCallback = serviceCallbacks.values().iterator().next();
    }

    try {
      omegaCallback.compensate(event);
    } catch (Exception e) {
      serviceCallbacks.values().remove(omegaCallback);
      throw e;
    }
  }
}
