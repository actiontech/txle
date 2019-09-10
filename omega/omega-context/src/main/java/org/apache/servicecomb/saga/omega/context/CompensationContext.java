/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.context;

import org.apache.servicecomb.saga.common.TxleConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CompensationContext {
  private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final Map<String, CompensationContextInternal> contexts = new ConcurrentHashMap<>();
  private final OmegaContext omegaContext;

  public CompensationContext(OmegaContext omegaContext) {
    this.omegaContext = omegaContext;
  }

  public void addCompensationContext(Method compensationMethod, Object target) {
    compensationMethod.setAccessible(true);
    contexts.put(compensationMethod.toString(), new CompensationContextInternal(target, compensationMethod));
  }

  public void apply(String globalTxId, String localTxId, String compensationMethod, Object... payloads) {
    CompensationContextInternal contextInternal = contexts.get(compensationMethod);

    String oldGlobalTxId = omegaContext.globalTxId();
    String oldLocalTxId = omegaContext.localTxId();
    try {
    	// for auto-compensation By Gannalyo
    	if (TxleConstants.AUTO_COMPENSABLE_METHOD.equals(compensationMethod)) {
    		contextInternal.compensationMethod.invoke(contextInternal.target, globalTxId, localTxId);
    		return;
    	}
      omegaContext.setGlobalTxId(globalTxId);
      omegaContext.setLocalTxId(localTxId);
      contextInternal.compensationMethod.invoke(contextInternal.target, payloads);
      LOG.info("Compensated transaction with global tx id [{}], local tx id [{}]", globalTxId, localTxId);
    } catch (IllegalAccessException | InvocationTargetException e) {
      LOG.error(
          "Pre-checking for compensation method " + contextInternal.compensationMethod.toString()
              + " was somehow skipped, did you forget to configure compensable method checking on service startup?",
          e);
      // Do not report exception here, because it's not convenient for collection business information.
    } finally {
      omegaContext.setGlobalTxId(oldGlobalTxId);
      omegaContext.setLocalTxId(oldLocalTxId);
    }
  }

  private static final class CompensationContextInternal {
    private final Object target;

    private final Method compensationMethod;

    private CompensationContextInternal(Object target, Method compensationMethod) {
      this.target = target;
      this.compensationMethod = compensationMethod;
    }
  }
}
