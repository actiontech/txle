/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */
package com.actionsky.txle.cache;

import org.apache.servicecomb.saga.common.ConfigCenterType;

import java.util.Map;

public class TxleRedisCache implements ITxleConsistencyCache {

    @Override
    public boolean setKeyValueCache(String key, String value) {
        return false;
    }

    @Override
    public boolean setKeyValueCache(String key, String value, int expire) {
        return false;
    }

    @Override
    public String getValueByCacheKey(String key) {
        return null;
    }

    @Override
    public boolean getBooleanValue(String instanceId, String category, ConfigCenterType type) {
        return false;
    }

    @Override
    public Map<String, String> getValueListByCacheKey(String keyPrefix) {
        return null;
    }

    @Override
    public boolean deleteAll() {
        return false;
    }

    @Override
    public boolean delete(String key) {
        return false;
    }

    @Override
    public boolean delete(String key, String value) {
        return false;
    }

    @Override
    public boolean deleteByKeyPrefix(String keyPrefix) {
        return false;
    }

    @Override
    public boolean deleteByKeyPrefix(String keyPrefix, String value) {
        return false;
    }
}
