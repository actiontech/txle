/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.cache;

import org.apache.servicecomb.saga.common.ConfigCenterType;

import java.util.List;

/**
 * @author Gannalyo
 * @since 2020/3/3
 */
public interface ITxleEhCache {

    void put(TxleCacheType txleCacheType, String key, Object value);

    void putIfAbsent(TxleCacheType txleCacheType, String key, Object value);

    void put(TxleCacheType txleCacheType, String key, Object value, int timeout);

    void putIfAbsent(TxleCacheType txleCacheType, String key, Object value, int timeout);

    Object get(TxleCacheType txleCacheType, String key);

    Boolean getBooleanValue(TxleCacheType txleCacheType, String key);

    List<String> getKeys(TxleCacheType txleCacheType);

    void remove(TxleCacheType txleCacheType, String key);

    void removeGlobalTxCache(String globalTxId);

    boolean readConfigCache(String serviceInstanceId, String category, ConfigCenterType type);

    boolean readConfigCache(String serviceInstanceId, String category, ConfigCenterType type, String globalTxId);

    int readIntConfigCache(String serviceInstanceId, String category, ConfigCenterType type);
}
