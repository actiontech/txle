/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */

package com.actionsky.txle.cache;

import org.apache.servicecomb.saga.common.ConfigCenterType;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 一致性缓存接口
 * 一致性缓存可能来自Redis、DB、甚至ZK/Consul(谨慎)等，多个实现类，在注入使用时需要留意
 * @author Gannalyo
 * @since 2020/3/3
 */
@Service
public interface ITxleConsistencyCache {

    Map<String, String> getSystemConfigCache();

    boolean resetLocalSystemConfigCache();

    boolean setKeyValueCache(String key, String value);

    // unit is second for field 'expire'
    boolean setKeyValueCache(String key, String value, int expire);

    int getKeyValueCacheCount();

    String getValueByCacheKey(String key);

    boolean getBooleanValue(String instanceId, String category, ConfigCenterType type);

    Map<String, String> getValueListByCacheKey(String keyPrefix);

    boolean deleteAll();

    boolean delete(String key);

    boolean delete(String key, String value);

    boolean deleteByKeyPrefix(String keyPrefix);

    boolean deleteByKeyPrefix(String keyPrefix, String value);
}