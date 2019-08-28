/*
 * Copyright (c) 2018-2019 ActionTech.
 * based on code by ServiceComb Pack CopyrightHolder Copyright (C) 2018,
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package org.apache.servicecomb.saga.omega.context;

/**
 * OmegaContext holds the globalTxId and localTxId which are used to build the invocation map
 */
public class OmegaContext {
  public static final String GLOBAL_TX_ID_KEY = "X-Pack-Global-Transaction-Id";
  public static final String LOCAL_TX_ID_KEY = "X-Pack-Local-Transaction-Id";
  public static final String GLOBAL_TX_CATEGORY_KEY = "X-Pack-Global-Transaction-Category";

  private final ThreadLocal<String> globalTxId = new InheritableThreadLocal<>();
  private final ThreadLocal<String> localTxId = new InheritableThreadLocal<>();
  private final IdGenerator<String> idGenerator;
  private final ThreadLocal<String> category = new InheritableThreadLocal<>();

  public OmegaContext(IdGenerator<String> idGenerator) {
    this.idGenerator = idGenerator;
  }

  public String newGlobalTxId() {
    String id = idGenerator.nextId();
    globalTxId.set(id);
    return id;
  }

  public void setGlobalTxId(String txId) {
    globalTxId.set(txId);
  }

  public String globalTxId() {
    return globalTxId.get();
  }

  public String newLocalTxId() {
    String id = idGenerator.nextId();
    localTxId.set(id);
    return id;
  }

  public void setLocalTxId(String localTxId) {
    this.localTxId.set(localTxId);
  }

  public String localTxId() {
    return localTxId.get();
  }

  public void setCategory(String category) {
    this.category.set(category);
  }

  public String category() {
    return category.get();
  }

  public void clear() {
    globalTxId.remove();
    localTxId.remove();
    category.remove();
  }

  @Override
  public String toString() {
    return "OmegaContext{" + "globalTxId=" + globalTxId.get() + ", localTxId=" + localTxId.get() + ", category=" + category.get() + '}';
  }
}
