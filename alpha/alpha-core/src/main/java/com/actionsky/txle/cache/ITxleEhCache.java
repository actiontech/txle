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

    void put(CacheName cacheName, String key, Object value);

    void putIfAbsent(CacheName cacheName, String key, Object value);

    void put(CacheName cacheName, String key, Object value, int timeout);

    void putIfAbsent(CacheName cacheName, String key, Object value, int timeout);

    Object get(CacheName cacheName, String key);

    List<String> getKeys(CacheName cacheName);

    void remove(CacheName cacheName, String key);

    boolean readConfigCache(String serviceInstanceId, String category, ConfigCenterType type);

    boolean readConfigCache(String serviceInstanceId, String category, ConfigCenterType type, String globalTxId);

    int readIntConfigCache(String serviceInstanceId, String category, ConfigCenterType type);
}
