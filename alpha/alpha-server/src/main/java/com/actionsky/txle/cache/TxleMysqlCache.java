/*
 * Copyright (c) 2018-2020 ActionTech.
 * License: http://www.apache.org/licenses/LICENSE-2.0 Apache License 2.0 or higher.
 */
package com.actionsky.txle.cache;

import com.actionsky.txle.grpc.interfaces.ICustomRepository;
import org.apache.servicecomb.saga.common.ConfigCenterType;
import org.apache.servicecomb.saga.common.TxleConstants;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TxleMysqlCache implements ITxleConsistencyCache {

    @Autowired
    private ICustomRepository customRepository;

    @Override
    public boolean setKeyValueCache(String key, String value) {
        this.delete(key);
        return this.customRepository.executeUpdate("INSERT INTO KeyValueCache(cachekey, cachevalue) VALUES(?, ?)", key, value) > 0;
    }

    @Override
    public boolean setKeyValueCache(String key, String value, int expire) {
        Date expireDate = null;
        if (expire > 0) {
            expireDate = new Date(System.currentTimeMillis() + expire * 1000);
        }
        this.delete(key);
        return this.customRepository.executeUpdate("INSERT INTO KeyValueCache VALUES(?, ?, ?)", key, value, expireDate) > 0;
    }

    @Override
    public String getValueByCacheKey(String key) {
        List list = this.customRepository.executeQuery("SELECT T.cachevalue FROM KeyValueCache T WHERE T.cachekey = ?", key);
        if (list != null && !list.isEmpty()) {
            Object value = list.get(0);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }

    @Override
    public boolean getBooleanValue(String instanceId, String category, ConfigCenterType type) {
        /**
         * 逻辑：
         * 1.先检测type的全局配置是否支持功能，若不支持，直接返回false
         * 2.验证客户端对应的type配置，如果有配置则返回该配置值
         * 3.如果无客户端配置，则验证type的全局配置，如果有则返回
         * 4.如果没有则取type的默认值
         */
        if ((TxleConstants.NO + "").equals(this.getValueByCacheKey(TxleConstants.constructGlobalConfigAbilityKey(null, null, type)))) {
            return false;
        }

        if (instanceId != null) {
            String cacheValue = this.getValueByCacheKey(TxleConstants.constructGlobalConfigValueKey(instanceId, category, type));
            if (cacheValue != null) {
                return "true".equals(cacheValue) || TxleConstants.ENABLED.equals(cacheValue);
            }
        }

        String cacheValue = this.getValueByCacheKey(TxleConstants.constructGlobalConfigValueKey(null, null, type));
        if (cacheValue != null) {
            return "true".equals(cacheValue) || TxleConstants.ENABLED.equals(cacheValue);
        }

        return type.defaultValue();
    }

    @Override
    public Map<String, String> getValueListByCacheKey(String keyPrefix) {
        List list = this.customRepository.executeQuery("SELECT T.cachevalue FROM KeyValueCache T WHERE T.cachekey LIKE CONCAT('', ?, '%')", keyPrefix);
        if (list != null && !list.isEmpty()) {
            Map<String, String> kvMap = new HashMap<>();
            list.forEach(obj -> {
                Object[] objArr = (Object[]) obj;
                kvMap.put(objArr[0] + "", objArr[1] + "");
            });
            return kvMap;
        }
        return null;
    }

    @Override
    public boolean deleteAll() {
        return this.customRepository.executeUpdate("DELETE FROM KeyValueCache") > 0;
    }

    @Override
    public boolean delete(String key) {
        return this.customRepository.executeUpdate("DELETE FROM KeyValueCache WHERE cachekey = ?", key) > 0;
    }

    @Override
    public boolean delete(String key, String value) {
        return this.customRepository.executeUpdate("DELETE FROM KeyValueCache WHERE cachekey = ? AND cachevalue = ?", key, value) > 0;
    }

    @Override
    public boolean deleteByKeyPrefix(String keyPrefix) {
        return this.customRepository.executeUpdate("DELETE FROM KeyValueCache WHERE cachekey LIKE CONCAT('', ?, '%')", keyPrefix) > 0;
    }

    @Override
    public boolean deleteByKeyPrefix(String keyPrefix, String value) {
        return this.customRepository.executeUpdate("DELETE FROM KeyValueCache WHERE cachekey LIKE CONCAT('', ?, '%') AND cachevalue = ?", keyPrefix, value) > 0;
    }
}
